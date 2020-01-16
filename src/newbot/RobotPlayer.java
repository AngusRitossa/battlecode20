package newbot;

import battlecode.common.*;
import java.util.*;

public strictfp class RobotPlayer {
    public static RobotController rc;

    // All directions except Direction.CENTER
    public static Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    // All surrounding squares in order of distance from origin.
    public static final int[] offsetDist = { 0, 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 8, 8, 8, 8, 9, 9, 9, 9,10,10,10,10,10,10,10,10,13,13,13,13,13,13,13,13,16,16,16,16,17,17,17,17,17,17,17,17,18,18,18,18,20,20,20,20,20,20,20,20,25,25,25,25,25,25,25,25,25,25,25,25,26,26,26,26,26,26,26,26,29,29,29,29,29,29,29,29,32,32,32,32,34,34,34,34,34,34,34,34,36,36,36,36,37,37,37,37,37,37,37,37,40,40,40,40,40,40,40,40,41,41,41,41,41,41,41,41,45,45,45,45,45,45,45,45,};
    public static final int[] offsetY    = { 0,-1, 0, 0, 1,-1,-1, 1, 1,-2, 0, 0, 2,-2,-2,-1,-1, 1, 1, 2, 2,-2,-2, 2, 2,-3, 0, 0, 3,-3,-3,-1,-1, 1, 1, 3, 3,-3,-3,-2,-2, 2, 2, 3, 3,-4, 0, 0, 4,-4,-4,-1,-1, 1, 1, 4, 4,-3,-3, 3, 3,-4,-4,-2,-2, 2, 2, 4, 4,-5,-4,-4,-3,-3, 0, 0, 3, 3, 4, 4, 5,-5,-5,-1,-1, 1, 1, 5, 5,-5,-5,-2,-2, 2, 2, 5, 5,-4,-4, 4, 4,-5,-5,-3,-3, 3, 3, 5, 5,-6, 0, 0, 6,-6,-6,-1,-1, 1, 1, 6, 6,-6,-6,-2,-2, 2, 2, 6, 6,-5,-5,-4,-4, 4, 4, 5, 5,-6,-6,-3,-3, 3, 3, 6, 6,};
    public static final int[] offsetX    = { 0, 0,-1, 1, 0,-1, 1,-1, 1, 0,-2, 2, 0,-1, 1,-2, 2,-2, 2,-1, 1,-2, 2,-2, 2, 0,-3, 3, 0,-1, 1,-3, 3,-3, 3,-1, 1,-2, 2,-3, 3,-3, 3,-2, 2, 0,-4, 4, 0,-1, 1,-4, 4,-4, 4,-1, 1,-3, 3,-3, 3,-2, 2,-4, 4,-4, 4,-2, 2, 0,-3, 3,-4, 4,-5, 5,-4, 4,-3, 3, 0,-1, 1,-5, 5,-5, 5,-1, 1,-2, 2,-5, 5,-5, 5,-2, 2,-4, 4,-4, 4,-3, 3,-5, 5,-5, 5,-3, 3, 0,-6, 6, 0,-1, 1,-6, 6,-6, 6,-1, 1,-2, 2,-6, 6,-6, 6,-2, 2,-4, 4,-5, 5,-5, 5,-4, 4,-3, 3,-6, 6,-6, 6,-3, 3,};

    // The round at which the water reaches a certain level for elevations [0, 99]
    // The water level rises monotonically at these levels.
    public static final int[] water_level_round = {1, 256, 464, 677, 931, 1210, 1413, 1546, 1640, 1713, 1771, 1819, 1861, 1897, 1929, 1957, 1983, 2007, 2028, 2048, 2067, 2084, 2100, 2115, 2129, 2143, 2155, 2168, 2179, 2190, 2201, 2211, 2220, 2230, 2239, 2247, 2256, 2264, 2271, 2279, 2286, 2293, 2300, 2307, 2313, 2319, 2325, 2331, 2337, 2343, 2348, 2353, 2359, 2364, 2369, 2374, 2379, 2383, 2388, 2392, 2397, 2401, 2405, 2409, 2413, 2417, 2421, 2425, 2429, 2433, 2436, 2440, 2443, 2447, 2450, 2454, 2457, 2460, 2464, 2467, 2470, 2473, 2476, 2479, 2482, 2485, 2488, 2491, 2493, 2496, 2499, 2502, 2504, 2507, 2509, 2512, 2514, 2517, 2519, 2522};

    public static final int MAX_MAP_SIZE = GameConstants.MAP_MAX_HEIGHT;

    public static int startRoundNum;
    public static int roundNum;
    public static MapLocation hqLoc = null; // Location of HQ.

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        startRoundNum = rc.getRoundNum();
        System.out.println("I'm a " + rc.getType() + " and I just got created!");

        while (true) {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                roundNum = rc.getRoundNum();
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());

                switch (rc.getType()) {
                    case HQ:
                        HQ.runHQ();
                        break;
                    case REFINERY:
                        Refinery.runRefinery();
                        break;
                    case VAPORATOR:
                        Vaporator.runVaporator();
                        break;
                    case DESIGN_SCHOOL:
                        DesignSchool.runDesignSchool();
                        break;
                    case FULFILLMENT_CENTER:
                        FulfillmentCenter.runFulfillmentCenter();
                        break;
                    case NET_GUN:
                        NetGun.runNetGun();
                        break;
                    case MINER:
                        Miner.runMiner();
                        break;
                    case LANDSCAPER:
                        Landscaper.runLandscaper();
                        break;
                    case DELIVERY_DRONE:
                        DeliveryDrone.runDeliveryDrone();
                        break;
                }

                if (roundNum != rc.getRoundNum()) {
                    drawError("used too many bytecodes, " + Clock.getBytecodesLeft() + " bytecodes left in round " + rc.getRoundNum());
                } else {
                    System.out.println(Clock.getBytecodesLeft() + " bytecodes left");
                }

            } catch (Exception e) {
                drawError(rc.getType() + " Exception");
                e.printStackTrace();
            }

            // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
            Clock.yield();
        }
    }

    // draw a big red x over the unit
    public static void drawError(String error) {
        System.out.println("ERROR: " + error);
        rc.setIndicatorLine(rc.getLocation().translate(-3, -3), rc.getLocation().translate(3, 3), 255, 0, 0);
        rc.setIndicatorLine(rc.getLocation().translate(-3, 3), rc.getLocation().translate(3, -3), 255, 0, 0);
    }

    // draws a red x over the unit
    public static void drawWarning(String warning) {
        System.out.println("WARNING: " + warning);
        rc.setIndicatorLine(rc.getLocation().translate(-1, -1), rc.getLocation().translate(1, 1), 255, 0, 0);
        rc.setIndicatorLine(rc.getLocation().translate(-1, 1), rc.getLocation().translate(1, -1), 255, 0, 0);
    }

    public static boolean is_bounded(int a, int low, int high) {
        return low <= a && a <= high;
    }

    // Used by HQ and Netguns.
    // Returns whether a unit was shot.
    public static boolean tryShootUnit() throws GameActionException {
        // TODO: prioritise which enemy drone?
        RobotInfo[] robots = rc.senseNearbyRobots(999999, rc.getTeam().opponent());
        for (RobotInfo robot : robots) {
            if (rc.canShootUnit(robot.ID)) {
                rc.shootUnit(robot.ID);
                rc.setIndicatorLine(rc.getLocation(), robot.location, 255, 0, 0);
                return true;
            }
        }
        return false;
    }

    // Returns a random non-Center direction.
    public static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

   

    // Assumes robot is ready.
    // Returns whether the build succeeded.
    public static boolean tryBuildInDir(RobotType type, Direction dir, boolean urgent) throws GameActionException {
        if (!canAffordToBuild(type, urgent)) {
            return false;
        }
        if (rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    public static int manhattanDistance(MapLocation a, MapLocation b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    public static final int SOUP_RESERVE_START = 225;
    public static int soupReserve() {
        // Current formula is kinda arbitrary
        if (roundNum < SOUP_RESERVE_START) {
            // Don't reserve in early game if enemy is attacking
            return 0;
        } else {
            return Math.min(250, rc.getRoundNum());
        }
    }

    public static boolean canAffordToBuild(RobotType unit, boolean urgent) {
        if (urgent) {
            return unit.cost <= rc.getTeamSoup();
        } else {
            return unit.cost <= rc.getTeamSoup() - soupReserve();
        }
    }

    public static boolean tryMoveTowards(MapLocation loc) throws GameActionException {
        // TODO: Implement bug nav or something
        // It is actually a lot more non-trivial than I originally thought when I set out to do it
    } 
}
