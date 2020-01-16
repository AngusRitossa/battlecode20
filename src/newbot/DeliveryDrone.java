package newbot;

import battlecode.common.*;

public class DeliveryDrone extends RobotPlayer {

    public static void runDeliveryDrone() throws GameActionException {
        doAction();
    }

    public static void doAction() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
    }
}
