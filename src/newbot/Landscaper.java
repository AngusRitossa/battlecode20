package newbot;

import battlecode.common.*;

public class Landscaper extends RobotPlayer {
    public static void runLandscaper() throws GameActionException {
        doAction();

    }

    public static void doAction() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
    }
}
