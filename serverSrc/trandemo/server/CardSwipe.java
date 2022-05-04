package trandemo.server;

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


import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;
import org.voltdb.types.TimestampType;

public class CardSwipe extends VoltProcedure {
    
 // @formatter:off

    public static final SQLStmt getUser = new SQLStmt("SELECT * FROM transport_user WHERE id = ?;");

    public static final SQLStmt checkDuplicateEvant = new SQLStmt(
            "SELECT user_id FROM event_dup_check WHERE user_id = ? AND event_id = ? AND event_type = ?");

    public static final SQLStmt insertDuplicateEvent = new SQLStmt(
            "INSERT INTO event_dup_check (user_id, event_id, event_type,event_timestamp) VALUES(?,?,?,?);");

    public static final SQLStmt checkSubsystem = new SQLStmt(
            "SELECT subsystem_name FROM subsystem WHERE subsystem_name = ?;");

    public static final SQLStmt checkStation = new SQLStmt("SELECT station_name FROM station WHERE station_name = ?;");

    public static final SQLStmt checkBusroute = new SQLStmt("SELECT route_id FROM busroutes WHERE route_id = ?;");

    public static final SQLStmt checkMinTimes = new SQLStmt("SELECT mptl.how_many, mptl.mintime, tu.last_arr_station "
            + "FROM minimum_plausible_trip_lengths mptl, " + "     transport_user tu " + " WHERE tu.id = ? "
            + " AND   tu.last_arr_station = mptl.start_station AND mptl.end_station = ?;");

    public static final SQLStmt updUserStartTrip = new SQLStmt("UPDATE transport_user "
            + "SET last_seen = ?, active_subway_event_start= ?, " + "    active_subway_event = ?,"
            + "    last_dep_station = ?," + "    last_arr_station = null, swipecount = NVL(swipecount,0) + 1 " + "WHERE id = ?;");

    public static final SQLStmt updUserEndTrip = new SQLStmt("UPDATE transport_user "
            + "SET last_seen = ?, active_subway_event_start= null, " 
            + "    active_subway_event = null,"
            + "    last_dep_station = null," 
            + "    last_arr_station = ?, " 
            + "    swipecount = NVL(swipecount,0) + 1 "
            + "WHERE id = ?;");

    public static final SQLStmt updUserLastFinEvent = new SQLStmt("UPDATE transport_user "
            + "SET last_spend_finevent_date = ?, last_spend_finevent_amount = ? "
            + "WHERE id = ?;");

    
    public static final SQLStmt getBal = new SQLStmt("SELECT credit FROM transport_user_balance WHERE user_id = ?;");

    public static final SQLStmt charge = new SQLStmt("INSERT INTO transport_user_financial_event "
            + "(user_id, event_id, event_timestamp, credit_adjustment, closing_credit, event_comment) VALUES (?,?,?,?,?,?);");

    public static final SQLStmt getFraud = new SQLStmt(
            "SELECT how_many, latest_event FROM transport_user_fraud_status WHERE user_id = ?;");

    public static final SQLStmt reportFraud = new SQLStmt(
            "INSERT INTO transport_user_fraud_event (user_id,event_id ,event_timestamp,event_comment) VALUES (?,?,?,?);");

    public static final SQLStmt logEvent = new SQLStmt(
            "INSERT INTO transport_user_subway_event (user_id, event_id,event_timestamp,start_station, end_station, duration_seconds) "
                    + "VALUES (?,?,?,?,?,?)");

    public static final SQLStmt recordBusJourney = new SQLStmt(
            "INSERT INTO transport_user_bus_event (user_id, event_id,event_timestamp,busroute) " + "VALUES (?,?,?,?)");

    public static final SQLStmt getFare = new SQLStmt(
            "SELECT fare_in_pennies FROM subway_fares WHERE from_station_name = ? AND to_station_name = ?;");

    public static final SQLStmt reportMissingStationPair = new SQLStmt(
            "INSERT INTO missing_subway_fares (from_station_name, to_station_name) VALUES (?,?);");
    
    public static final SQLStmt reportOutcome = new SQLStmt("INSERT INTO user_trip_outcomes (user_id, trip_time, outcome_code,outcome_message) VALUES (?,?,?,?);");

    private static final long NO_CURRENT_JOURNEY = Long.MIN_VALUE;

 
 // @formatter:on
    
