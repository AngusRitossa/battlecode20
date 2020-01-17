package newbot;

import battlecode.common.GameActionException;

public class Refinery extends RobotPlayer {
    public static void runRefinery() throws GameActionException {
    	readBlockchain(9000);
    	doAction();
    	readBlockchain(1000);
    }
    
    public static void doAction() throws GameActionException {
    	reportLocation();
        if (!rc.isReady()) {
            return;
        }
    }

    public static boolean reportedLocation = false;
    public static void reportLocation() throws GameActionException {
        if (reportedLocation) {
            return;
        }
        int myLoc = rc.getLocation().x*MAX_MAP_SIZE + rc.getLocation().y;
        int message[] = new int[7];
        message[0] = MESSAGE_TYPE_REFINERY_LOC;
        message[1] = myLoc;
        if (sendBlockchain(message, 1)) {
            reportedLocation = true;
        }
    }
}
