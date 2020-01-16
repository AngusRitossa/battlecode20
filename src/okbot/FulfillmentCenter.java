package okbot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

public class FulfillmentCenter extends RobotPlayer {
    public static void runFulfillmentCenter() throws GameActionException {
        doAction();

        readBlockchain(1500);
    }
    public static void doAction() throws GameActionException {
        // TODO: something smarter
        if (roundNum % 3 <= 1 && rc.getTeamSoup() <= 300) {
            return;
        }
        if (hqLoc != null) {
            Direction dir = rc.getLocation().directionTo(hqLoc);
            if (dir != Direction.CENTER && tryBuildInDir(RobotType.DELIVERY_DRONE, dir, false)) {
                return;
            }
        }
        MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        Direction cen = rc.getLocation().directionTo(center);
        if (cen != Direction.CENTER && tryBuildInDir(RobotType.DELIVERY_DRONE, cen, false)) {
            return;
        }
        for (Direction dir : directions) {
            if (tryBuild(RobotType.DELIVERY_DRONE, dir, false)) {
                return;
            }
        }
    }
}
