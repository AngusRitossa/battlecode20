package newbot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

public class DesignSchool extends RobotPlayer {
    public static void runDesignSchool() throws GameActionException {
        readBlockchain(9000);
    	doAction();
    	readBlockchain(1000);
    }

    public static void doAction() throws GameActionException {
    	reportLocation();
        if (!rc.isReady()) {
            return;
        }
        if (tryBuildLandscaper()) {
        	return;
        }
    }

    public static int landscapersBuilt = 0;
    public static boolean tryBuildLandscaper() throws GameActionException {
    	if (landscapersBuilt >= 20) {
    		// TODO: Not this
    		return false;
    	}
    	for (Direction dir : directions) {
            if (tryBuildInDir(RobotType.LANDSCAPER, dir, false)) {
                landscapersBuilt++;
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
        message[0] = MESSAGE_TYPE_DESIGN_SCHOOL_LOC;
        message[1] = myLoc;
        if (sendBlockchain(message, 1)) {
            reportedLocation = true;
        }
    }
}