    public VoltTable[] run(long userId, long eventId, String eventType, TimestampType eventTime, String subsystem,
            String locationStation, String oysterCardZones, String jnyTyp, String busRouteId, String finalProduct)
            throws VoltAbortException {

        if (!eventType.equals(ReferenceData.BUS_EVENT) && !eventType.equals(ReferenceData.STARTSUBWAY_EVENT)
                && !eventType.equals(ReferenceData.ENDSUBWAY_EVENT)) {

            reportError(userId, eventId, ReferenceData.STATUS_UNKNOWN_EVENT, "Event " + eventType + " not known",eventTime);
            return null;
        }

        // 0. Ask some questions - we'll check the answers in a second...
        voltQueueSQL(getUser, userId);
        voltQueueSQL(checkDuplicateEvant, userId, eventId, eventType);
        voltQueueSQL(checkSubsystem, subsystem);

        if (eventType.equals(ReferenceData.BUS_EVENT)) {
            voltQueueSQL(checkBusroute, busRouteId);
        } else {
            voltQueueSQL(checkStation, locationStation);
        }

        voltQueueSQL(getBal, userId);
        voltQueueSQL(getFraud, userId);

        VoltTable[] results = voltExecuteSQL();
        VoltTable userRecord = results[0];
        VoltTable dupcheckRecord = results[1];
        VoltTable subsystemRecord = results[2];
        VoltTable routeOrStationRecord = results[3];
        VoltTable userCreditRecord = results[4];
        VoltTable userFraudRecord = results[5];

        // 1. Check to see if we know about this user
        if (!userRecord.advanceRow()) {
            reportError(userId, eventId, ReferenceData.STATUS_UNKNOWN_USER, "User " + userId + " not known",eventTime);
            return null;
        }

        // 2. Check to see if we have seen this event before - important to
        // avoid double charging
        if (dupcheckRecord.advanceRow()) {
            reportError(userId, eventId, ReferenceData.STATUS_DUPLICATE_EVENT,
                    "Event " + eventId + "/" + eventType + " already processed",eventTime);
            return null;
        }

        // 3. Sanity check other fields
        if (!subsystemRecord.advanceRow()) {
            reportError(userId, eventId, ReferenceData.STATUS_UNKNOWN_SUBSYSTEM,
                    "Subsystem '" + subsystem + "' not known",eventTime);
            return null;
        }

        if (!routeOrStationRecord.advanceRow()) {

            if (eventType.equals(ReferenceData.BUS_EVENT)) {

                reportError(userId, eventId, ReferenceData.STATUS_UNKNOWN_BUSROUTE,
                        "Bus Route '" + busRouteId + "' not known",eventTime);
                return null;

            } else {

                reportError(userId, eventId, ReferenceData.STATUS_UNKNOWN_STATION,
                        "Station '" + locationStation + "' not known",eventTime);
                return null;
            }
        }

        // get balance
        long currentBal = 0;

        if (!userCreditRecord.advanceRow()) {
            reportError(userId, eventId, ReferenceData.STATUS_NO_FININFO, "No credit info found",eventTime);
            return null;

        } else {
            currentBal = userCreditRecord.getLong("CREDIT");
        }

        if (userFraudRecord.advanceRow()) {

            long fraudCount = userFraudRecord.getLong("HOW_MANY");

            if (fraudCount > ReferenceData.FRAUD_TOLERANCE) {

                TimestampType latestFraud = userFraudRecord.getTimestampAsTimestamp("LATEST_EVENT");
                reportFraud(userId, eventId, ReferenceData.STATUS_KNOWN_FRAUD,
                        "Too many suspicious events. Last was at " + latestFraud.toString(), eventTime);
                return null;
            }

        }

        // Follow different paths based on event Type

        if (eventType.equals(ReferenceData.BUS_EVENT)) {

            // Fraud Detection: See if already on a subway train somewhere else
            long inProgressJourneyId = getCurrentJourney(userRecord);

            if (inProgressJourneyId > 0) {

                final String fraudMessage = userId + " attempted to board bus while already on train journey "
                        + inProgressJourneyId;
                reportFraud(userId, eventId, ReferenceData.STATUS_ON_ANOTHER_TRAIN, fraudMessage, eventTime);
                return null;

            }

            // See if enough credit exists
            long fareNeeded = getBusFare(busRouteId);

            if (currentBal < fareNeeded) {

                reportUserEvent(userId, eventId, ReferenceData.STATUS_NO_MONEY,
                        userId + " attempted to board bus that costs " + fareNeeded + " but only has " + currentBal,
                        eventTime);
                return null;

            }

            // if so log start of journey
            startBus(userId, eventId, busRouteId, eventTime);

            // if so, charge fare, update database, return results
            charge(userId, eventId, "Trip on " + busRouteId, fareNeeded, oysterCardZones, finalProduct, currentBal,
                    eventTime);

            this.setAppStatusCode(ReferenceData.STATUS_OK);
            this.setAppStatusString(userId + " - Bus swipe OK");

        }

        else if (eventType.equals(ReferenceData.STARTSUBWAY_EVENT)) {

            // Fraud Detection: See if already on a subway train somewhere else
            long inProgressJourneyId = getCurrentJourney(userRecord);

            if (inProgressJourneyId != NO_CURRENT_JOURNEY) {

                this.setAppStatusCode(ReferenceData.STATUS_ON_ANOTHER_TRAIN);
                final String fraudMessage = userId + " attempted to board train while already on train journey "
                        + inProgressJourneyId;
                reportFraud(userId, eventId, ReferenceData.STATUS_ON_ANOTHER_TRAIN, fraudMessage, eventTime);
                return null;

            }

            // Fraud Detection: is it possible for them to get to this station
            // since their last journey?
            String suspiciouslyFastCustomer = sanityCheck(userRecord, locationStation, eventTime);
            if (suspiciouslyFastCustomer != null) {

                reportFraud(userId, eventId, ReferenceData.STATUS_MOVING_TOO_FAST, suspiciouslyFastCustomer.toString(),
                        eventTime);
                return null;

            }

            // See if minimum credit exists
            long fareNeeded = getMinimumSubwayFare(locationStation);

            if (currentBal < fareNeeded) {

                reportUserEvent(userId, eventId, ReferenceData.STATUS_NO_MONEY,
                        userId + " attempted to board subway that will cost at least " + fareNeeded + " but only has "
                                + currentBal,
                        eventTime);
                return null;

            }

            // if so log start of journey
            startSubway(userId, eventId, locationStation, eventTime);

            reportUserEvent(userId, eventId, ReferenceData.STATUS_OK, "Subway Swipe OK", eventTime);

        }

        else if (eventType.equals(ReferenceData.ENDSUBWAY_EVENT)) {

            // See if we know where they started

            long tripStart = userRecord.getTimestampAsLong("ACTIVE_SUBWAY_EVENT_START") / 1000;

            if (userRecord.getString("LAST_DEP_STATION") == null) {

                reportError(userId, eventId, ReferenceData.STATUS_NO_TRIP_STARTED,
                        "Ended subway journey at " + locationStation + " but we don't know where they started from"
                             ,eventTime);

                return null;

            }

            // See if enough credit exists

            String startStation = userRecord.getString("LAST_DEP_STATION");
            long fareNeeded = getSubwayFare(startStation, locationStation);

            if (currentBal < fareNeeded) {

                reportUserEvent(userId, eventId,
                        ReferenceData.STATUS_NO_MONEY, "Attempted subway journey from " + startStation + " to "
                                + locationStation + " that costs " + fareNeeded + " but only has " + currentBal,
                        eventTime);
                return null;

            }

            // if so, charge fare, update database, return results
            charge(userId, eventId, "Trip on subway from " + startStation + " to " + locationStation, fareNeeded,
                    oysterCardZones, finalProduct, currentBal, eventTime);

            long durationSecs = ((eventTime.getTime() / 1000) - tripStart) / 1000;

            endSubwayEvent(userId, eventId, startStation, locationStation, durationSecs, eventTime);

            reportUserEvent(userId, eventId, ReferenceData.STATUS_OK, "Subway Exit OK", eventTime);

        } else {

            reportError(userId, eventId, ReferenceData.STATUS_UNKNOWN_EVENT,
                    "Unknown event type of '" + eventType + "' seen.",eventTime);

            return null;
        }

        voltQueueSQL(insertDuplicateEvent, userId, eventId, eventType, eventTime);
        
        

        return voltExecuteSQL(true);
    }

