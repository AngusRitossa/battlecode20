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
    public static ArrayList<MapLocation> knownRefineries = new ArrayList<MapLocation>(); // includes HQ
    public static ArrayList<MapLocation> knownRefineriesWithSoup = new ArrayList<MapLocation>(); // that that have soup near them 
    public static ArrayList<MapLocation> unreachableRefineries = new ArrayList<MapLocation>(); 
    public static ArrayList<MapLocation> knownDesignSchools = new ArrayList<MapLocation>();

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

    public static final int SOUP_RESERVE_START = 100;
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

    public static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.canMove(dir) && (rc.getType() == RobotType.DELIVERY_DRONE || !rc.senseFlooding(rc.adjacentLocation(dir)))) {
            rc.move(dir);
            return true;
        } else {
            return false;
        }
    }
    // This is just move function taken from okbot
    // TODO: Make a new nav function
    public static MapLocation destination = null;
    public static int bestSoFar = 0;
    // which direction to start your checks in the next bugnav move
    public static int startDir = -1;
    // whether to search clockwise (as opposed to anticlockwise) in bugnav
    public static boolean clockwise = false;
    // number of turns in which startDir has been missing in a row
    public static int startDirMissingInARow = 0;
    public static boolean tryMoveTowards(MapLocation loc) throws GameActionException {

        // New movement function
        // Implements something based on bugnav but not quite
        // Has a few heuristics that hopefully will make it faster as opposed to degenerate
        // Has some safeguards to deal with the fact that there are other robots in the way, not just stationary obstacles

        // TODO: figure out how best to deal with destination changing
        // TODO: senseFlooding to avoid drowning
        // TODO: precalc which adjacent squares are about to be flooded in the next turn for even better non-drowning

        if (rc.getType() == RobotType.DELIVERY_DRONE) {
            drawError("don't use this function for drones");
        }
        // Stores whether there is an enemy drone adjacent to this dir
        // TODO: maybe can use this for flooding too?
        boolean[] dangerousDir = new boolean[8];
        RobotInfo[] enemies = rc.senseNearbyRobots(18, rc.getTeam().opponent());
        for (RobotInfo enemy : enemies) {
            if (enemy.type == RobotType.DELIVERY_DRONE) {
                for (int i = 0; i < 8; i++) {
                    if (rc.adjacentLocation(directions[i]).distanceSquaredTo(enemy.location) <= 8) {
                        dangerousDir[i] = true;
                    }
                }
            }
        }

        if (destination == null || !loc.equals(destination)) {
            destination = loc;
            bestSoFar = 99999;
            startDir = -1;
            clockwise = Math.random() < 0.5;
            startDirMissingInARow = 0;
        }

        if (rc.getLocation().equals(loc)) {
            drawWarning("already at destination");
            return false;
        }

        int dist = hybridDistance(rc.getLocation(), destination);
        if (dist < bestSoFar) {
            bestSoFar = dist;
            startDir = rc.getLocation().directionTo(destination).ordinal();

            // Refresh choice of anticlockwise vs clockwise based on which one moves closer to the destination
            // If they are equally close, prefer the current direction
            int firstDist = -1; // Distance if you move in the current clockwise/anticlockwise direction
            int lastDist = -1; // Distance if you move in the opposite direction
            int dir = startDir;
            for (int i = 0; i < 8; i++) {
                MapLocation next = rc.adjacentLocation(directions[dir]);
                // TODO: sense squares which are not flooded but will be next turn
                if (rc.onTheMap(next) && !dangerousDir[dir] && rc.canMove(directions[dir]) &&
                        !rc.senseFlooding(rc.adjacentLocation(directions[dir]))) {
                    int nextDist = hybridDistance(next, destination);
                    if (firstDist == -1) {
                        firstDist = nextDist;
                    }
                    lastDist = nextDist;
                }
                if (clockwise) dir = (dir + 1) % 8;
                else dir = (dir + 7) % 8;
            }
            System.out.println("clockwise = " + clockwise + ", firstDist = " + firstDist + ", lastDist = " + lastDist);
            if (lastDist < firstDist) {
                // Switch directions
                clockwise = !clockwise;
            }
        }

        System.out.println("startDir = " + startDir + ", clockwise = " + clockwise);
        if (!rc.onTheMap(rc.adjacentLocation(directions[startDir]))) {
            startDir = rc.getLocation().directionTo(destination).ordinal();
            drawWarning("starting dir should not point off the map");
        }
        int dir = startDir;
        for (int i = 0; i < 8; i++) {
            MapLocation next = rc.adjacentLocation(directions[dir]);
            // If you hit the edge of the map, reverse direction
            if (!rc.onTheMap(next)) {
                clockwise = !clockwise;
                dir = startDir;
            } else if (!dangerousDir[dir] && tryMove(directions[dir])) {
                // Safeguard 1: dir might equal startDir if this robot was blocked by another robot last turn
                // that has since moved.
                if (dir != startDir) {
                    if (clockwise) startDir = dir % 2 == 1 ? (dir + 5) % 8 : (dir + 6) % 8;
                    else startDir = dir % 2 == 1 ? (dir + 3) % 8 : (dir + 2) % 8;

                    startDirMissingInARow = 0;
                } else {
                    // Safeguard 2: If the obstacle that should be at startDir is missing 2/3 turns in a row
                    // reset startDir to point towards destination
                    if (++startDirMissingInARow == 3) {
                        startDir = rc.getLocation().directionTo(destination).ordinal();
                        startDirMissingInARow = 0;
                    }
                }
                // Rare occasion when startDir gets set to Direction.CENTER
                if (startDir == 8) {
                    startDir = 0;
                }
                // Safeguard 3: If startDir points off the map, reset startDir towards destination
                if (!rc.onTheMap(rc.adjacentLocation(directions[startDir]))) {
                    startDir = rc.getLocation().directionTo(destination).ordinal();
                }
                rc.setIndicatorLine(rc.getLocation(), loc, 255, 255, 255);
                rc.setIndicatorDot(rc.adjacentLocation(directions[startDir]), 127, 127, 255);
                return true;
            }

            if (clockwise) dir = (dir + 1) % 8;
            else dir = (dir + 7) % 8;
        }

        return false;
    }
    // hybrid between manhattan distance (dx + dy) and max distance max(dx, dy)
    public static int hybridDistance(MapLocation a, MapLocation b) {
        int dy = Math.abs(a.y - b.y);
        int dx = Math.abs(a.x - b.x);
        return dy + dx + Math.max(dy, dx);
    }
    public static int manhattanDistance(MapLocation a, MapLocation b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }


    public static void findHQ() throws GameActionException {
        // HQ loc should be known via blockchain, if not check it using senseNearbyRobots
        if (hqLoc == null) {
            RobotInfo[] robots = rc.senseNearbyRobots(9999, rc.getTeam());
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ) {
                    hqLoc = robot.location;
                    return;
                }
            }
        }   
    }


    // Blockchain 

    public static final int MESSAGE_TYPE_HQ_LOC = 1;
    public static final int MESSAGE_TYPE_REFINERY_LOC = 2;
    public static final int MESSAGE_TYPE_REFINERY_IS_DEAD = 3;
    public static final int MESSAGE_TYPE_DESIGN_SCHOOL_LOC = 4;
    public static final int MESSAGE_TYPE_DESIGN_SCHOOL_IS_DEAD = 5;

    public static final int[] xorValues = { 483608780, 1381610763, 33213801, 157067759, 1704169077, 1285648416, 1172763091 };
    public static boolean sendBlockchain(int[] message, int cost) throws GameActionException {
        if (message[6] != 0) {
            drawError("don't use last spot in message");
        }
        int k = 0;
        for (int i = 0; i < 6; i++ ) {
            k = (k + message[i]) % 42069;
        }
        if (rc.getTeam() == Team.A) {
            k += 666;
        }
        k %= 42069;
        message[6] = k;
        for (int i = 0; i < 7; i++) {
            message[i] ^= xorValues[i];
        }
        // xor the last value by the cost for *maximum* security 
        message[6] ^= cost;

        if (rc.canSubmitTransaction(message, cost)) {
            rc.submitTransaction(message, cost);
            return true;
        }
        return false;
    }

    public static boolean decodeBlockchain(int[] message, int cost) throws GameActionException {
        // Returns false if its not our message, true if it (probably) is
        for (int i = 0; i < 7; i++) {
            message[i] ^= xorValues[i];
        }
        message[6] ^= cost;

        int k = 0;
        for (int i = 0; i < 6; i++ ) {
            k = (k + message[i]) % 42069;
        }
        if (rc.getTeam() == Team.A) {
            k += 666;
        }
        k %= 42069;

        return k == message[6];
    }

    public static int currBlock = 1;
    public static void readBlockchain(int bytecodeLimit) throws GameActionException {
        while (currBlock < roundNum && Clock.getBytecodesLeft() > bytecodeLimit) {
            Transaction[] transactions = rc.getBlock(currBlock);
            for (Transaction transaction : transactions) {
                int[] message = transaction.getMessage();
                int cost = transaction.getCost();
                if (decodeBlockchain(message, cost)) {
                    interpretBlockchain(message, cost);
                }
            }
            currBlock++;
        }
    }

    public static void interpretBlockchain(int[] message, int cost) throws GameActionException {
        if (message[0] == MESSAGE_TYPE_HQ_LOC) {
            int x = message[1] / MAX_MAP_SIZE;
            int y = message[1] % MAX_MAP_SIZE;
            hqLoc = new MapLocation(x, y);
            knownRefineries.add(hqLoc);
            knownRefineriesWithSoup.add(hqLoc);
            System.out.println("Our HQ location: " + x + " " + y);
        } else if (message[0] == MESSAGE_TYPE_REFINERY_LOC) {
            if (rc.getType() == RobotType.MINER) {
                int x = message[1] / MAX_MAP_SIZE;
                int y = message[1] % MAX_MAP_SIZE;
                MapLocation loc = new MapLocation(x, y);
                knownRefineries.add(loc);
                knownRefineriesWithSoup.add(loc);
            }
        } else if (message[0] == MESSAGE_TYPE_DESIGN_SCHOOL_LOC) {
            int x = message[1] / MAX_MAP_SIZE;
            int y = message[1] % MAX_MAP_SIZE;
            MapLocation loc = new MapLocation(x, y);
            knownDesignSchools.add(loc);
        } else if (message[0] == MESSAGE_TYPE_DESIGN_SCHOOL_IS_DEAD) {
            int x = message[1] / MAX_MAP_SIZE;
            int y = message[1] % MAX_MAP_SIZE;
            MapLocation loc = new MapLocation(x, y);
            knownDesignSchools.remove(loc);
        }
    }


    // Random movement
    public static int turnStartedLastMovement = -9999;
    public static MapLocation randomSquareMovingTowards = null;
    public static boolean tryMoveRandomly() throws GameActionException {
        // Picks a random square on the board and moves towards it
        if (randomSquareMovingTowards == null || rc.getRoundNum() - turnStartedLastMovement >= 50 || rc.getLocation().distanceSquaredTo(randomSquareMovingTowards) < 20) {
            int x = (int) (Math.random() * rc.getMapWidth());
            int y = (int) (Math.random() * rc.getMapWidth());
            randomSquareMovingTowards = new MapLocation(x, y);
            turnStartedLastMovement = rc.getRoundNum();
        }
        System.out.println("Moving randomly towards: " + randomSquareMovingTowards.x + " " + randomSquareMovingTowards.y);
        return tryMoveTowards(randomSquareMovingTowards);
    }

    // This function is here, rather than in landscaper, because miner needs to access it too
    // TODO: Maybe clean up file stuff so that lower turtle stuff isn't split between multiple files
    public static boolean canBeDugForLowerTurtle(MapLocation loc) throws GameActionException {
        // Returns whether this square in the larger turtle can be dug from
        // Should be the case that every square has such a square adj to it
        if (hqLoc == null || loc == hqLoc) {
            return false;
        }
        return (loc.x - hqLoc.x)%3 == 0 && (loc.y - hqLoc.y)%3 == 0;
    }
}
