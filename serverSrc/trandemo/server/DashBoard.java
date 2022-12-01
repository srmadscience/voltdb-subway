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
import org.voltdb.VoltType;
import org.voltdb.types.TimestampType;

public class DashBoard extends VoltProcedure {

   
 // @formatter:off

    public static final SQLStmt getMaxTime = new SQLStmt("select   dateadd(minute, -60, simulation_time) st,dateadd(minute, -1,simulation_time) latest_time,  simulation_time et  from simulation_time order by simulation_time;");
    public static final SQLStmt getSimTime = new SQLStmt("select   dateadd(minute, -1,simulation_time) latest_time, tps_or_speed, number_value from simulation_time, simulation_speed order by simulation_time;");

    public static final SQLStmt getCurrentSubwayUserCount = new SQLStmt("select * from active_subway_users order by how_many, latest_activity;");
    public static final SQLStmt getSubwayStartsPerMin = new SQLStmt("select * from subway_starts_by_min where event_time between ? and ? order by event_time desc limit ?;");
    public static final SQLStmt getSubwayEndsPerMin = new SQLStmt("select * from subway_ends_by_min  where event_time between ? and ?  order by event_time desc limit ?;");
    public static final SQLStmt getBusStartsPerMin = new SQLStmt("select * from bus_event_total_by_minute where event_minute between ? and ? order by event_minute desc limit ?;");
    public static final SQLStmt getBusiestStationPairs = new SQLStmt("SELECT start_station, end_station, sum(how_many) sum_how_many FROM subway_activity_by_minute "
            + "WHERE event_minute BETWEEN ? AND ? GROUP BY  start_station, end_station ORDER BY sum_how_many  DESC, start_station, end_station LIMIT ?; ");
    
    public static final SQLStmt getBusiestBusRoutes = new SQLStmt("SELECT busroute, sum(how_many) sum_how_many "
            + "FROM bus_event_by_minute WHERE event_minute BETWEEN ? AND ? "
            + "GROUP BY busroute ORDER BY sum_how_many DESC, busroute  LIMIT ?; ");
    
    
    public static final SQLStmt getTotalCredit = new SQLStmt("select 'GBP '||format_currency(cast(sum(credit) / 100 as decimal),2)  total_credit_gbp from transport_user_balance;");
    public static final SQLStmt getbadOutcomes = new SQLStmt("select user_id, TRIP_TIME, OUTCOME_MESSAGE from trip_outcome_summary order by trip_time desc, user_id, outcome_code, outcome_message desc limit ?;");
 
    public static final SQLStmt getSubwayStationBoardsPerMin = new SQLStmt("select start_station, sum(how_many) sum_how_many from subway_board_activity_by_minute  where event_minute between ? and ? group by start_station  order by  sum(how_many) desc,start_station limit ?;");
    public static final SQLStmt getSubwayStationEndsPerMin = new SQLStmt("select end_station, sum(how_many) sum_how_many from subway_finish_activity_by_minute  where event_minute between ? and ? group by end_station  order by  sum(how_many)  desc, end_station limit ?;");
    
    
    public static final SQLStmt getSpending = new SQLStmt("select 'GBP '||format_currency(cast (nvl(sum(last_add_finevent_amount)/100,0) as decimal),2) Purchases from transport_user where truncate(minute,LAST_ADD_FINEVENT_DATE) = ? ");
    public static final SQLStmt getPurchases= new SQLStmt("select 'GBP '||format_currency(cast (nvl(sum(last_spend_finevent_amount)/100,0) as decimal),2) Spending from transport_user where truncate(minute,LAST_SPEND_FINEVENT_DATE) = ?");
    
    // @formatter:on

    
    public VoltTable[] run(int howMany) throws VoltAbortException {
        
        // Get current minute and hour
        TimestampType currentStartMinute = new TimestampType(this.getTransactionTime());
        TimestampType currentEndMinute = new TimestampType(this.getTransactionTime());
        TimestampType latestTime = new TimestampType(this.getTransactionTime());
 
        voltQueueSQL(getMaxTime); 
        
        VoltTable[] times = voltExecuteSQL();      
        
        if (times[0].advanceRow()) {
            currentStartMinute = times[0].getTimestampAsTimestamp("ST");
            currentEndMinute = times[0].getTimestampAsTimestamp("ET");
            latestTime =  times[0].getTimestampAsTimestamp("LATEST_TIME");
        }
        
        voltQueueSQL(getSimTime);
        voltQueueSQL(getCurrentSubwayUserCount);  
        voltQueueSQL(getSubwayStartsPerMin, currentStartMinute, currentEndMinute, 1);  
        voltQueueSQL(getSubwayEndsPerMin, currentStartMinute, currentEndMinute, 1); 
        voltQueueSQL(getBusStartsPerMin, currentStartMinute, currentEndMinute, 1); 
        voltQueueSQL(getBusiestStationPairs, currentStartMinute,currentEndMinute,howMany); 
        voltQueueSQL(getBusiestBusRoutes, currentStartMinute,currentEndMinute, (howMany * 2 / 3)); 
        voltQueueSQL(getTotalCredit);  
        voltQueueSQL(getbadOutcomes, howMany);
        voltQueueSQL(getSubwayStationBoardsPerMin, currentStartMinute,currentEndMinute, howMany); 
        voltQueueSQL(getSubwayStationEndsPerMin, currentStartMinute,currentEndMinute, howMany); 
        
        voltQueueSQL(getSpending, latestTime); 
        voltQueueSQL(getPurchases, latestTime); 
        
        return voltExecuteSQL(true);
       
    }
}
