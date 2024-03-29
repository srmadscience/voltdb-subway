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

public class UpdateSimulationTime extends VoltProcedure {

    // @formatter:off

    public static final SQLStmt delTime = new SQLStmt("DELETE FROM simulation_time;");
    public static final SQLStmt insertTime = new SQLStmt("INSERT INTO simulation_time (simulation_time) VALUES (?);");
    public static final SQLStmt getSpeed = new SQLStmt("SELECT * FROM simulation_speed;");

    // @formatter:on

    public VoltTable[] run(TimestampType newTime) throws VoltAbortException {

        voltQueueSQL(delTime);
        voltQueueSQL(insertTime, newTime);
        voltQueueSQL(getSpeed);
        
        return voltExecuteSQL(true);
       
    }

}
