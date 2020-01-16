package newbot;

import battlecode.common.GameActionException;

public class NetGun extends RobotPlayer {
    public static void runNetGun() throws GameActionException {
       	doAction();
    }

    public static void doAction() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
    }
}
