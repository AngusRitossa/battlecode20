package okbot;

import battlecode.common.*;
import java.util.*;

public class Miner extends RobotPlayer {
    public static boolean[][] minerGiveUp = new boolean[MAX_MAP_SIZE / BLOCK_SIZE + 1][MAX_MAP_SIZE / BLOCK_SIZE + 1];

    public static void runMiner() throws GameActionException {
        // 1. Mine
        // 2. Build buildings
        // TODO: implement miner
        // TODO: decrease bytecode usage

        readBlockchain(9000);
        updateRefineryLocations();

        // Find HQ and set initial tendency based on HQ direction
        if (hqLoc == null) {
            for (Direction dir : directions) {
                RobotInfo robot = rc.senseRobotAtLocation(rc.adjacentLocation(dir));
                if (robot != null && robot.team == rc.getTeam() && robot.type == RobotType.HQ) {
                    hqLoc = robot.location;
                    refineryLocations.add(hqLoc);
                    refineriesWithSoup.add(hqLoc);
                    tendency = dir.opposite();
                    break;
                }
            }
            if (hqLoc == null) {
                findHQ();
                if (hqLoc == null) {
                    drawWarning("Can't find HQ.");
                }
            }
        }

        // don't block turtle
        if (roundNum > 1000 && hqLoc != null && rc.getLocation().distanceSquaredTo(hqLoc) <= 8) {
            disintegrate();
        }

        doMinerAction();

        //System.out.println("bytescodes before last blockchain read " + Clock.getBytecodesLeft());
        // Read blockchain
        readBlockchain(1500);
    }

    public static void doMinerAction() throws GameActionException {
        // Do action
        if (!rc.isReady()) {
            return;
        }

        if (tryMinerBuild()) {
            return;
        }

        // TODO: take into account friendly HQ and net guns
        if (tryRunAwayFromDrones()) {
            return;
        }

        if (tryMineSoup()) {
            return;
        }

        tryMoveToTendency();
    }

    public static boolean biddedForFulfillmentCenter = false;
    public static boolean bidOver = false;
    public static boolean wonBid = false;

    public static boolean tryMinerBuild() throws GameActionException {
        // If money is too high, rebuild
        if (/*rc.getRobotCount() == 4 || */rc.getTeamSoup() > 450 || rc.getTeamSoup() > 300 && roundNum < 225) {
            // TODO: better build direction choice
            // Build towards HQ
            Direction bestDir = null;
            int bestValue = -1;
            for (Direction dir : directions) {
                if ((hqLoc == null || !rc.adjacentLocation(dir).isAdjacentTo(hqLoc)) &&
                        rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir)) {
                    int dist = 15;
                    int value = 15;
                    if (hqLoc != null) {
                        dist = hqLoc.distanceSquaredTo(rc.adjacentLocation(dir));
                        if (dist <= 8) {
                            value = 1;
                        } else {
                            value = 999 - dist;
                        }
                    }
                    if (value > bestValue) {
                        bestValue = value;
                        bestDir = dir;
                    }
                }
            }
            if (bestDir != null) {
                if (tryBuild(RobotType.DESIGN_SCHOOL, bestDir, true)) {
                    return true;
                }
            }
        }

