package newbot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

public class HQ extends RobotPlayer {
    public static void runHQ() throws GameActionException {
        doAction();
    }

    public static void doAction() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
        if (tryShootUnit()) {
            return;
        }
    }
}
