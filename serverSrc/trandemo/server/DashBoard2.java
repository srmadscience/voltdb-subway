package trandemo.server;

/* This file is part of VoltDB.
 * Copyright (C) 2008-2017 VoltDB Inc.
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

public class DashBoard2 extends VoltProcedure {

 // @formatter:off

    public static final SQLStmt deleteStats = new SQLStmt("DELETE FROM sim_stats;");
    public static final SQLStmt addStat = new SQLStmt("INSERT INTO sim_stats (stat_name,stat_label_1, stat_label_2, latitude, longitude, stat_value) VALUES (?,?,?,?,?,?);");

    public static final SQLStmt getMaxTime = new SQLStmt("select   dateadd(minute, -60, simulation_time) st"
            + ",dateadd(minute, -1,simulation_time) latest_time"
            + ",  simulation_time et  "
            + "from simulation_time "
            + "order by simulation_time;");
    public static final SQLStmt getSimTime = new SQLStmt("select   dateadd(minute, -1,simulation_time) latest_time"
            + ",  since_epoch(SECOND, simulation_time) simulation_time_epoch "
            + ",  dayofweek(simulation_time) dayofweek"
            + ",  hour(simulation_time) t_hour"
            + ",  minute(simulation_time) t_minute "
            + ", tps_or_speed"
            + ", number_value "
            + "from simulation_time"
            + "   , simulation_speed"
            + " order by simulation_time;");
    public static final SQLStmt getCurrentSubwayUserCount = new SQLStmt("select * from active_subway_users order by how_many, latest_activity;");
    public static final SQLStmt getSubwayStartsPerMin = new SQLStmt("select * from subway_starts_by_min where event_time between ? and ? order by event_time desc limit ?;");
    public static final SQLStmt getSubwayEndsPerMin = new SQLStmt("select * from subway_ends_by_min  where event_time between ? and ?  order by event_time desc limit ?;");
    public static final SQLStmt getBusStartsPerMin = new SQLStmt("select * from bus_event_total_by_minute where event_minute between ? and ? order by event_minute desc limit ?;");
    public static final SQLStmt getBusiestStationPairs = new SQLStmt("SELECT start_station, end_station, sum(how_many) sum_how_many FROM subway_activity_by_minute "
            + "WHERE event_minute BETWEEN ? AND ? GROUP BY  start_station, end_station ORDER BY sum_how_many  DESC, start_station, end_station LIMIT ?; ");

    public static final SQLStmt getBusiestBusRoutes = new SQLStmt("SELECT busroute, sum(how_many) sum_how_many "
            + "FROM bus_event_by_minute WHERE event_minute BETWEEN ? AND ? "
            + "GROUP BY busroute ORDER BY sum_how_many DESC, busroute  LIMIT ?; ");


   // public static final SQLStmt getTotalCredit = new SQLStmt("select sum(credit) total_credit_gbp from transport_user_balance;");
    public static final SQLStmt getbadOutcomes = new SQLStmt("select user_id, TRIP_TIME, OUTCOME_MESSAGE from trip_outcome_summary order by trip_time desc, user_id, outcome_code, outcome_message desc limit ?;");

    public static final SQLStmt getSubwayStationBoardsPerMin = new SQLStmt("select start_station, sum(how_many) sum_how_many from subway_board_activity_by_minute  where event_minute between ? and ? group by start_station  order by  sum(how_many) desc,start_station limit ?;");
    public static final SQLStmt getSubwayStationEndsPerMin = new SQLStmt("select end_station, sum(how_many) sum_how_many from subway_finish_activity_by_minute  where event_minute between ? and ? group by end_station  order by  sum(how_many)  desc, end_station limit ?;");


    public static final SQLStmt getPurchases  = new SQLStmt("select nvl(sum(last_add_finevent_amount),0) Purchases from transport_user where truncate(minute,LAST_ADD_FINEVENT_DATE) = ? ");
    public static final SQLStmt getSpending = new SQLStmt("select nvl(sum(last_spend_finevent_amount),0) Spending from transport_user where truncate(minute,LAST_SPEND_FINEVENT_DATE) = ?");

    public static final SQLStmt getBusiestStartStations = new SQLStmt("SELECT start_station station, sum(how_many) sum_how_many, latitude, longitude  "
            + "FROM subway_activity_by_minute, station "
            + "WHERE event_minute BETWEEN ? AND ? "
            + "AND start_station = station_name "
            + "GROUP BY  start_station, latitude, longitude  "
            + "ORDER BY sum_how_many  DESC, start_station LIMIT ?; ");

    public static final SQLStmt getBusiestEndStations = new SQLStmt("SELECT end_station station, sum(how_many) sum_how_many, latitude, longitude  "
            + "FROM subway_activity_by_minute, station "
            + "WHERE event_minute BETWEEN ? AND ? "
            + "AND end_station = station_name "
            + "GROUP BY  end_station, latitude, longitude  "
            + "ORDER BY sum_how_many  DESC, end_station LIMIT ?; ");




    // @formatter:on

    public static final double[] LATENCY_PERCENTILES = { 50, 95, 99, 99.9, 99.99, 99.999, 100 };
    public static final String[] SERVER_LATENCY_PERCENTILES = { "MIN_EXECUTION_TIME", "AVG_EXECUTION_TIME",
            "MAX_EXECUTION_TIME" };

    public VoltTable[] run(int howMany, double[] wallPercentiles, double[] serverPercentiles)
            throws VoltAbortException {

        // Get current minute and hour
        TimestampType currentStartMinute = new TimestampType(this.getTransactionTime());
        TimestampType currentEndMinute = new TimestampType(this.getTransactionTime());
        TimestampType latestTime = new TimestampType(this.getTransactionTime());

        voltQueueSQL(getMaxTime);
        voltQueueSQL(deleteStats);

        VoltTable[] times = voltExecuteSQL();

        if (times[0].advanceRow()) {
            currentStartMinute = times[0].getTimestampAsTimestamp("ST");
            currentEndMinute = times[0].getTimestampAsTimestamp("ET");
            latestTime = times[0].getTimestampAsTimestamp("LATEST_TIME");
        }

        //
        //
        voltQueueSQL(getSubwayStartsPerMin, currentStartMinute, currentEndMinute, 1);
        voltQueueSQL(getSubwayEndsPerMin, currentStartMinute, currentEndMinute, 1);
        voltQueueSQL(getBusStartsPerMin, currentStartMinute, currentEndMinute, 1);
        voltQueueSQL(getBusiestStationPairs, currentStartMinute, currentEndMinute, howMany);
        voltQueueSQL(getBusiestBusRoutes, currentStartMinute, currentEndMinute, (howMany * 2 / 3));
        voltQueueSQL(getCurrentSubwayUserCount);
        // voltQueueSQL(getTotalCredit);
        voltQueueSQL(getSubwayStationBoardsPerMin, currentStartMinute, currentEndMinute, howMany);
        voltQueueSQL(getSubwayStationEndsPerMin, currentStartMinute, currentEndMinute, howMany);

        voltQueueSQL(getSpending, latestTime);
        voltQueueSQL(getPurchases, latestTime);

        voltQueueSQL(getbadOutcomes, howMany);

        voltQueueSQL(getSimTime);

        voltQueueSQL(getBusiestStartStations, currentStartMinute, currentEndMinute, howMany * 5);
        voltQueueSQL(getBusiestEndStations, currentStartMinute, currentEndMinute, howMany * 5);

        VoltTable[] firstResults = voltExecuteSQL();

        addStat("subway", "subway_start", "boardings_this_min", getValue("boardings_this_min", firstResults[0]));
        addStat("subway", "subway_end", "ends_this_min", getValue("ends_this_min", firstResults[1]));
        addStat("bus", "bus_start", null, getValue("how_many", firstResults[2]));
        addBusiestStationPairs(firstResults[3]);
        addBusiestBusRoutes(firstResults[4]);

        addStat("subway_totals", "current_users", null, getValue("how_many", firstResults[5]));
      

        addStat("subway_totals", "boards_per_minute", null, getValue("SUM_HOW_MANY", firstResults[6]));
        addStat("subway_totals", "ends_per_minute", null, getValue("SUM_HOW_MANY", firstResults[7]));

        addStat("subway_totals", "spending_per_minute", null, getValue("Spending", firstResults[8]));
        addStat("subway_totals", "purchases_per_minute", null, getValue("Purchases", firstResults[9]));
        addSimTimeStats(firstResults[11]);
        addBusiestStations(firstResults[12], "starting");
        addBusiestStations(firstResults[13], "ending");
        addPercentileStats("client", wallPercentiles);
        addPercentileStats("server", serverPercentiles);

        return voltExecuteSQL(true);

    }

    private void addPercentileStats(String kind, double[] percentiles) {

        if (percentiles.length == LATENCY_PERCENTILES.length) {
            for (int i = 0; i < LATENCY_PERCENTILES.length; i++) {
                addStat("latency_" + kind, "P" + LATENCY_PERCENTILES[i], null, percentiles[i]);

            }

        } else {
            for (int i = 0; i < SERVER_LATENCY_PERCENTILES.length; i++) {
                addStat("latency_" + kind, SERVER_LATENCY_PERCENTILES[i], null, percentiles[i]);

            }

        }

    }

    private void addBusiestStations(VoltTable voltTable, String kind) {

        while (voltTable.advanceRow()) {
            String station = voltTable.getString("station");
            double longitude = voltTable.getDouble("longitude");
            double latitude = voltTable.getDouble("latitude");
            long howMany = voltTable.getLong("sum_how_many");
            addLatLongStat("busy_" + kind + "stations", station, station, latitude, longitude, howMany);
        }

    }

    private void addSimTimeStats(VoltTable voltTable) {
        voltTable.advanceRow();
        addStat("subway_time", "seconds_since_epoch", null, getValue("simulation_time_epoch", voltTable));
        addStat("subway_time", "dayofweek", null, getValue("dayofweek", voltTable));
        addStat("subway_time", "hour", null, getValue("t_hour", voltTable));
        addStat("subway_time", "minute", null, getValue("t_minute", voltTable));
    }

    private void addBusiestBusRoutes(VoltTable voltTable) {

        while (voltTable.advanceRow()) {
            String busRoute = voltTable.getString("busroute");
            long howMany = voltTable.getLong("sum_how_many");
            addStat("busy_bus_routes", "",busRoute, howMany);
        }

    }

    private void addBusiestStationPairs(VoltTable voltTable) {

        while (voltTable.advanceRow()) {
            String startStation = voltTable.getString("start_station");
            String endStation = voltTable.getString("end_station");
            long howMany = voltTable.getLong("sum_how_many");
            addStat("busy_station_pairs", startStation, endStation, howMany);
        }

    }

    private long getValue(String column, VoltTable voltTable) {
        long value = 0;

        voltTable.resetRowPosition();

        if (voltTable.advanceRow()) {
            value = voltTable.getLong(column);
        }
        return value;
    }

    private void addLatLongStat(String statName, String label1, String label2, double latitude, double longitude,
            double statValue) {
        voltQueueSQL(addStat, statName, label1, label2, latitude, longitude, statValue);

    }

    private void addStat(String statName, String label1, String label2, double value) {
        voltQueueSQL(addStat, statName, label1, label2, null, null, value);
    }
}
