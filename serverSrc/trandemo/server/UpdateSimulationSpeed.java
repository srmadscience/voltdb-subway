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
import org.voltdb.VoltType;
import org.voltdb.types.TimestampType;

public class UpdateSimulationSpeed extends VoltProcedure {

    // @formatter:off

    public static final SQLStmt delSpeed = new SQLStmt("DELETE FROM simulation_speed;");
    public static final SQLStmt insertSpeed = new SQLStmt("INSERT INTO simulation_speed (tps_or_speed, number_value) VALUES (?,?);");

    // @formatter:on

    public VoltTable[] run(String tpsOrSpeed, int numberValue) throws VoltAbortException {

        if ((tpsOrSpeed.equalsIgnoreCase("TPS") || tpsOrSpeed.equalsIgnoreCase("SPEED")) && numberValue > 0) {

            voltQueueSQL(delSpeed);
            voltQueueSQL(insertSpeed, tpsOrSpeed, numberValue);

        } else {
            throw new VoltAbortException("Parameters " + tpsOrSpeed + "," + numberValue + " not usable");
        }

        return voltExecuteSQL(true);

    }

}
