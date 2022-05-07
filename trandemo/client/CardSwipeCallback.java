package trandemo.client;

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

import java.util.Date;
import java.util.Random;

import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcedureCallback;
import org.voltdb.voltutil.stats.SafeHistogramCache;

import trandemo.server.ReferenceData;

/**
 * This class is used to handle responses from async calls to the 
 * CardSwipe stored procedure. If the call errored it retries. If
 * the transaction requires more money than the user has it adds
 * money and then retries.
 * @author drolfe
 *
 */
public class CardSwipeCallback implements ProcedureCallback {

    final int RETRIES = 20;

    SafeHistogramCache shc = SafeHistogramCache.getInstance();

    long startMs;

    /**
     * For various reasons we need to use a seperate connection to the database
     * when speaking to it via a callback...
     */
    Client callbackClient;
    
    long userId;
    long eventId;
    String tripType;
    Date tripDate;
    String subsystem;
    String jnytyp;
    String busrouteOrStation;
    String product;
    String zvppt;

    Random r = new Random();

    public CardSwipeCallback(Client callbackClient, long userId, long eventId, String tripType, Date tripDate,
            String subsystem, String jnytyp, String busrouteOrStation, String product, String zvppt) {
        
        super();

        startMs = System.currentTimeMillis();

        this.callbackClient = callbackClient;
        this.userId = userId;
        this.eventId = eventId;
        this.tripType = tripType;
        this.tripDate = tripDate;
        this.subsystem = subsystem;
        this.jnytyp = jnytyp;
        this.busrouteOrStation = busrouteOrStation;
        this.product = product;
        this.zvppt = zvppt;

    }

