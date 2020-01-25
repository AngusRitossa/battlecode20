package okbot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

public class HQ extends RobotPlayer {
    public static int minersBuilt = 0;
    public static int hqBuildDirsIdx = 0;
    public static int roundLastMinerBuilt = -999;
    public static final int[] hqBuildDirs = {0, 4, 2, 6, 1, 5, 3, 7};
    public static int turnGapBetweenMinerBuilds = 200;
    public static void runHQ() throws GameActionException {
        doHQAction();

        if (roundNum == 1) {
            int[] message = new int[7];
            message[0] = MESSAGE_TYPE_HQ_LOC;
            message[1] = rc.getLocation().y * MAX_MAP_SIZE + rc.getLocation().x;
            encrypt(message);
            if (rc.canSubmitTransaction(message, 1)) {
                rc.submitTransaction(message, 1);
            }
        }
    }

    public static void doHQAction() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
        if (tryShootUnit()) {
            return;
        }

        // TODO: better build choice logic
        if (roundNum <= 225) turnGapBetweenMinerBuilds = 45;
        else turnGapBetweenMinerBuilds = 200;
        if (/*rc.getRobotCount() >= 4 && */rc.getRoundNum()-roundLastMinerBuilt <= turnGapBetweenMinerBuilds) {
            return;
        }

        // try to build towards soup
        int sensorRadius = rc.getCurrentSensorRadiusSquared();
        for (int i = 0; i < offsetDist.length; i++) {
            if (offsetDist[i] > sensorRadius) {
                break;
            }
            MapLocation loc = rc.getLocation().translate(offsetX[i], offsetY[i]);
            if (onTheMap(loc) && rc.senseSoup(loc) > 0) {
                if (tryBuildInDir(RobotType.MINER, rc.getLocation().directionTo(loc), false)) {
                    minersBuilt++;
                    roundLastMinerBuilt = rc.getRoundNum();
                    return;
                }
                break;
            }
        }

        // try to build towards center first
        MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        Direction dirToCenter = rc.getLocation().directionTo(center);
        if (dirToCenter == Direction.CENTER) {
            dirToCenter = Direction.NORTH;
        }
        if (tryBuildInDir(RobotType.MINER, directions[(indexOf(dirToCenter) + hqBuildDirs[hqBuildDirsIdx]) % 8], false)) {
            minersBuilt++;
            roundLastMinerBuilt = rc.getRoundNum();
            hqBuildDirsIdx = (hqBuildDirsIdx + 1) % 8;
            return;
        }

        for (Direction dir : directions) {
            if (tryBuild(RobotType.MINER, dir, false)) {
                minersBuilt++;
                roundLastMinerBuilt = rc.getRoundNum();
                return;
            }
        }
    }
}
