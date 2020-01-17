package newbot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

public class HQ extends RobotPlayer {
    public static void runHQ() throws GameActionException {
        readBlockchain(9000);
        doAction();
        readBlockchain(1000);
    }

    public static void doAction() throws GameActionException {
        reportLocation();
        if (!rc.isReady()) {
            return;
        }
        if (tryShootUnit()) {
            return;
        }
        if (tryBuildMiner()) {
            return;
        }
    }

    public static int minersBuilt = 0;
    public static boolean tryBuildMiner() throws GameActionException {
        // TODO: Don't just build miners
        if (minersBuilt >= 8 || minersBuilt >= rc.getRoundNum()/40 + 2) {
            return false;
        }
        for (Direction dir : directions) {
            if (tryBuildInDir(RobotType.MINER, dir, false)) {
                minersBuilt++;
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
        message[0] = MESSAGE_TYPE_HQ_LOC;
        message[1] = myLoc;
        if (sendBlockchain(message, 1)) {
            reportedLocation = true;
        }
    }


}
