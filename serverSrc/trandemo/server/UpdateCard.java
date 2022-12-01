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
import org.voltdb.types.TimestampType;

public class UpdateCard extends VoltProcedure {

    // @formatter:off

    public static final SQLStmt getUser = new SQLStmt("SELECT id FROM transport_user WHERE id = ?;");
    
    public static final SQLStmt getBalance = new SQLStmt(
            "SELECT credit FROM transport_user_balance WHERE user_id = ?;");

    public static final SQLStmt addUser = new SQLStmt("INSERT INTO transport_user (id, first_seen,last_seen) VALUES (?,?,?);");
    
    public static final SQLStmt updUser = new SQLStmt("UPDATE transport_user SET last_seen = ?, last_add_finevent_amount = ?, last_add_finevent_date = ? WHERE ID = ?;");

    public static final SQLStmt addCredit = new SQLStmt(
            "INSERT INTO transport_user_financial_event (user_id   ,event_id     ,event_timestamp     "
                    + ",credit_adjustment ,closing_credit   ,event_comment) VALUES (?,?,?,?,?,?);");

    // @formatter:on
    
    public VoltTable[] run(long userId, long addBalance, String isNew,TimestampType eventTime) throws VoltAbortException {

        long currentBalance = 0;

        voltQueueSQL(getUser, userId);
        voltQueueSQL(getBalance, userId);

        VoltTable[] results = voltExecuteSQL();

        if (isNew.equalsIgnoreCase("Y")) {

            if (results[0].advanceRow()) {
                throw new VoltAbortException("User " + userId + " exists but shouldn't");
            }
            
            currentBalance = addBalance;
            
            final String status = "Created user " + userId + " with opening credit of " + addBalance;
            voltQueueSQL(addUser, userId, eventTime, eventTime);
            voltQueueSQL(addCredit, userId, -1, eventTime, currentBalance, currentBalance,
                   status );
            this.setAppStatusCode(ReferenceData.STATUS_OK);
            this.setAppStatusString(status);

        } else {

            if (!results[0].advanceRow()) {
                throw new VoltAbortException("User " + userId + " does not exist");
            }

            if (!results[1].advanceRow()) {
                throw new VoltAbortException("User " + userId + " exists but has no financial history...");
            }

            currentBalance = results[1].getLong("CREDIT") + addBalance;
            
            final String status ="Updated user " + userId + " - added credit of " + addBalance + "; balance now " + currentBalance;

            
                    
            voltQueueSQL(updUser, eventTime, addBalance, eventTime, userId);
            voltQueueSQL(addCredit, userId, -1, eventTime, addBalance, currentBalance,status);
 
            
            this.setAppStatusCode(ReferenceData.STATUS_OK);
            this.setAppStatusString(status);

        }

        return voltExecuteSQL(true);
    }
}
