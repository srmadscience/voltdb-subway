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
import org.voltdb.VoltType;
import org.voltdb.types.TimestampType;

public class MeasureThroughput extends VoltProcedure {

    // @formatter:off

    public static final SQLStmt getMaxEvent = new SQLStmt("SELECT total_swipes FROM total_swipes;");
    public static final SQLStmt getSimTime = new SQLStmt("select   dateadd(minute, -1,simulation_time) latest_time, tps_or_speed, number_value from simulation_time, simulation_speed order by simulation_time;");
 
    // @formatter:on

    public VoltTable[] run() throws VoltAbortException {

        voltQueueSQL(getMaxEvent);
        voltQueueSQL(getSimTime);

        VoltTable[] firstResults = voltExecuteSQL(true);

        VoltTable t = new VoltTable(new VoltTable.ColumnInfo("TS", VoltType.BIGINT),
                new VoltTable.ColumnInfo("EVENTS", VoltType.BIGINT));

        long lastMinUnixTimestamp = this.getTransactionTime().getTime();
        long howMany = 0;

        if (firstResults[0].advanceRow()) {
            howMany = firstResults[0].getLong(0);
        }

        t.addRow(lastMinUnixTimestamp, howMany);

        VoltTable[] secondResults = { t , firstResults[0] };

        return secondResults;

    }
}
