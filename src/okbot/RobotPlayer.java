package okbot;

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
    public static final int BLOCK_SIZE = 4;

    public static int startRoundNum;
    public static int roundNum;
    public static MapLocation hqLoc = null; // Location of HQ.
    public static ArrayList<MapLocation> possibleEnemyHQLocs = new ArrayList<MapLocation>();
    public static ArrayList<MapLocation> refineryLocations = new ArrayList<MapLocation>(); // Includes the HQ
    public static ArrayList<MapLocation> refineriesWithSoup = new ArrayList<MapLocation>(); // Refineries with nearby soup
    public static ArrayList<MapLocation> unreachableRefineries = new ArrayList<MapLocation>();
    public static boolean destroySelf = false; // used by disintegrate()
    // TODO: replace this with actual logic
    public static Direction tendency = null;

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
                if (destroySelf) {
                    System.out.println("disintegrating...");
                    return;
                }
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
    // Avoids drowning.
    // Returns whether move succeeded.
    public static Direction lastMove = null;
    public static boolean tryMove(Direction dir) throws GameActionException {
        if (!rc.isReady()) {
            drawError("tried to move twice, probably logic error in code");
        }
        if (rc.canMove(dir) && (rc.getType() == RobotType.DELIVERY_DRONE || !rc.senseFlooding(rc.adjacentLocation(dir)))) {
            lastMove = dir;
            rc.move(dir);
            return true;
        } else return false;
    }

    // Tries to move towards tendency, and rotates tendency a bit on failure
    // Avoids tryMoveToTendencyForbidden directions
    public static boolean[] tryMoveToTendencyForbidden = new boolean[8];
    public static boolean tryMoveToTendency() throws GameActionException {
        if (tendency == null) {
            drawWarning("tendency is null");
            tendency = randomDirection();
        }
        int i = 0;
        int dirInt = tendency.ordinal();
        for (i = 0; i < 8; i++) {
            if (!tryMoveToTendencyForbidden[dirInt] && tryMove(directions[dirInt])) {
                break;
            }
            if (i % 2 == 0) {
                dirInt = (dirInt + (i+1)) % 8;
            } else {
                dirInt = (dirInt + 8 - (i+1)) % 8;
            }
        }
        if (i < 8) {
            //rc.setIndicatorLine(rc.getLocation(), rc.adjacentLocation(tendency), 0, 0, 255);
            if (i >= 1) {
                if (i % 2 == 0) {
                    tendency = tendency.rotateLeft();
                } else {
                    tendency = tendency.rotateRight();
                }
            } else {
                double p = Math.random();
                if (p < 0.1) {
                    tendency = tendency.rotateLeft();
                } else if (p < 0.2) {
                    tendency = tendency.rotateRight();
                }
            }
            //rc.setIndicatorLine(rc.getLocation(), rc.adjacentLocation(tendency), 255, 255, 0);
            return true;
        } else {
            return false;
        }
    }

    // Try to move towards location.
    // Sets tendency to direction of movement.
    public static MapLocation destination = null;
    public static int bestSoFar = 0;
    public static int patience = 0;
    public static boolean tryMoveTowards(MapLocation loc) throws GameActionException {
        // TODO: change all movement and other functionality to the new function
        if (rc.getType() == RobotType.MINER || rc.getType() == RobotType.LANDSCAPER) {
            return tryMoveTowardsNew(loc);
        }
        if (!loc.equals(destination)) {
            // new destination
            if (destination == null || manhattanDistance(destination, loc) > 15) {
                patience = 10;
            } else {
                patience = 2;
            }
            destination = loc;
            bestSoFar = manhattanDistance(rc.getLocation(), destination);
        } else {
            int dist = manhattanDistance(rc.getLocation(), destination);
            if (dist < bestSoFar) {
                bestSoFar = dist;
                patience += 2;
            } else {
                patience--;
            }
        }
        System.out.println("patience " + patience);
        Direction dir = rc.getLocation().directionTo(loc);
        if (patience <= 0) {
            System.out.println("out of patience");
            if (rc.getType() == RobotType.LANDSCAPER) {
                if (tryMove(dir)) {
                    return true;
                }
                // TODO: try not to drown yourself
                int myElevation = rc.senseElevation(rc.getLocation());
                int toElevation = rc.senseElevation(rc.adjacentLocation(dir));
                if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
                    if (myElevation > toElevation) {
                        rc.digDirt(Direction.CENTER);
                    } else {
                        rc.digDirt(dir);
                    }
                } else {
                    if (myElevation > toElevation) {
                        rc.depositDirt(dir);
                    } else {
                        rc.depositDirt(Direction.CENTER);
                    }
                }
                return true;
            } else if (rc.getType() == RobotType.MINER) {
                // TODO: try not to refer to other classes from RobotPlayer
                Miner.minerGiveUp[destination.y / BLOCK_SIZE][destination.x / BLOCK_SIZE] = true;
                patience += 5;
            }
        }
        if (lastMove == null) {
            for (int i = 0; i < 8; i++) {
                if (tryMove(dir)) {
                    tendency = dir;
                    return true;
                }
                for (int j = 0; j <= i; j++) {
                    if (i % 2 == 0) dir = dir.rotateLeft();
                    else dir = dir.rotateRight();
                }
            }
        } else {
            // TODO: implement actual bug nav or just fix this to be less bad
            rc.setIndicatorLine(rc.getLocation(), rc.adjacentLocation(dir), 255, 255, 255);
            rc.setIndicatorLine(rc.getLocation(), rc.adjacentLocation(lastMove), 0, 0, 0);
            int last = indexOf(lastMove);
            int cur = indexOf(dir);
            boolean mirror = true;
            int diff = (last - cur + 8) % 8;
            if (diff > 4) {
                mirror = true;
                diff = 8 - diff;
            }
            Direction move = dir;
            if (diff == 4) {
                move = move.rotateRight();
            }
            for (int i = 0; i < 8; i++) {
                if (move == dir.opposite()) {
                    break;
                }
                if (tryMove(move)) {
                    rc.setIndicatorLine(rc.adjacentLocation(move.opposite()), rc.getLocation(), 0, 255, 0);
                    return true;
                }
                move = mirror ? move.rotateLeft() : move.rotateRight();
            }
            move = mirror ? dir.rotateRight() : dir.rotateLeft();
            for (int i = 0; i < 8; i++) {
                if (move == dir.opposite()) {
                    break;
                }
                if (tryMove(move)) {
                    rc.setIndicatorLine(rc.adjacentLocation(move.opposite()), rc.getLocation(), 0, 255, 0);
                    return true;
                }
                move = mirror ? move.rotateRight() : move.rotateLeft();
            }
        }
        return false;
    }
    // which direction to start your checks in the next bugnav move
    public static int startDir = -1;
    // whether to search clockwise (as opposed to anticlockwise) in bugnav
    public static boolean clockwise = false;
    // number of turns in which startDir has been missing in a row
    public static int startDirMissingInARow = 0;
    public static boolean tryMoveTowardsNew(MapLocation loc) throws GameActionException {
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
                if (onTheMap(next) && !dangerousDir[dir] && rc.canMove(directions[dir]) &&
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
        if (!onTheMap(rc.adjacentLocation(directions[startDir]))) {
            startDir = rc.getLocation().directionTo(destination).ordinal();
            drawWarning("starting dir should not point off the map");
        }
        int dir = startDir;
        for (int i = 0; i < 8; i++) {
            MapLocation next = rc.adjacentLocation(directions[dir]);
            // If you hit the edge of the map, reverse direction
            if (!onTheMap(next)) {
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
                if (!onTheMap(rc.adjacentLocation(directions[startDir]))) {
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

    // Assumes robot is ready.
    // Returns whether the build succeeded.
    public static boolean tryBuild(RobotType type, Direction dir, boolean urgent) throws GameActionException {
        if (!canAffordToBuild(type, urgent)) {
            return false;
        }
        if (rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    public static boolean tryBuildInDir(RobotType type, Direction dir, boolean urgent) throws GameActionException {
        if (!canAffordToBuild(type, urgent)) {
            return false;
        }
        int d = indexOf(dir);
        if (d == -1) {
            d = 0;
        }
        for (int i = 0; i < 8; i++) {
            if (tryBuild(type, directions[d], false)) {
                return true;
            }
            if (i % 2 == 0) {
                d = (d + (i + 1)) % 8;
            } else {
                d = (d - (i + 1) + 80) % 8;
            }
        }
        return false;
    }

    public static boolean onTheMap(MapLocation loc) {
        // TODO: check if this uses more/less bytecode than storing map width and height
        return 0 <= loc.x && loc.x < rc.getMapWidth() &&
                0 <= loc.y && loc.y < rc.getMapHeight();
    }

    // Sets hqLoc to location of HQ if HQ is in sensor range.
    public static void findHQ() throws GameActionException {
        if (hqLoc == null) {
            // Check adjacent squares
            for (Direction dir : directions) {
                MapLocation loc = rc.adjacentLocation(dir);
                if (onTheMap(loc)) {
                    RobotInfo robot = rc.senseRobotAtLocation(loc);
                    if (robot != null && robot.team == rc.getTeam() && robot.type == RobotType.HQ) {
                        hqLoc = loc;
                        return;
                    }
                }
            }

            // TODO: refactor senseNearbyRobots call
            RobotInfo[] robots = rc.senseNearbyRobots(99999, rc.getTeam());
            for (RobotInfo robot : robots) {
                if (robot.team == rc.getTeam() && robot.type == RobotType.HQ) {
                    hqLoc = robot.location;
                    return;
                }
            }

            System.out.println("WARNING: Don't know where HQ is.");
        }
    }

    // Self destruct
    public static void disintegrate() throws GameActionException {
        destroySelf = true;
        throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "disintegrating");
    }

    // TODO: replace this with .ordinal() for bytecode speedup?
    public static int indexOf(Direction dir) {
        for (int i = 0; i < 8; i++) {
            if (directions[i] == dir) {
                return i;
            }
        }
        return -1;
    }

    // TODO: actually encrypt and decrypt
    public static final int[] verySecureBtw = {1187569015,1897194543,1742877415,1115706596,1950859704,697346254,765275357};
    public static void encrypt(int[] message) {
        if (message[6] != 0) {
            drawError("don't use last spot in message");
        }
        int k = rc.getTeam() == Team.A ? 2 : 1;
        for (int i = 0; i < 6; i++) {
            k = (k + (message[i] % 1003) * (i + 2));
        }
        message[6] = (1003 - k % 1003) % 1003;
        for (int i = 0; i < 7; i++) {
            if (((message[i] ^ verySecureBtw[i]) ^ verySecureBtw[i]) != message[i]) {
                drawError("encryption broken");
            }
            message[i] ^= verySecureBtw[i];
        }
    }

    // returns whether the message is probably ours
    public static boolean decrypt(int[] message) {
        if (message.length != 7) {
            return false;
        }
        int k = 0;
        for (int i = 0; i < 7; i++) {
            message[i] ^= verySecureBtw[i];
            if (i < 6) {
                k = (k + (message[i] % 1003) * (i + 2));
            } else {
                k = (k + message[i] % 1003) % 1003;
            }
        }
        System.out.println("k = " + k);
        return k == (rc.getTeam() == Team.A ? 1001 : 1002);
    }

    public static int curBlock = 1;
    public static void readBlockchain(int bytecodeLimit) throws GameActionException {
        // TODO: limit reading so you don't run out of bytecode
        // or don't limit it because you have 10 turns anyway I guess
        // TODO: smarter limiting and smart deciding which blocks to read first
        while (curBlock < roundNum && Clock.getBytecodesLeft() > bytecodeLimit) {
            //System.out.println("blockchain bytecodes left " + Clock.getBytecodesLeft() + " reading round " + curBlock);
            Transaction[] transactions = rc.getBlock(curBlock);
            for (Transaction transaction : transactions) {
                int[] message = transaction.getMessage();
                if (decrypt(message)) {
                    interpretTransaction(message, transaction.getCost());
                }
            }
            curBlock++;
        }
    }

    public static final int MESSAGE_TYPE_HQ_LOC = 55;
    public static final int MESSAGE_TYPE_ENEMY_HQ_LOC = 66;
    public static final int MESSAGE_TYPE_I_WILL_BUILD_FULFILLMENT_CENTER = 79;
    public static final int MESSAGE_TYPE_REFINERY_LOC = 80;
    public static final int MESSAGE_TYPE_REFINERY_IS_DEAD = 81;
    // TODO: add a message to say that the enemy HQ is not at a location
    public static boolean seenEnemyHQMessage = false;
    public static int bestBuildFullfillmentCenterBid = 9999999;
    public static int bestBuildFullfillmentCenterBidID = -1;
    public static int bestBuildFullfillmentCenterBidRound = -1;
    public static void interpretTransaction(int[] message, int cost) {
        //System.out.println("cost: " + cost + ", " + Arrays.toString(message));
        if (message[0] == MESSAGE_TYPE_HQ_LOC) {
            int y = message[1] / MAX_MAP_SIZE;
            int x = message[1] % MAX_MAP_SIZE;
            System.out.println("hq loc is " + x + " " + y);
            if (hqLoc == null || possibleEnemyHQLocs.size() == 0) {
                hqLoc = new MapLocation(x, y);
                if (rc.getMapWidth() - 1 - x != x) {
                    possibleEnemyHQLocs.add(new MapLocation(rc.getMapWidth() - 1 - x, y));
                }
                if (rc.getMapHeight() - 1 - y != y) {
                    possibleEnemyHQLocs.add(new MapLocation(x, rc.getMapHeight() - 1 - y));
                }
                possibleEnemyHQLocs.add(new MapLocation(rc.getMapWidth() - 1 - x, rc.getMapHeight() - 1 - y));
//                for (MapLocation loc : possibleEnemyHQLocs) {
//                    rc.setIndicatorLine(rc.getLocation(), loc, 255, 255, 255);
//                }
                Collections.shuffle(possibleEnemyHQLocs);
            }
        } else if (message[0] == MESSAGE_TYPE_ENEMY_HQ_LOC) {
            int y = message[1] / MAX_MAP_SIZE;
            int x = message[1] % MAX_MAP_SIZE;
            possibleEnemyHQLocs.clear();
            possibleEnemyHQLocs.add(new MapLocation(x, y));
            seenEnemyHQMessage = true;
        } else if (message[0] == MESSAGE_TYPE_I_WILL_BUILD_FULFILLMENT_CENTER) {
            int dist = message[1];
            int id = message[2];
            if (dist < bestBuildFullfillmentCenterBid) {
                bestBuildFullfillmentCenterBid = dist;
                bestBuildFullfillmentCenterBidID = id;
                bestBuildFullfillmentCenterBidRound = roundNum;
            }
        } else if (message[0] == MESSAGE_TYPE_REFINERY_LOC) {
            int y = message[1] / MAX_MAP_SIZE;
            int x = message[1] % MAX_MAP_SIZE;
            refineryLocations.add(new MapLocation(x, y));
            refineriesWithSoup.add(new MapLocation(x, y));
        } else if (message[0] == MESSAGE_TYPE_REFINERY_IS_DEAD) {
            int y = message[1] / MAX_MAP_SIZE;
            int x = message[1] % MAX_MAP_SIZE;
            refineryLocations.remove(new MapLocation(x, y)); 
            refineriesWithSoup.remove(new MapLocation(x, y));
        }
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
            return Math.max(0, Math.min(250, rc.getRoundNum() - 20));
        }
    }

    public static boolean canAffordToBuild(RobotType unit, boolean urgent) {
        if (urgent) {
            return unit.cost <= rc.getTeamSoup();
        } else {
            return unit.cost <= rc.getTeamSoup() - soupReserve();
        }
    }

    // TODO: refactor senseNearbyRobots
    public static boolean tryRunAwayFromDrones() throws GameActionException {
        // TODO: don't get trapped in corner
        RobotInfo[] robots = rc.senseNearbyRobots(8, rc.getTeam().opponent());
        int[] closest = new int[8];
        int curDist = 9999;
        // TODO: check bytecode usage of this...
        Arrays.fill(closest, 9999);
        for (RobotInfo robot : robots) {
            if (robot.type == RobotType.DELIVERY_DRONE) {
                for (int i = 0; i < 8; i++) {
                    int dist = rc.adjacentLocation(directions[i]).distanceSquaredTo(robot.location);
                    if (dist < closest[i]) closest[i] = dist;
                }
                curDist = Math.min(curDist, rc.getLocation().distanceSquaredTo(robot.location));
            }
        }
        if (closest[0] == 9999) {
            System.out.println("no drones");
            return false;
        } else {
            System.out.println("drones!");
            int bestDir = -1;
            int bestDist = curDist;
            for (int i = 0; i < 8; i++) {
                // TODO: sense if square will be flooded next turn
                if (closest[i] > bestDist && rc.canMove(directions[i]) &&
                        !rc.senseFlooding(rc.adjacentLocation(directions[i]))) {
                    bestDir = i;
                    bestDist = closest[i];
                }
                //System.out.println("closest " + i + " " + closest[i] + " " + rc.canMove(directions[i]) + " " +
                //        (onTheMap(rc.adjacentLocation(directions[i])) && !rc.senseFlooding(rc.adjacentLocation(directions[i]))));
            }
            if (bestDir != -1) {
                rc.move(directions[bestDir]);
                tendency = directions[bestDir];
                System.out.println("running away to " + directions[bestDir]);
                return true;
            } else {
                return false;
            }
        }
    }
}
