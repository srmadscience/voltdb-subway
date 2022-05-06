package trandemo.server;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class ResetData extends VoltProcedure {

    final SQLStmt sqDelStation = new SQLStmt("DELETE FROM STATIONS;");
    final SQLStmt sqlDelProduct = new SQLStmt("DELETE FROM PRODUCTS;");
    final SQLStmt sqlDelSubsystem = new SQLStmt("DELETE FROM SUBSYSTEMS;");
    final SQLStmt sqlDelJourney = new SQLStmt("DELETE FROM JOURNEYS;");
    final SQLStmt sqlDelBusRides = new SQLStmt("DELETE FROM BUSRIDES;");
    final SQLStmt sqlDelSubwayRides = new SQLStmt("DELETE FROM SUBWAY_RIDES;");
    
    

    public VoltTable[] run() {

        this.voltQueueSQL(sqDelStation);
        this.voltQueueSQL(sqlDelProduct);
        this.voltQueueSQL(sqlDelSubsystem);
        this.voltQueueSQL(sqlDelJourney);
        this.voltQueueSQL(sqlDelBusRides);
        this.voltQueueSQL(sqlDelSubwayRides);
        
        return voltExecuteSQL(true);
        

    }

}