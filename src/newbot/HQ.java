package newbot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

public class HQ extends RobotPlayer {
    public static void runHQ() throws GameActionException {
        readBlockchain(9000);
        doAction();
        readBlockchain(2000);
    }

    public static void doAction() throws GameActionException {
        reportLocation();
        tryInitiateEarlySwarm();
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
        if (minersBuilt >= 10 || minersBuilt >= rc.getRoundNum()/30 + 3) {
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

    public static boolean initiatedEarlySwarm = false;
    public static final int dronesRequiredForEarlySwarm = 90;
    public static void tryInitiateEarlySwarm() throws GameActionException {
        if (rc.getRoundNum() < swarmRound-250 && totalNumberDronesBuilt >= dronesRequiredForEarlySwarm && !initiatedEarlySwarm) {
            System.out.println("Try to initiate early swarm");
            // initiates a swarm for 100 turns time
            int message[] = new int[7];
            message[0] = MESSAGE_TYPE_DO_EARLY_SWARM;
            message[1] = rc.getRoundNum()+100;
            if (sendBlockchain(message, 1)) {
                initiatedEarlySwarm = true;
                System.out.println("Initiated early swarm");
            }
        }
    }


}
