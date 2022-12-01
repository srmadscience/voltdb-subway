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

public class UpdateSubwayTripPrice extends VoltProcedure {

    // @formatter:off

    public static final SQLStmt getFare = new SQLStmt("SELECT * FROM subway_fares WHERE from_station_name = ? AND to_station_name = ?;");
    public static final SQLStmt insertFare = new SQLStmt("INSERT INTO subway_fares (from_station_name, to_station_name, fare_in_pennies) VALUES (?,?,?);");
    public static final SQLStmt updateFare = new SQLStmt("UPDATE subway_fares SET fare_in_pennies = ?, discount_fare_in_pennies = ? "
            + " WHERE from_station_name = ? AND to_station_name = ?;");

    // @formatter:on
    
    public VoltTable[] run(String fromStation, String toStation, long fareInPennies) throws VoltAbortException {

        voltQueueSQL(getFare, fromStation, toStation );
        
        VoltTable[] results =voltExecuteSQL();
        
        if (results[0].advanceRow()) {
            // Get default fare
            long defaultFare = results[0].getLong("fare_in_pennies");
            long discountFare = results[0].getLong("discount_fare_in_pennies");
            
            if (fareInPennies > defaultFare) {
                
                discountFare = defaultFare;
                defaultFare = fareInPennies;
            } else {
                discountFare = fareInPennies;
            }
            
            voltQueueSQL(updateFare,defaultFare,discountFare , fromStation,toStation);
            
        } else {
          // no row - do an insert  
            voltQueueSQL(insertFare, fromStation,toStation,fareInPennies );
        }
        
        return voltExecuteSQL(true);
      
    }

}