        // TODO: handle bid winner dying/failing to complete build
        // TODO: handle not having enough money to communicate
        if (!bidOver) {
            if (bestBuildFullfillmentCenterBidRound != -1 &&
                    bestBuildFullfillmentCenterBidRound <= roundNum - 2) {
                bidOver = true;
                if (bestBuildFullfillmentCenterBidID == rc.getID()) {
                    wonBid = true;
                }
            }
        }
        if (!bidOver && (/*rc.getRobotCount() >= 12 || */roundNum >= 200) && !biddedForFulfillmentCenter) {
            biddedForFulfillmentCenter = true;
            // bid to build fulfillment center
            // TODO: maybe not all bid?...
            int[] message = new int[7];
            message[0] = MESSAGE_TYPE_I_WILL_BUILD_FULFILLMENT_CENTER;
            message[1] = hqLoc == null ? 9999 : hybridDistance(rc.getLocation(), hqLoc);
            message[2] = rc.getID();
            encrypt(message);
            if (rc.canSubmitTransaction(message, 1)) {
                rc.submitTransaction(message, 1);
            }
        }
        if (wonBid) {
            // TODO: smart direction choice
            // TODO: reserve money to build building?
            // Build towards HQ
            Direction bestDir = null;
            int bestValue = -1;
            for (Direction dir : directions) {
                if ((hqLoc == null || !rc.adjacentLocation(dir).isAdjacentTo(hqLoc)) &&
                        rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, dir)) {
                    int dist = 15;
                    int value = 15;
                    if (hqLoc != null) {
                        dist = hqLoc.distanceSquaredTo(rc.adjacentLocation(dir));
                        if (dist <= 8) {
                            value = 1;
                        } else {
                            value = 999 - dist;
                        }
                    }
                    if (value > bestValue) {
                        bestValue = value;
                        bestDir = dir;
                    }
                }
            }
            if (bestDir != null) {
                if (tryBuild(RobotType.FULFILLMENT_CENTER, bestDir, true)) {
                    wonBid = false;
                    return true;
                }
            }
        }

        if (tryBuildNetGun()) {
            return true;
        }

        return false;
    }

    // Try to mine soup or move towards soup in sensor range.
    // Return to HQ if carrying max soup.
    // Returns true on action.
    // TODO: maintain this list better
    public static ArrayList<MapLocation> knownSoup = new ArrayList<MapLocation>();
    public static int lastGetOutOfWayRound = -999;
    public static final int newRefineryBuildingDistance = 100; // distance squared away from nearest refinery before we build a new one
    public static int numberOfTurnsFullOfSoup = 0; // the number of turns in a row during which we have been full of soup
    public static int numberOfTurnsNearRefinery = 0; // used to tell if we should give up on a refinery

    public static boolean tryMineSoup() throws GameActionException {
        // Update known soup
        for (int i = knownSoup.size() - 1; i >= 0; i--) {
            if (rc.canSenseLocation(knownSoup.get(i)) && rc.senseSoup(knownSoup.get(i)) == 0) {
                knownSoup.remove(i);
            }
        }

        // Return soup to refinery
        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
            numberOfTurnsFullOfSoup++;
            // Find nearest refinery
            // TODO: Deal with if we are disconnected from nearest refinery :(
            MapLocation bestKnownRefinery = null;
            for (MapLocation loc : refineryLocations) {
                if (bestKnownRefinery == null || rc.getLocation().distanceSquaredTo(loc) < rc.getLocation().distanceSquaredTo(bestKnownRefinery)) {
                    bestKnownRefinery = loc;
                }
            }

            if (bestKnownRefinery == null || rc.getLocation().distanceSquaredTo(bestKnownRefinery) > newRefineryBuildingDistance) {
                if (numberOfTurnsFullOfSoup < 5 || bestKnownRefinery == null) {
                    // first do a sense nearby robots check to see if there is a nearby refinery we didn't know about
                    RobotInfo[] robots = rc.senseNearbyRobots(newRefineryBuildingDistance, rc.getTeam());
                    for (RobotInfo robot : robots) {
                        if (robot.type == RobotType.HQ || robot.type == RobotType.REFINERY) {
                            MapLocation loc = robot.location;
                            if (unreachableRefineries.contains(loc) == false) {
                                refineryLocations.add(loc);
                                refineriesWithSoup.add(loc);
                                if (bestKnownRefinery == null || rc.getLocation().distanceSquaredTo(loc) < rc.getLocation().distanceSquaredTo(bestKnownRefinery)) {
                                    bestKnownRefinery = loc;
                                }
                            }
                        }
                    }
                    if (bestKnownRefinery == null || rc.getLocation().distanceSquaredTo(bestKnownRefinery) > newRefineryBuildingDistance) {
                        // Find best adj location to build a refinery
                        System.out.println("I want to build a refinery!");
                        Direction bestDir = null;
                        for (Direction dir : directions) {
                            if (rc.canBuildRobot(RobotType.REFINERY, dir) &&
                                    (bestDir == null || rc.senseElevation(rc.adjacentLocation(dir)) > rc.senseElevation(rc.adjacentLocation(bestDir)))) {
                                bestDir = dir;
                            }
                        }
                        if (bestDir != null && tryBuild(RobotType.REFINERY, bestDir, true)) {
                            MapLocation loc = rc.adjacentLocation(bestDir);
                            refineryLocations.add(loc);
                            refineriesWithSoup.add(loc);
                            // communicate the new refinery
                            int[] message = new int[7];
                            message[0] = MESSAGE_TYPE_REFINERY_LOC;
                            message[1] = loc.y * MAX_MAP_SIZE + loc.x;
                            encrypt(message);
                            if (rc.canSubmitTransaction(message, 1)) {
                                rc.submitTransaction(message, 1);
                            }
                            return true;
                        }
                    }
                }
                if (bestKnownRefinery == null) {
                    tryMoveToTendency();
                    return true;
                }
            }
            Direction dir = rc.getLocation().directionTo(bestKnownRefinery);
            if (rc.canDepositSoup(dir)) {
                rc.depositSoup(dir, rc.getSoupCarrying());
                return true;
            }
            // Give up on a refinery if we have been unable to reach it for 40 turns
            if (rc.getLocation().distanceSquaredTo(bestKnownRefinery) <= 20) {
                numberOfTurnsNearRefinery++;
                if (numberOfTurnsNearRefinery >= 40) {
                    numberOfTurnsNearRefinery = 0;
                    unreachableRefineries.add(bestKnownRefinery);
                    refineryLocations.remove(bestKnownRefinery);
                }
            } else {
                numberOfTurnsNearRefinery = 0;
            }
            tryMoveTowards(bestKnownRefinery);
            return true;
        }
        numberOfTurnsFullOfSoup = numberOfTurnsNearRefinery = 0;
        // Get out of the way of friendly miners
        // TODO: make this less shit
        if (lastGetOutOfWayRound <= roundNum - 3) {
            for (Direction dir : directions) {
                if (!rc.canSenseLocation(rc.adjacentLocation(dir))) {
                    continue;
                }
                RobotInfo robot = rc.senseRobotAtLocation(rc.adjacentLocation(dir));
                if (robot != null && robot.team == rc.getTeam() && robot.type == RobotType.MINER) {
                    Direction[] dirs = {dir.opposite(), dir.opposite().rotateLeft(), dir.opposite().rotateRight()};
                    for (Direction move : dirs) {
                        if (rc.senseSoup(rc.getLocation()) > 0 ||
                                (rc.canSenseLocation(rc.adjacentLocation(move)) && rc.senseSoup(rc.adjacentLocation(move)) > 0)) {
                            if (tryMove(move)) {
                                //rc.setIndicatorLine(robot.location, rc.getLocation(), 255, 0, 255);
                                lastGetOutOfWayRound = roundNum;
                                return true;
                            }
                        }
                    }
                }
            }
        }

        // Mine adjacent soup
        for (Direction dir : Direction.allDirections()) {
            if (rc.canMineSoup(dir)) {
                rc.mineSoup(dir);
                MapLocation loc = rc.adjacentLocation(dir);
                if (rc.senseSoup(loc) > 0 && knownSoup.indexOf(loc) == -1) {
                    minerGiveUp[loc.y / BLOCK_SIZE][loc.x / BLOCK_SIZE] = false;
                    System.out.println("Adding known soup location " + loc);
                    //rc.setIndicatorDot(rc.adjacentLocation(dir), 255, 255, 0);
                    knownSoup.add(loc);
                } else if (rc.senseSoup(loc) == 0) {
                    // find closest with soup, add to knownsoup, to prevent miner from ignoring nearby soup
                    int sensorRadius = rc.getCurrentSensorRadiusSquared();
                    for (int i = 0; i < offsetDist.length; i++) {
                        if (offsetDist[i] > sensorRadius) {
                            break;
                        }
                        loc = rc.getLocation().translate(offsetX[i], offsetY[i]);
                        if (onTheMap(loc) && rc.senseSoup(loc) > 0) {
                            if (knownSoup.indexOf(loc) == -1) {
                                knownSoup.add(loc);
                            }
                            break;
                        }
                    }
                }
                return true;
            }
        }

        // Find soup in sensor radius
        int sensorRadius = rc.getCurrentSensorRadiusSquared();
        for (int i = 0; i < offsetDist.length; i++) {
            if (offsetDist[i] > sensorRadius) {
                break;
            }
            MapLocation loc = rc.getLocation().translate(offsetX[i], offsetY[i]);
            // TODO: try again after some time
            if (onTheMap(loc) && !minerGiveUp[loc.y / BLOCK_SIZE][loc.x / BLOCK_SIZE] && rc.senseSoup(loc) > 0) {
                if (tryMoveTowards(loc)) {
                    return true;
                }
            }
        }

        // Go towards known soup locations
        MapLocation bestKnownSoup = null;
        for (MapLocation loc : knownSoup) {
            // TODO: use distance squared or manhattan? or something else?
            if (bestKnownSoup == null || rc.getLocation().distanceSquaredTo(loc) < rc.getLocation().distanceSquaredTo(bestKnownSoup)) {
                bestKnownSoup = loc;
            }
        }
        // If we don't know the locations of any soup; move towards refineries
        // We don't want to get stuck moving towards refineries with no soup left; thats what refineriesWithSoup is for
        if (bestKnownSoup == null) {
            for (MapLocation loc : refineriesWithSoup) {
                if (!loc.equals(hqLoc) && (bestKnownSoup == null || rc.getLocation().distanceSquaredTo(loc) < rc.getLocation().distanceSquaredTo(bestKnownSoup))) {
                    bestKnownSoup = loc;
                }
            }
            if (bestKnownSoup != null && rc.getLocation().distanceSquaredTo(bestKnownSoup) <= 2) {
                // Remove this from refineriesWithSoup, because we are not aware of any nearby soup
                // TODO: Maybe communicate this, or have the refineries themselves communicate this
                refineriesWithSoup.remove(bestKnownSoup);
                System.out.println("nearby refinery doesn't have soup");
            }
        }

        if (bestKnownSoup != null) {
            tryMoveTowards(bestKnownSoup);
            return true;
        }

        return false;
    }

    public static void updateRefineryLocations() throws GameActionException {
        // Check all existing refineries
        // If any doesn't exist - remove it and send a message proclaiming its death
        // This should always be called after checking new messages

        // TODO: Prevent multiple units from declaring the death of one

        if (refineryLocations.size() > 1) {
            for (int i = refineryLocations.size() - 1; i >= 0; i--) {
                MapLocation loc = refineryLocations.get(i);
                if (!loc.equals(hqLoc) && rc.canSenseLocation(loc)) {
                    RobotInfo robot = rc.senseRobotAtLocation(loc);
                    if (robot == null || robot.team != rc.getTeam() || robot.type != RobotType.REFINERY) {
                        // TODO: Handle if we can't afford to do this
                        int[] message = new int[7];
                        message[0] = MESSAGE_TYPE_REFINERY_IS_DEAD;
                        message[1] = loc.y * MAX_MAP_SIZE + loc.x;
                        encrypt(message);
                        if (rc.canSubmitTransaction(message, 1)) {
                            rc.submitTransaction(message, 1);
                        }
                        refineryLocations.remove(i);
                        refineriesWithSoup.remove(i);
                    }
                }
            }
        }
    }

    public static boolean tryBuildNetGun() throws GameActionException {
        // TODO: might need to save money in some situations?
        if (rc.getTeamSoup() < RobotType.NET_GUN.cost) {
            return false;
        }

        // try to build netgun if you sense a nearby enemy drone
        RobotInfo[] enemies = rc.senseNearbyRobots(99999, rc.getTeam().opponent());
        boolean foundDrone = false;
        for (RobotInfo enemy : enemies) {
            if (enemy.type == RobotType.DELIVERY_DRONE) {
                foundDrone = true;
                break;
            }
        }
        if (!foundDrone) {
            return false;
        }

        // should be within 5 units of a friendly building (that is not a net gun/HQ)
        // and should not be within 24 units of a friendly net gun/HQ
        // try to make it closer to friendly buildings (except net guns/HQs) and further from friendly net guns/HQs
        RobotInfo[] allies = rc.senseNearbyRobots(9999, rc.getTeam());
        int[] closestFriendlyBuilding = new int[8];
        int[] closestNetGun = new int[8];
        for (int i = 0; i < 8; i++) {
            closestNetGun[i] = closestFriendlyBuilding[i] = 9999;
        }
        for (RobotInfo ally : allies) {
            if (ally.type == RobotType.NET_GUN || ally.type == RobotType.HQ) {
                for (int i = 0; i < 8; i++) {
                    closestNetGun[i] = Math.min(closestNetGun[i], rc.adjacentLocation(directions[i]).distanceSquaredTo(ally.location));
                }
            } else if (ally.type.isBuilding()) {
                for (int i = 0; i < 8; i++) {
                    closestFriendlyBuilding[i] = Math.min(closestFriendlyBuilding[i],
                            rc.adjacentLocation(directions[i]).distanceSquaredTo(ally.location));
                }
            }
        }
        int bestDir = -1;
        int bestScore = -9999;
        for (int i = 0; i < 8; i++) {
            if (closestFriendlyBuilding[i] <= 5 && closestNetGun[i] >= 25) {
                int score = 2 * closestNetGun[i] - closestFriendlyBuilding[i];
                if (score > bestScore && rc.canBuildRobot(RobotType.NET_GUN, directions[i])) {
                    bestDir = i;
                    bestScore = score;
                }
            }
        }
        if (bestDir != -1) {
            rc.setIndicatorLine(rc.getLocation(), rc.adjacentLocation(directions[bestDir]), 255, 255, 255);
            System.out.println("my sensor range is " + rc.getCurrentSensorRadiusSquared() + " " + closestFriendlyBuilding[bestDir] +
                    " " + closestNetGun[bestDir]);
            rc.buildRobot(RobotType.NET_GUN, directions[bestDir]);
            return true;
        }

        return false;
    }
}