    private void startBus(long userId, long eventId, String busRouteId, TimestampType eventTime) {

        voltQueueSQL(recordBusJourney, userId, eventId, eventTime, busRouteId);
        voltExecuteSQL();

    }

    private void reportUserEvent(long userId, long eventId, byte eventType, String message, TimestampType eventTime) {

        this.setAppStatusCode(eventType);
        this.setAppStatusString("User=" + userId + " Event=" + eventId + " " + message);
        voltQueueSQL(reportOutcome,userId,eventTime,eventType,message);
        voltExecuteSQL();

    }

    private void reportError(long userId, long eventId, byte eventType, String message, TimestampType eventTime) {

        this.setAppStatusCode(eventType);
        this.setAppStatusString("User=" + userId + " Event=" + eventId + " " + message);
        voltQueueSQL(reportOutcome,userId,eventTime,eventType,message);
        voltExecuteSQL();

    }

    private void reportFraud(long userId, long eventId, byte eventType, String message, TimestampType eventTime) {

        this.setAppStatusCode(eventType);
        this.setAppStatusString("User=" + userId + " Event=" + eventId + " " + message);
        voltQueueSQL(reportFraud, userId, eventId, eventTime, message);
        voltExecuteSQL();
    }

    private void endSubwayEvent(long userId, long eventId, String startStation, String locationStation,
            long durationSecs, TimestampType eventTime) {

        voltQueueSQL(updUserEndTrip, eventTime, locationStation, userId);
        voltQueueSQL(logEvent, userId, eventId, eventTime, startStation, locationStation, durationSecs);
        voltExecuteSQL();

    }

