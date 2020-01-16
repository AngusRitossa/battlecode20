package okbot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

public class DesignSchool extends RobotPlayer {
    public static void runDesignSchool() throws GameActionException {
        doAction();

        readBlockchain(1500);
    }

    public static void doAction() throws GameActionException {
        // TODO: something smarter
        if (roundNum % 3 == 2 && rc.getTeamSoup() <= 300) {
            return;
        }
        if (hqLoc != null) {
            Direction dir = rc.getLocation().directionTo(hqLoc);
            System.out.println("direction to HQ " + dir);
            if (dir != Direction.CENTER && tryBuildInDir(RobotType.LANDSCAPER, dir, false)) {
                return;
            }
        }
        MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        Direction cen = rc.getLocation().directionTo(center);
        if (cen != Direction.CENTER && tryBuildInDir(RobotType.LANDSCAPER, cen, false)) {
            return;
        }
        for (Direction dir : directions) {
            if (tryBuild(RobotType.LANDSCAPER, dir, false)) {
                return;
            }
        }
    }
}
