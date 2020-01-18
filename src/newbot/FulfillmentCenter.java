package newbot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

public class FulfillmentCenter extends RobotPlayer {
    public static void runFulfillmentCenter() throws GameActionException {
        doAction();
    }
    public static void doAction() throws GameActionException {
    	reportLocation();
        if (!rc.isReady()) {
            return;
        }
        if (tryBuildDrone()) {
        	return;
        }
    }

    public static int numberOfDronesWanted() {
        return Math.min(10, rc.getRoundNum()/100);
    }

    public static int dronesBuilt = 0;
    public static boolean tryBuildDrone() throws GameActionException {
    	if (dronesBuilt >= numberOfDronesWanted()) {
    		// TODO: Not this
    		return false;
    	}
    	for (Direction dir : directions) {
            if (tryBuildInDir(RobotType.DELIVERY_DRONE, dir, false)) {
                dronesBuilt++;
                System.out.println("Building drone " + dronesBuilt);
                return true;
            }
        }
        return false;
    }

    public static boolean reportedLocation = false;
    public static void reportLocation() throws GameActionException {
        if (reportedLocation) {
            return;
        }
        int myLoc = rc.getLocation().x*MAX_MAP_SIZE + rc.getLocation().y;
        int message[] = new int[7];
        message[0] = MESSAGE_TYPE_FULFILLMENT_CENTER_LOC;
        message[1] = myLoc;
        if (sendBlockchain(message, 1)) {
            reportedLocation = true;
        }
    }
}
