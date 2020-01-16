package newbot;

import battlecode.common.GameActionException;

public class Refinery extends RobotPlayer {
    public static void runRefinery() throws GameActionException {
    	doAction();
    }
    
    public static void doAction() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
    }
}
