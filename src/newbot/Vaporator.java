package newbot;

import battlecode.common.GameActionException;

public class Vaporator extends RobotPlayer {
    public static void runVaporator() throws GameActionException {
    	doAction();
    }
    
    public static void doAction() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
    }
}
