package trandemo.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcCallException;
import org.voltdb.types.TimestampType;
import org.voltdb.voltutil.stats.SafeHistogramCache;

import trandemo.server.ReferenceData;

/* This file is part of VoltDB.
 * Copyright (C) 2008-2018 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

public class Demo {

    private static final long TWENTY_SECONDS_MS = 20 * 1000;
    SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");
    final String dayOfYear = df1.format(new Date(System.currentTimeMillis()));

    // We use two clients - for various reasons 
    // Callbacks need their own connection
    Client mainClient = null;
    Client callbackClient = null;
    
    
    // Input text file path.
    String filename = null;
    
    // How many users we have. For simple 
    int howMany = 1;
    int maxRowsPerFile = 0;

    /**
     * Run a demo that uses real subway system data to show VoltDB's
     * OLTP and HTAP capabilities.
     * @param hostnames list of voltDB servers, comma delimited.
     * @param filename input filename. Normally 'subwaytestfullweek.csv'
     * @param howMany How many users the system has. subwaytestfullweek.csv
     * has 2,533,405 events, and we map users to events by dividing the userId 
     * into the eventId. So if howMany is 1,000,000 each user will make around 
     * 2.53 trips per day. Reducing the number of users increases contention
     * for access to the users balance, and the likelyhood they will be called
     * out for being on two trains at once.
     */
    public Demo(String hostnames, String filename, int howMany) {

        super();

        try {
            mainClient = connectVoltDB(hostnames);
            callbackClient = connectVoltDB(hostnames);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.filename = filename;
        this.howMany = howMany;
    }

  
    /**
     * Reset function
     * @param howMany How any users to create.
     * @param credit How much money, in pennies, they should each start with.
     * @param tpMs How many transactions per millisecond we should do.
     * @throws IOException
     * @throws NoConnectionsException
     * @throws ProcCallException
     * @throws InterruptedException
     */
    private void reset(long howMany, long credit, int tpMs)
            throws IOException, NoConnectionsException, ProcCallException, InterruptedException {
        
        msg("Reset Database Starting...");
        ClientResponse cr = mainClient.callProcedure("ResetDatabase");
        msg("Reset Database Finished.");
        msg(cr.getAppStatusString());

        ComplainOnErrorCallback iuCallback = new ComplainOnErrorCallback();

        msg("Creating " + howMany + " users each with credit of " + credit);

        long currentMs = System.currentTimeMillis();
        int tpThisMs = 0;
        final TimestampType startRun = new TimestampType (new Date(System.currentTimeMillis()));

        for (int i = 0; i < howMany; i++) {

            if (tpThisMs++ > tpMs) {

                while (currentMs == System.currentTimeMillis()) {
                    Thread.sleep(0, 50000);
                }

                currentMs = System.currentTimeMillis();
                tpThisMs = 0;
            }

             mainClient.callProcedure(iuCallback, "UpdateCard", i, credit, "Y",startRun);

            if (i % 10000 == 1) {
                msg("Created " + i + " users...");

            }

        }
        msg("Finished queueing requests to create " + howMany + " users...");

        mainClient.drain();
        msg("All requests done...");
    }

    /**
     * Load 'weeks' worth of data. 
     * @param weeks How many weeks.
     * @param tpmsOrSpeedup Value for transactions per millisecond or speedup from reality,
     * as specified by isTps.
     * @param isTps 'true' if we are trying to run at a specific tps.
     * @return How nany events we did.
     */
    public int load(int weeks, int tpmsOrSpeedup,  boolean isTps) {
        
        long currentTpmsOrSpeedup = tpmsOrSpeedup;
        boolean currentIsTps = isTps;

        try {
            if (currentIsTps) {
                mainClient.callProcedure("UpdateSimulationSpeed", "TPS", currentTpmsOrSpeedup);

            } else {
                mainClient.callProcedure("UpdateSimulationSpeed", "SPEED", currentTpmsOrSpeedup);

            }
        } catch (IOException | ProcCallException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        msg("Weeks to run for: " + weeks + "...");
        SafeHistogramCache shc = SafeHistogramCache.getInstance();

        int maxEventCount = 0;
        int eventCount = 0;
        File f = new File(filename);

        long tpThisPerMs = currentTpmsOrSpeedup;

        if (!currentIsTps) {
            tpThisPerMs = 50;
        }

        for (int weekCount = 0; weekCount < weeks; weekCount++) {
            msg("Week " + weekCount);

            if (f.exists() && f.isFile() && f.canRead()) {
                BufferedReader br = null;
                try {

                    long currentMs = System.currentTimeMillis();
                    int transThisMs = 0;

                    FileReader fr = new FileReader(f);
                    br = new BufferedReader(fr);

                    String line;
                    Date lastTimeSeen = new Date();
                    long startOfThisMinute = 0;
                    String lastDaySeen = "";
                    int day = 1;

                    while ((line = br.readLine()) != null) {
                        eventCount++;

                        String[] lineContents = line.split(",");
                        try {

                            String today = lineContents[0];

                            if (!today.equals(lastDaySeen)) {
                                day = setDayOfWeek(lineContents[0], weekCount);
                                lastDaySeen = today;
                                msg("Day is " + lastDaySeen);
                            }

                            Date tripDate = getDate(lineContents[1], weekCount, day);

                            if (!tripDate.equals(lastTimeSeen)) {
                                // Time has changed...

                                if (!currentIsTps) {
                                    // Figure out how much of minute we have to
                                    // wait
                                    // before moving on....
                                    long officialMSToTake = (60 * 1000) / currentTpmsOrSpeedup;
                                    long actualMsTaken = System.currentTimeMillis() - startOfThisMinute;

                                    if (actualMsTaken < officialMSToTake) {
                                        // msg("Sleeping " +(officialMSToTake -
                                        // actualMsTaken) + "ms" );
                                        Thread.sleep(officialMSToTake - actualMsTaken);
                                    }
                                }

                                ClientResponse updateTimeResponse = mainClient.callProcedure("UpdateSimulationTime",
                                        tripDate);
                                
                                // See if speed has changed...
                                VoltTable speedTable = updateTimeResponse.getResults()[2];
                                speedTable.advanceRow();
                                
                                if (currentIsTps &&  speedTable.getString("TPS_OR_SPEED").equals("SPEED") ) {
                                    msg("Simulation Mode changed to SPEED");
                                    currentIsTps = false;
                                }

                                if (( !currentIsTps)  &&  speedTable.getString("TPS_OR_SPEED").equals("TPS") ) {
                                    msg("Simulation Mode changed to TPS");
                                    currentIsTps = true;
                                }

                                if (currentTpmsOrSpeedup != speedTable.getLong("NUMBER_VALUE")) {
                                    currentTpmsOrSpeedup =  speedTable.getLong("NUMBER_VALUE");
                                    msg("Simulation Speed changed to " + currentTpmsOrSpeedup);
                                    
                                }

                                if (lineContents[1].endsWith(":00")) {
                                    msg("Time is : " + lineContents[1] + " eventcount=" + eventCount);
                                }
                                lastTimeSeen = tripDate;
                                startOfThisMinute = System.currentTimeMillis();
                                
                                    ComplainOnErrorCallback coec = new ComplainOnErrorCallback();
                                    mainClient.callProcedure(coec, "DashBoard2", 10);
                             


                            }

                            String tripType = lineContents[2];

                            long eventId = getLong(lineContents[3]) + ((long) weekCount * maxRowsPerFile);
                            long userId = (eventId) % howMany;
                            String subsystem = lineContents[4];

                            if (eventId % 1 == 0) {
                                if (tripType.equals(ReferenceData.BUS_EVENT)) {

                                    // Example:
                                    // Mon,04:32,BUSTRIP,893179,LTB,PPY,N,100,100,59,PAYG

                                    String jnytyp = lineContents[5];
                                    //String capped = lineContents[6];
                                    String busroute = lineContents[9];
                                    String product = lineContents[10];

                                    CardSwipeCallback cwbc = new CardSwipeCallback(callbackClient, userId, eventId,
                                            tripType, tripDate, subsystem, jnytyp, busroute, product, "");
                                    mainClient.callProcedure(cwbc, "CardSwipe", userId, eventId, tripType, tripDate,
                                            subsystem, "", "", jnytyp, busroute, product);

                                }

                                else if (tripType.equals(ReferenceData.STARTSUBWAY_EVENT)) {

                                    // Example:
                                    // Mon,05:00,BEGINTRIP,897290,LUL,Morden

                                    String startStation = lineContents[5].trim();
                                   

                                    CardSwipeCallback cwbc = new CardSwipeCallback(callbackClient, userId, eventId,
                                            tripType, tripDate, subsystem, "", startStation, "", "");
                                    mainClient.callProcedure(cwbc, "CardSwipe", userId, eventId, tripType, tripDate,
                                            subsystem, startStation, "", "", "", "");

                                } else if (tripType.equals(ReferenceData.ENDSUBWAY_EVENT)) {

                                    // Example:
                                    // Mon,05:26,ENDTRIP,897114,LUL,Westminster,Z0102,TKT,N,0,0,LUL
                                    // Travelcard-Annual

                                    String endStation = lineContents[5].trim();
                                    String zvppt = lineContents[6];
                                    String jnytyp = lineContents[7];
                                    String product = lineContents[9];

                                    CardSwipeCallback cwbc = new CardSwipeCallback(callbackClient, userId, eventId,
                                            tripType, tripDate, subsystem, jnytyp, endStation, product, zvppt);
                                    mainClient.callProcedure(cwbc, "CardSwipe", userId, eventId, tripType, tripDate,
                                            subsystem, endStation, zvppt, jnytyp, "", product);

                                }
                            }

                        } catch (Exception e) {
                            msg("Event " + eventCount + " is bad. line=" + line + " error=" + e.getMessage());
                        }

                        // msg(line);

  
                        //
                        // Limit how often we speak to the database to avoid
                        // swamping it...
                        //

                        // If we've already done as many requests this ms
                        // as we're supposed to...
                        if (transThisMs++ > tpThisPerMs) {

                            // Sleep until the MS has changed...
                            while (currentMs == System.currentTimeMillis()) {
                                Thread.sleep(0, 50000);
                            }

                            // currentMs has changed...
                            currentMs = System.currentTimeMillis();
                            transThisMs = 0;
                        }
                        

                    }


                    br.close();
                    fr.close();

                } catch (Exception e) {
                    msg(e.getMessage());
                } finally {

                }

                msg("Finished...");

                msg(shc.toString());

            } else {
                msg("Can't load file " + filename);
                System.exit(1);

            }

            if (maxRowsPerFile == 0) {
                maxRowsPerFile = eventCount;
            }

        }
        return maxEventCount;

    }

    private static int setDayOfWeek(String dayofWeek, int weekCount) {
        switch (dayofWeek) {
        case "Sun":
            return 0 + (weekCount * 7);

        case "Mon":
            return 1 + (weekCount * 7);

        case "Tue":
            return 2 + (weekCount * 7);

        case "Wed":
            return 3 + (weekCount * 7);

        case "Thu":
            return 4 + (weekCount * 7);

        case "Fri":
            return 5 + (weekCount * 7);

        case "Sat":
            return 6 + (weekCount * 7);

        }

        return -1;

    }

    private static Client connectVoltDB(String hostname) throws Exception {
        Client client = null;
        ClientConfig config = null;

        try {
            msg("Logging into VoltDB");

            config = new ClientConfig(); // "admin", "idontknow");
            config.setMaxOutstandingTxns(20000);
            config.setMaxTransactionsPerSecond(200000);
            config.setTopologyChangeAware(true);
            config.setReconnectOnConnectionLoss(true);

            client = ClientFactory.createClient(config);

            String[] hostnameArray = hostname.split(",");

            for (int i = 0; i < hostnameArray.length; i++) {
                msg("Connect to " + hostnameArray[i] + "...");
                try {
                    client.createConnection(hostnameArray[i]);
                } catch (Exception e) {
                    msg(e.getMessage());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("VoltDB connection failed.." + e.getMessage(), e);
        }

        return client;

    }

    public static void connectToOtherNodes(Client theClient) {
        VoltTable[] results;
        try {

            List<InetSocketAddress> l = theClient.getConnectedHostList();

            results = theClient.callProcedure("@SystemInformation", "OVERVIEW").getResults();

            while (results[0].advanceRow()) {
                String key = results[0].getString("KEY");
                String value = results[0].getString("VALUE");

                if (key.equals("IPADDRESS")) {
                    // See if we are already connected...

                    boolean notFound = true;
                    for (InetSocketAddress anAddress : l) {
                        if (anAddress.getAddress().toString().equals(value)) {
                            notFound = false;
                            break;
                        }

                        if (notFound) {
                            msg("Connecting to " + value + "...");
                            theClient.createConnection(value);
                        }
                    }
                }
            }

        } catch (NoConnectionsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ProcCallException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void msg(String message) {
        
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        System.out.println(strDate + ":" + message);
    }

    private static long getLong(String string) {
        Long l = null;
        try {
            l = Long.parseLong(string);
        } catch (NumberFormatException e) {

            msg("Value of " + string + " is not a number");
            l = -1L;
        }

        return l.longValue();

    }

    private Date getDate(String timeString, long weeks, int days) {

        Date newDate = null;
        try {
            newDate = parseFormat.parse(dayOfYear + "  " + timeString);

            long newDateAsTime = newDate.getTime();

            newDateAsTime += ((days + (weeks * 7)) * (24 * 60 * 60 * 1000));

            newDate = new Date(newDateAsTime);

        } catch (ParseException e) {
            newDate = null;
            e.printStackTrace();
        }

        return newDate;
    }

    private void closeClients() {
 
        try {
            mainClient.close();
        } catch (InterruptedException e) {
            Demo.msg(e.getMessage());
        }

        try {
            callbackClient.close();
        } catch (InterruptedException e) {
            Demo.msg(e.getMessage());
        }


    }

 
    public static void main(String[] args) {

        try {

            if (args.length < 3) {
                msg("Usage: TransportDemo hostnames purpose filename howmany credit weeks [SPEED|TPS] value");
                System.exit(1);
            }

            String hostnames = args[0];
            String purpose = args[1];
            String filename = args[2];

            File testFile = new File(filename);

            if (!testFile.exists()) {
                msg("File '" + filename + "' not found");
                System.exit(2);
            }

            long howMany = 5000000;
            long credit = 10000;
            int weeks = 1;
            boolean isTps = true;
            int tpMsOrSpeedup = 30;

            try {
                howMany = Long.parseLong(args[3]);
                credit = Long.parseLong(args[4]);
                weeks = Integer.parseInt(args[5]);
                if (args[6].equalsIgnoreCase("SPEED")) {
                    isTps = false;
                }
                tpMsOrSpeedup = Integer.parseInt(args[7]);

            } catch (Exception e) {
                msg("Invalid Parameter.");
            }

            msg("Hostnames=" + hostnames);
            msg("Purpose=" + purpose);
            msg("File = " + filename);
            msg("Registered Users = " + howMany);
            msg("Credit per use in pennies = " + credit);
            msg("Weeks to run for:" + weeks);

            if (isTps) {
                msg("TPS or Speed = TPS");
                msg("transactions per MS = " + tpMsOrSpeedup);
            } else {
                msg("TPS or Speed = SPEED");
                msg("speedup from real time = " + tpMsOrSpeedup);
            }

            Demo l = new Demo(hostnames, filename, (int) howMany);

            if (purpose.equalsIgnoreCase("RESET")) {

                l.reset(howMany, credit, tpMsOrSpeedup);

            } else if (purpose.equalsIgnoreCase("RUN")) {

                l.reset(howMany, credit, tpMsOrSpeedup);

                l.load(weeks, tpMsOrSpeedup, isTps);

            } else {
                msg("Purpose '" + purpose + "' unknown");
            }

            l.closeClients();

            System.exit(0);

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

}
