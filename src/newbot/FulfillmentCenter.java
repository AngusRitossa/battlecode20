package newbot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

public class FulfillmentCenter extends RobotPlayer {
    public static void runFulfillmentCenter() throws GameActionException {
        readBlockchain(9000);
        doAction();
        readBlockchain(2000);
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
        if (rc.getRoundNum() < 200) {
            return 2;
        }
        if (rc.getRoundNum() < 400) {
            return 4;
        }
        if (knownVaporators.size() > 32) {
            return rc.getRoundNum()/20;
        }
        if (rc.getRoundNum() > water_level_round[lowerTurtleHeight]-250) {
            return 99999;
        }
        return rc.getRoundNum()/100;
    }

    public static int dronesBuilt = 0;
    public static boolean tryBuildDrone() throws GameActionException {
    	if (dronesBuilt >= numberOfDronesWanted()) {
    		// TODO: Not this
    		return false;
    	}
    	for (Direction dir : directions) {
            if (tryBuildInDir(RobotType.DELIVERY_DRONE, dir, dronesBuilt == 0)) {
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