    /**
     * Handle possible responses to an async call to CardSwipe. 
     * 
     * @see org.voltdb.client.ProcedureCallback#clientCallback(org.voltdb.client.ClientResponse)
     */
    @Override
    public void clientCallback(ClientResponse arg0) throws Exception {

        // Keep track of outcome
        shc.incCounter(mapStatusByte(arg0.getStatus()));

        // If the client reports a failure while talking to the DB...
        if (arg0.getStatus() != ClientResponse.SUCCESS) {

            Demo.msg("User Id/EventId/Error Code " + userId + "/" + eventId + "/" + arg0.getStatusString());
            shc.reportLatency("WALL_FAIL_TIME_FOR_" + mapStatusByte(arg0.getStatus()), startMs, "", 4000);

            // Try up to RETRIES times to make this work...
            boolean notDone = true;
            for (int i = 0; i < RETRIES && notDone; i++) {

                // Sleep for 1, 4, 8, 16.. ms
                if (i > 0) {
                    Thread.sleep((i * i) * 1000);
                }
                
                Demo.msg("Event " + eventId + ": Retry " + i + " of " + RETRIES);

                if (tripType.equals(ReferenceData.BUS_EVENT)) {

                    Demo.msg("Retrying Bus Swipe...");

                    try {
                        ClientResponse cr2 = callbackClient.callProcedure("CardSwipe", userId, eventId, tripType,
                                tripDate, subsystem, "", "", jnytyp, busrouteOrStation, product);
                        notDone = false;
                        Demo.msg(cr2.getAppStatusString());
                        shc.report(tripType + "_RETRIES", i, "", RETRIES);

                    } catch (Exception e) {
                        Demo.msg(e.getMessage());
                    }

                } else if (tripType.equals(ReferenceData.STARTSUBWAY_EVENT)) {

                    Demo.msg("Retrying Tube Start Swipe...");

                    try {

                        ClientResponse cr2 = callbackClient.callProcedure("CardSwipe", userId, eventId, tripType,
                                tripDate, subsystem, busrouteOrStation, "", "", "", "");

                        notDone = false;
                        Demo.msg(cr2.getAppStatusString());
                        shc.report(tripType + "_RETRIES", i, "", RETRIES);

                    } catch (Exception e) {
                        Demo.msg(e.getMessage());
                    }

                } else if (tripType.equals(ReferenceData.ENDSUBWAY_EVENT)) {

                    Demo.msg("Retrying Tube End Swipe...");

                    try {

                        ClientResponse cr2 = callbackClient.callProcedure("CardSwipe", userId, eventId, tripType,
                                tripDate, subsystem, busrouteOrStation, zvppt, jnytyp, "", product);
                        notDone = false;
                        Demo.msg(cr2.getAppStatusString());
                        shc.report(tripType + "_RETRIES", i, "", RETRIES);

                    } catch (Exception e) {
                        Demo.msg(e.getMessage());
                    }

                }

            } // End of for loop

            if (notDone) {
                shc.incCounter(tripType + "_RETRY_FAILS");
            }

            // If the call to the database worked, but something application related
            // went wrong...
        } else if (arg0.getAppStatus() != ReferenceData.STATUS_OK) {

            Demo.msg("Bad Swipe: " + arg0.getAppStatusString());
            
            shc.reportLatency("WALL_TIME_BAD_SWIPE", startMs, "", 4000);
            shc.incCounter("BAD_SWIPE");

            // if it's because we did't have enough $ add some and retry...
            if (arg0.getAppStatus() == ReferenceData.STATUS_NO_MONEY) {

                shc.incCounter("No Money");

                Demo.msg("Trying to add money...");
                ClientResponse cr = null;

                // Add a number of whole GBP between 10 and 200 to the
                // card...
                boolean notDone = true;
                for (int i = 0; i < RETRIES && notDone; i++) {
                    try {
                        // Sleep for 1, 4, 8, 16.. ms
                        if (i > 0) {
                            Thread.sleep((i * i) * 1000);
                        }
                        cr = callbackClient.callProcedure("UpdateCard", userId, (10 + r.nextInt(190)) * 100, "N",
                                tripDate);
                        notDone = false;

                    } catch (Exception e) {
                        Demo.msg(e.getMessage());
                    }
                }
                Demo.msg(cr.getAppStatusString());

                if (cr.getAppStatus() == ReferenceData.STATUS_OK) {

                    notDone = true;
                    for (int i = 0; i < RETRIES && notDone; i++) {

                        // Sleep for 1, 4, 8, 16.. ms
                        if (i > 0) {
                            Thread.sleep((i * i) * 1000);
                        }
                        try {

                            if (tripType.equals(ReferenceData.BUS_EVENT)) {

                                Demo.msg("Retrying Bus Swipe...");
                                try {

                                    ClientResponse cr2 = callbackClient.callProcedure("CardSwipe", userId, eventId,
                                            tripType, tripDate, subsystem, "", "", jnytyp, busrouteOrStation, product);
                                    notDone = false;
                                    Demo.msg(cr2.getAppStatusString());

                                } catch (Exception e) {
                                    Demo.msg(e.getMessage());
                                }

                            } else if (tripType.equals(ReferenceData.STARTSUBWAY_EVENT)) {

                                Demo.msg("Retrying Tube Board Swipe...");

                                try {

                                    ClientResponse cr2 = callbackClient.callProcedure("CardSwipe", userId, eventId,
                                            tripType, tripDate, subsystem, busrouteOrStation, "", "", "", "");
                                    notDone = false;
                                    Demo.msg(cr2.getAppStatusString());

                                } catch (Exception e) {
                                    Demo.msg(e.getMessage());
                                }

                            } else if (tripType.equals(ReferenceData.ENDSUBWAY_EVENT)) {

                                Demo.msg("Retrying TubeEnd Swipe...");

                                try {

                                    ClientResponse cr2 = callbackClient.callProcedure("CardSwipe", userId, eventId,
                                            tripType, tripDate, subsystem, busrouteOrStation, zvppt, jnytyp, "",
                                            product);
                                    notDone = false;
                                    Demo.msg(cr2.getAppStatusString());

                                } catch (Exception e) {
                                    Demo.msg(e.getMessage());
                                }

                            }

                        } catch (Exception e) {
                            Demo.msg(e.getMessage());
                        }
                    } // for...
                }

            } else {
                shc.incCounter("Bad Swipe, Other than No Money");
            }

        } else {
            shc.reportLatency("WALL_TIME_OK", startMs, "", 4000);
            shc.incCounter("OK");
        }

    }

    /**
     * Map byte status code returned by VoltDB to a String.
     * @param appStatus
     * @return
     */
    public static String mapStatusByte(byte appStatus) {
        String status = "value=" + (appStatus + 0);

        switch (appStatus) {
        case ClientResponse.CONNECTION_LOST:
            status = "CONNECTION LOST";
            break;
        case ClientResponse.CONNECTION_TIMEOUT:
            status = "CONNECTION TIMEOUT";
            break;
        case ClientResponse.GRACEFUL_FAILURE:
            status = "GRACEFUL_FAILURE";
            break;
        case ClientResponse.OPERATIONAL_FAILURE:
            status = "OPERATIONAL_FAILURE ";
            break;
        case ClientResponse.RESPONSE_UNKNOWN:
            status = "RESPONSE UNKNOWN";
            break;
        case ClientResponse.SERVER_UNAVAILABLE:
            status = "SERVER UNAVAILABLE";
            break;
        case ClientResponse.SUCCESS:
            status = "SUCCESS";
            break;
        case ClientResponse.TXN_MISPARTITIONED:
            status = "TXN_MISPARTITIONED";
            break;
        case ClientResponse.TXN_MISROUTED:
            status = "TXN_MISROUTED";
            break;
        case ClientResponse.TXN_RESTART:
            status = "TXN_RESTART";
            break;
        case ClientResponse.UNEXPECTED_FAILURE:
            status = "UNEXPECTED_FAILURE";
            break;
        }

        return status;
    }

}