    /**
     * Checks to see if it's possible for the user to be at locationStation, based
     * on where & when  he last got off the network, and how long we know it takes
     * to get from the last station he was at to this one..
     * @param userRecord
     * @param locationStation
     * @param eventTime
     * @return
     */
    private String sanityCheck(VoltTable userRecord, String locationStation, TimestampType eventTime) {
        // if we return null there is no problem...
        String scr = null;

        TimestampType lastTime = userRecord.getTimestampAsTimestamp("LAST_SEEN");

        // if it's been more than 2 hours since the user last used a train don't
        // bother...
        if (lastTime != null) {

            long secondsSinceLastJourney = (eventTime.getTime() - lastTime.getTime()) / 1000;
            
            if (secondsSinceLastJourney < 7200) {

                // if he last used the system less than 2 hours ago find out how long it would
                // take us to get from his last station to this station. We then multiply that
                // time by MINIMUM_TRIP_FUDGE_FACTOR. 
                voltQueueSQL(checkMinTimes, userRecord.getLong("ID"), locationStation);
                VoltTable[] scrResults = voltExecuteSQL();

                if (scrResults[0].advanceRow()) {

                    long howMany = scrResults[0].getLong("HOW_MANY");

                    if (howMany > 20) {
                        long minSeconds = scrResults[0].getLong("MINTIME");

                        if ((ReferenceData.MINIMUM_TRIP_FUDGE_FACTOR * minSeconds) < secondsSinceLastJourney) {
                            String otherStation = scrResults[0].getString("LAST_ARR_STATION");

                            scr = "Took " + secondsSinceLastJourney + " to get from " + otherStation + " to "
                                    + locationStation;
                        }
                    } 
                }
            }
        }

        return scr;
    }

    private void startSubway(long userId, long eventId, String locationStation, TimestampType eventTime) {

        voltQueueSQL(updUserStartTrip, EXPECT_ONE_ROW, eventTime, eventTime, eventId, locationStation, userId);
        voltExecuteSQL();

    }

    private void charge(long userId, long eventId, String description, long fareNeeded, String oysterCardZones,
            String finalProduct, long currentBal, TimestampType eventTime) {

        long closingCredit = currentBal - fareNeeded;

        voltQueueSQL(charge, userId, eventId, eventTime, (fareNeeded * -1), closingCredit,
                description + " " + oysterCardZones);
        voltQueueSQL(updUserLastFinEvent, eventTime, (fareNeeded * -1), userId);
        
        
        voltExecuteSQL();
    }

    private long getSubwayFare(String fromStation, String endStation) {

        long fare = ReferenceData.DEFAULT_SUBWAY_FARE;

        voltQueueSQL(getFare, fromStation, endStation);

        VoltTable[] results = voltExecuteSQL();

        if (results[0].advanceRow()) {

            fare = results[0].getLong("FARE_IN_PENNIES");

        } else {

            voltQueueSQL(reportMissingStationPair, fromStation, endStation);
            voltExecuteSQL();

        }

        return fare;
    }

    static private long getMinimumSubwayFare(String locationStation) {

        return ReferenceData.MINIMUM_SUBWAY_FARE;

    }

    static private long getCurrentJourney(VoltTable userRecord) {

        if (userRecord != null) {
            long currentJourney = userRecord.getLong("ACTIVE_SUBWAY_EVENT");

            if (userRecord.wasNull()) {
                return NO_CURRENT_JOURNEY;
            } else {
                return currentJourney;
            }

        }
        return NO_CURRENT_JOURNEY;
    }

    static private long getBusFare(String busRouteId) {

        if (busRouteId.startsWith("N")) {
            return 250;
        }

        return 100;
    }

}
