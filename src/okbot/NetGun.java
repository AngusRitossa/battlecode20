package okbot;

import battlecode.common.GameActionException;

public class NetGun extends RobotPlayer {
    public static void runNetGun() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
        if (tryShootUnit()) {
            return;
        }
    }
}
