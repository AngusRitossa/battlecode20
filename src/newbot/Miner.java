package newbot;

import battlecode.common.*;
import java.util.*;

public class Miner extends RobotPlayer {
    public static void runMiner() throws GameActionException {
        doAction();
    }

    public static void doAction() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
    }
}