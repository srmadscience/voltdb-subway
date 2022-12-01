package trandemo.server;

/* This file is part of VoltDB.
 * Copyright (C) 2008-2022 VoltDB Inc.
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

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class ResetDatabase extends VoltProcedure {

    // @formatter:off

    public static final SQLStmt deleteProducts = new SQLStmt("DELETE FROM products ;");
    
   
    public static final SQLStmt deleteSubsystems = new SQLStmt("DELETE FROM subsystem;");
    public static final SQLStmt deleteBusroutes = new SQLStmt("DELETE FROM busroutes;");
    public static final SQLStmt deleteUser = new SQLStmt("DELETE FROM transport_user;");
    public static final SQLStmt deleteUserBalance = new SQLStmt("DELETE FROM transport_user_balance;");
    public static final SQLStmt deleteDupCheck = new SQLStmt("DELETE FROM event_dup_check;");
    public static final SQLStmt deleteBusEvent = new SQLStmt("DELETE FROM transport_user_bus_event;");
    public static final SQLStmt deleteTUSE = new SQLStmt("DELETE FROM TRANSPORT_USER_SUBWAY_EVENT;");
    public static final SQLStmt deleteTUFS = new SQLStmt("DELETE FROM transport_user_fraud_status;");
    public static final SQLStmt deleteEventCodes = new SQLStmt("DELETE FROM event_code;");
    public static final SQLStmt deleteTripOutcomes = new SQLStmt("DELETE FROM trip_outcome_summary;");
    
    

    public static final SQLStmt insertProducts = new SQLStmt("INSERT INTO products VALUES (?);");
    public static final SQLStmt insertEventCodes = new SQLStmt("INSERT INTO event_code (code, code_desc) VALUES (?,?);");
    public static final SQLStmt insertBusRoutes = new SQLStmt("INSERT INTO busroutes VALUES (?);");
    public static final SQLStmt insertStations = new SQLStmt("UPSERT INTO station (station_name) VALUES (?);");
    public static final SQLStmt insertSubsystem = new SQLStmt("INSERT INTO subsystem VALUES (?,?);");
    
    // @formatter:on

    StringBuffer statusString = null;

    public VoltTable[] run() throws VoltAbortException {

        statusString = new StringBuffer("ResetDatabase: ");
        deleteAll();

        // @formatter:off

        voltQueueSQL(insertSubsystem,"DLR","Docklands Light Railway");
        voltQueueSQL(insertSubsystem,"DLR/LRC","Docklands Light Railway/London Overground");
        voltQueueSQL(insertSubsystem,"HEX","Heathrow Express");
        voltQueueSQL(insertSubsystem,"LRC","London Overground");
        voltQueueSQL(insertSubsystem,"LTB","London Buses");
        voltQueueSQL(insertSubsystem,"LUL","London Underground");
        voltQueueSQL(insertSubsystem,"LUL/DLR","London Underground/Docklands Light Railway");
        voltQueueSQL(insertSubsystem,"LUL/LRC","London Underground/London Overground");
        voltQueueSQL(insertSubsystem,"LUL/NR","London Underground/National Rail");
        voltQueueSQL(insertSubsystem,"LUL/NR/DLR","London Underground/National Rail/Docklands Light Railway"); 
        voltQueueSQL(insertSubsystem,"LUL/NR/LRC","London Underground/National Rail/London Overground");
        voltQueueSQL(insertSubsystem,"LUL/TRAM","London Underground/Croydon Tram");
        voltQueueSQL(insertSubsystem,"NR","National Rail");
        voltQueueSQL(insertSubsystem,"NR/DLR","National Rail/Docklands Light Railway"); 
        voltQueueSQL(insertSubsystem,"NR/LRC","National Rail/London Overground");
        voltQueueSQL(insertSubsystem,"TRAM","Croydon Tram");
        
        statusString.append("Added Subsystems; ");
             
        voltQueueSQL(insertEventCodes,ReferenceData.STATUS_OK,"Swipe OK");
        voltQueueSQL(insertEventCodes,ReferenceData.STATUS_BAD_DFARE,"Bad Discount Fare");
        voltQueueSQL(insertEventCodes,ReferenceData.STATUS_BAD_FFARE,"Bad Full Fare");
        voltQueueSQL(insertEventCodes,ReferenceData.STATUS_DUPLICATE_EVENT,"Duplicate Event Proffered");
        voltQueueSQL(insertEventCodes,ReferenceData.STATUS_KNOWN_FRAUD,"User has many fradulent events");
        voltQueueSQL(insertEventCodes,ReferenceData.STATUS_MOVING_TOO_FAST,"User moving too quickly between stations");
        voltQueueSQL(insertEventCodes,ReferenceData.STATUS_NO_FININFO,"No financial info available");
        voltQueueSQL(insertEventCodes,ReferenceData.STATUS_NO_MONEY,"User doesn't have enough money to pay");
        voltQueueSQL(insertEventCodes,ReferenceData.STATUS_NO_TRIP_STARTED,"User finished a subway trip that doesn't appear to have started");
        voltQueueSQL(insertEventCodes,ReferenceData.STATUS_ON_ANOTHER_TRAIN,"User is currently on another train");
        voltQueueSQL(insertEventCodes,ReferenceData.STATUS_UNKNOWN_BUSROUTE,"Bus Route not known");
        voltQueueSQL(insertEventCodes,ReferenceData.STATUS_UNKNOWN_EVENT,"Unrecognized event type");
        voltQueueSQL(insertEventCodes,ReferenceData.STATUS_UNKNOWN_STATION,"Unrecognized station");
        voltQueueSQL(insertEventCodes,ReferenceData.STATUS_UNKNOWN_SUBSYSTEM,"Unrecognized subsystem");
        voltQueueSQL(insertEventCodes,ReferenceData.STATUS_UNKNOWN_USER,"User not known");

        // @formatter:on

        statusString.append("Added Event Codes; ");

        for (int i = 0; i < ReferenceData.STATIONS.length; i++) {
            voltQueueSQL(insertStations, ReferenceData.STATIONS[i]);
        }
        statusString.append("Added " + ReferenceData.STATIONS.length + " stations; ");

        for (int i = 0; i < ReferenceData.BUSROUTES.length; i++) {
            voltQueueSQL(insertBusRoutes, ReferenceData.BUSROUTES[i]);

        }
        statusString.append("Added " + ReferenceData.BUSROUTES.length + " bus routes; ");

        for (int i = 0; i < ReferenceData.PRODUCTS.length; i++) {
            voltQueueSQL(insertProducts, ReferenceData.PRODUCTS[i]);
        }
        statusString.append("Added " + ReferenceData.PRODUCTS.length + " products; ");

        this.setAppStatusString(statusString.toString());

        voltExecuteSQL(true);
        return null;
    }

    private void deleteAll() {

        voltQueueSQL(deleteProducts);
       // voltQueueSQL(deleteStations);
        voltQueueSQL(deleteSubsystems);
        voltQueueSQL(deleteBusroutes);
        voltQueueSQL(deleteUser);
        voltQueueSQL(deleteUserBalance);
        voltQueueSQL(deleteDupCheck);
        voltQueueSQL(deleteBusEvent);
        voltQueueSQL(deleteTUSE);
        voltQueueSQL(deleteTUFS);
        voltQueueSQL(deleteEventCodes);
        voltQueueSQL(deleteTripOutcomes);

        voltExecuteSQL();

    }

}
