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
    public static int isOnTurtle = -1;
    public static void doAction() throws GameActionException {
    	reportLocation();
        if (isOnTurtle == -1) {
            // check our elevation to see if its >= turtle elevation
            if (rc.canSenseLocation(rc.getLocation())) {
                if (rc.senseElevation(rc.getLocation()) >= lowerTurtleHeight) {
                    isOnTurtle = 1;
                } else {
                    isOnTurtle = 0;
                }
            }
        }
        if (!rc.isReady()) {
            return;
        }
        if (tryBuildLandscaper()) {
        	return;
        }
    }
    public static int numberOfLandscapersWanted() {
        if (isOnTurtle == 0) {
            return 10;
        } else {
            return Math.min(10, rc.getRoundNum()/100);
        }
    }

    public static int landscapersBuilt = 0;
    public static boolean tryBuildLandscaper() throws GameActionException {
    	if (landscapersBuilt >= numberOfLandscapersWanted()) {
    		// TODO: Not this
    		return false;
    	}
    	for (Direction dir : directions) {
            if (tryBuildInDir(RobotType.LANDSCAPER, dir, false)) {
                landscapersBuilt++;
                System.out.println("Building landscaper " + landscapersBuilt);
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
