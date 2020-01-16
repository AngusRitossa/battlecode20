package okbot;

import battlecode.common.*;

public class Landscaper extends RobotPlayer {
    public static void runLandscaper() throws GameActionException {
        // TODO: implement landscaper

        readBlockchain(9000);

        doLandscaperAction();

        readBlockchain(1500);
    }

    public static void doLandscaperAction() throws GameActionException {
        findHQ();

        if (!rc.isReady()) {
            return;
        }

        if (tryDefend()) {
            return;
        }
        // Defend HQ
//        if (tryLandscaperDefend(hqLoc)) {
//            return;
//        }
//
//        // Defend other buildings
//        RobotInfo[] robots = rc.senseNearbyRobots(rc.getLocation(), 99999, rc.getTeam());
//        for (RobotInfo robot : robots) {
//            if (robot.type.isBuilding() && tryLandscaperDefend(robot.location)) {
//                return;
//            }
//        }
        // TODO: take into account friendly HQ and net guns
        if (tryRunAwayFromDrones()) {
            return;
        }

        if (tryTurtle()) {
            return;
        }

        if (tryLandscaperAttackBuildings()) {
            return;
        }

        if (possibleEnemyHQLocs.size() > 1) {
            for (int i = possibleEnemyHQLocs.size() - 1; i >= 0; i--) {
                MapLocation loc = possibleEnemyHQLocs.get(i);
                if (rc.canSenseLocation(loc)) {
                    RobotInfo robot = rc.senseRobotAtLocation(loc);
                    if (robot == null || robot.type != RobotType.HQ) {
                        possibleEnemyHQLocs.remove(i);
                    } else {
                        for (int j = possibleEnemyHQLocs.size() - 1; j > i; j--) {
                            possibleEnemyHQLocs.remove(j);
                        }
                        while (possibleEnemyHQLocs.size() > 1) {
                            possibleEnemyHQLocs.remove(0);
                        }
                        break;
                    }
                }
            }
        }
        if (!seenEnemyHQMessage && possibleEnemyHQLocs.size() == 1) {
            int[] message = new int[7];
            message[0] = MESSAGE_TYPE_ENEMY_HQ_LOC;
            message[1] = possibleEnemyHQLocs.get(0).y * MAX_MAP_SIZE + possibleEnemyHQLocs.get(0).x;
            encrypt(message);
            if (rc.canSubmitTransaction(message, 1)) {
                rc.submitTransaction(message, 1);
            }
        }
        if (possibleEnemyHQLocs.isEmpty()) {
            // TODO: scan for enemy HQ
            System.out.println("can't find enemy HQ");
        }

        if (!possibleEnemyHQLocs.isEmpty()) {
            MapLocation loc = possibleEnemyHQLocs.get(0);
            // TODO: different movement methods (digging, carried by drone)
            tryMoveTowards(loc);
            return;
        }

        if (tendency == null) {
            tendency = randomDirection();
        }
        tryMoveToTendency();
    }

    public static boolean tryLandscaperAttackBuildings() throws GameActionException {
        MapLocation dest = null;
        boolean targetIsHQ = false;

        int sensorRadius = rc.getCurrentSensorRadiusSquared();
        for (int i = 0; i < offsetDist.length; i++) {
            if (offsetDist[i] > sensorRadius) {
                break;
            }
            MapLocation loc = rc.getLocation().translate(offsetX[i], offsetY[i]);
            if (onTheMap(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot != null && robot.team != rc.getTeam() && robot.type.isBuilding()) {
                    dest = loc;
                    targetIsHQ = robot.type == RobotType.HQ;
                    break;
                }
            }
        }

        if (dest != null) {
            rc.setIndicatorLine(rc.getLocation(), dest, 63, 63, 63);
            Direction dir = rc.getLocation().directionTo(dest);
            if (rc.adjacentLocation(dir).equals(dest)) {
                if (rc.canDepositDirt(dir)) {
                    rc.depositDirt(dir);
                    return true;
                }
                // TODO: dig in smart direction
                Direction bestDir = null;
                for (Direction dir2 : directions) {
                    if (dir2 != dir && rc.canDigDirt(dir2)) {
                        RobotInfo robot = rc.senseRobotAtLocation(rc.adjacentLocation(dir2));
                        if (robot != null && robot.team != rc.getTeam()) {
                            if (robot.type.isBuilding()) {
                                // don't help enemy buildings
                                continue;
                            } else {
                                bestDir = dir2;
                            }
                        } else if (robot != null && robot.team == rc.getTeam() && robot.type != RobotType.DELIVERY_DRONE) {
                            // don't dig under friendly robots except drones
                            continue;
                        } else {
                            if (bestDir == null) bestDir = dir2;
                        }
                    }
                }
                if (bestDir == null && rc.canDigDirt(Direction.CENTER)) {
                    bestDir = Direction.CENTER;
                }
                if (bestDir != null) {
                    // TODO: add indicator dot and decide on colours for different actions
                    rc.setIndicatorDot(rc.adjacentLocation(bestDir), 0, 255, 255);
                    rc.digDirt(bestDir);
                    return true;
                }
            } else {
                tryMoveTowards(dest);
                return true;
            }
        }
        return false;
    }

    public static boolean tryLandscaperDefend(MapLocation loc) throws GameActionException {
        // TODO: defend multiple buildings if you are next to multiple buildings
        RobotInfo[] robots = rc.senseNearbyRobots(loc, 2, rc.getTeam().opponent());
        for (RobotInfo robot : robots) {
            if (robot.type == RobotType.LANDSCAPER) {
                if (rc.getLocation().isAdjacentTo(loc)) {
                    Direction dir = rc.getLocation().directionTo(loc);
                    if (rc.canDigDirt(dir)) {
                        rc.setIndicatorDot(loc, 0, 255, 255);
                        rc.digDirt(dir);
                    } else if (rc.canDepositDirt(dir.opposite())) {
                        // TODO: smarter deposit direction
                        rc.setIndicatorDot(loc, 127, 0, 0);
                        rc.depositDirt(dir.opposite());
                    }
                } else {
                    tryMoveTowards(loc);
                }
                return true;
            }
        }
        return false;
    }

    // Defence priorities, lower ordinal is more important
    public enum DefencePriority {
        DIG_HQ, // adjacent HQ has dirt on it, dig
        HQ_URGENT, // nearby HQ has less adjacent friendly landscapers than enemy landscapers, move to it
        DIG_FRIENDLY_BUILDING,
        FRIENDLY_BUILDING_URGENT,
        HQ_BACKUP, // nearby HQ has same amount of friendly and enemy landscapers, and enemies > 0 move to it if nothing else urgent
        FRIENDLY_BUILDING_BACKUP,
    }
    public static boolean tryDefend() throws GameActionException {
        DefencePriority bestPriority = null;
        MapLocation target = null;
        boolean actionIsDig = false;

        RobotInfo[] allies = rc.senseNearbyRobots(999, rc.getTeam());
        for (RobotInfo ally : allies) {
            if (!ally.type.isBuilding()) {
                continue;
            }
            // ally and enemy count of landscapers (except self)
            int allyCount = 0, enemyCount = 0, freeSpots = 0;
            for (Direction dir : directions) {
                MapLocation loc = ally.location.add(dir);
                if (rc.canSenseLocation(loc)) {
                    RobotInfo adj = rc.senseRobotAtLocation(loc);
                    if (adj == null || adj.ID == rc.getID()) freeSpots++;
                    if (adj != null && adj.type == RobotType.LANDSCAPER) {
                        if (adj.team == rc.getTeam()) {
                            // When counting friendly landscapers, don't count self
                            if (adj.ID != rc.getID()) {
                                allyCount++;
                            }
                        } else {
                            enemyCount++;
                        }
                    }
                }
            }
            if (freeSpots == 0) {
                // can't help
                continue;
            }
            int danger = -1;
            if (enemyCount > 0) danger = enemyCount - allyCount;
            DefencePriority priority = null;
            boolean isDig = rc.getLocation().isAdjacentTo(ally.location) &&
                    (rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit || rc.canDigDirt(rc.getLocation().directionTo(ally.location)));
            if (ally.type == RobotType.HQ) {
                if (isDig) priority = DefencePriority.DIG_HQ;
                else if (danger > 0) priority = DefencePriority.HQ_URGENT;
                else if (danger == 0) priority = DefencePriority.HQ_BACKUP;
            } else {
                if (isDig) priority = DefencePriority.DIG_FRIENDLY_BUILDING;
                else if (danger > 0) priority = DefencePriority.FRIENDLY_BUILDING_URGENT;
                else if (danger == 0) priority = DefencePriority.FRIENDLY_BUILDING_BACKUP;
            }

            if (priority != null && (bestPriority == null || priority.ordinal() < bestPriority.ordinal())) {
                bestPriority = priority;
                target = ally.location;
                actionIsDig = isDig;
            }
        }

        if (target != null) {
            if (actionIsDig) {
                // TODO: smart deposit direction choice
                Direction dir = rc.getLocation().directionTo(target);
                if (rc.canDigDirt(dir)) {
                    rc.digDirt(dir);
                } else if (rc.getDirtCarrying() > 0) {
                    Direction bestDir = null;
                    int bestValue = -1;
                    for (Direction depDir : directions) {
                        MapLocation loc = rc.adjacentLocation(depDir);
                        if (onTheMap(loc)) {
                            RobotInfo robot = rc.senseRobotAtLocation(loc);
                            int value = -1;
                            if (robot != null && robot.team == rc.getTeam() && robot.type != RobotType.DELIVERY_DRONE) {
                                continue;
                            } else if (robot != null && robot.team == rc.getTeam().opponent() && robot.type != RobotType.DELIVERY_DRONE) {
                                if (robot.type.isBuilding()) {
                                    value = 99;
                                } else {
                                    value = 50;
                                }
                            } else {
                                value = 10;
                            }
                            if (value > bestValue) {
                                bestValue = value;
                                bestDir = depDir;
                            }
                        }
                    }
                    if (bestDir == null) {
                        bestDir = Direction.CENTER;
                    }
                    rc.depositDirt(bestDir);
                }
            } else {
                if (!rc.getLocation().isAdjacentTo(target)) {
                    tryMoveTowards(target);
                }
            }
            System.out.println("Defending " + target);
            return true;
        }
        return false;
    }

    // Move towards HQ and check if you need to turtle
    public static boolean amTurtle = false;
    public static int triedTurtleRound = -1;
    public static boolean tryTurtle() throws GameActionException {
        // TODO: maybe try turtling again when it's past some round?
        // maybe not though because we don't want to pull away attacking landscapers for no reason
        if (triedTurtleRound != -1) {
            // If it's been a while since you tried turtling and there are no nearby enemies, try it again
            if (roundNum >= triedTurtleRound + 150 || roundNum == 450) {
                RobotInfo[] robots = rc.senseNearbyRobots(9999, rc.getTeam().opponent());
                if (robots.length == 0) {
                    triedTurtleRound = -1;
                }
            }
            if (triedTurtleRound != -1) {
                return false;
            }
        }

        if (hqLoc == null) {
            return false;
        }

        // If can't sense all surrounding squares of HQ, move towards HQ
        for (Direction dir : directions) {
            if (onTheMap(hqLoc.add(dir)) && !rc.canSenseLocation(hqLoc.add(dir))) {
                tryMoveTowards(hqLoc);
                return true;
            }
        }

        // TODO: deal with earlier flooding, either by making landscaper move around HQ or by making landscapers turtle faster
        // If less than desired number of landscapers adjacent, find a free spot or else fail
        // If desired number reached then don't worry about turtling
        // TODO: ensure that if the squares are taken up by enemy robots of some kind, the response to the enemy is adequate
        int desired = 0;
        if (roundNum > 125) desired = 1;
        if (roundNum > 150) desired = 2;
        if (roundNum > 175) desired = 4;
        if (roundNum > 225) desired = 6;
        if (roundNum > 325) desired = 8;
        int current = 0;
        int bestDistToHQ = 999;
        int bestDist = 999;
        MapLocation freeSpot = null;
        for (Direction dir : directions) {
            MapLocation loc = hqLoc.add(dir);
            if (!onTheMap(loc)) {
                continue;
            }
            RobotInfo robot = rc.senseRobotAtLocation(loc);
            // Don't count self
            if (robot != null && robot.team == rc.getTeam() && robot.type == RobotType.LANDSCAPER && robot.ID != rc.getID()) {
                current++;
            } else if (robot == null) {
                // Prefer squares in cardinal directions to HQ.
                int distToHQ = loc.distanceSquaredTo(hqLoc);
                int dist = rc.getLocation().distanceSquaredTo(loc);
                if (distToHQ < bestDistToHQ || (distToHQ == bestDistToHQ && dist < bestDist)) {
                    bestDistToHQ = distToHQ;
                    bestDist = dist;
                    freeSpot = loc;
                }
            }
        }
        // TODO: move inward if other landscapers die
        if (!amTurtle) {
            if (current < desired && (roundNum < 1000 || rc.getLocation().distanceSquaredTo(hqLoc) >= 9)) {
                if (rc.getLocation().isAdjacentTo(hqLoc) && rc.getLocation().distanceSquaredTo(hqLoc) <= bestDistToHQ) {
                    amTurtle = true;
                } else if (freeSpot != null) {
                    tryMoveTowards(freeSpot);
                    return true;
                } else {
                    tryMoveTowards(hqLoc);
                    return true;
                }
            } else {
                if (roundNum < 450) {
                    triedTurtleRound = roundNum;
                    return false;
                } else {
                    // If it's past some round just turtle as much as you can
                    if (rc.getLocation().distanceSquaredTo(hqLoc) <= 8) {
                        if (rc.getDirtCarrying() > 0) {
                            int lowestElevation = rc.senseElevation(rc.getLocation());
                            Direction bestDir = Direction.CENTER;
                            // TODO: calculate actual optimal point
                            if (lowestElevation > 150) {
                                lowestElevation = 999999;
                                bestDir = null;
                            }
                            for (Direction dir : directions) {
                                MapLocation loc = rc.adjacentLocation(dir);
                                if (!onTheMap(loc) || loc.equals(hqLoc) || !rc.canDepositDirt(dir) || !hqLoc.isAdjacentTo(loc)) {
                                    continue;
                                }
                                int elevation = rc.senseElevation(loc);
                                if (elevation < lowestElevation) {
                                    lowestElevation = elevation;
                                    bestDir = dir;
                                }
                            }
                            if (bestDir != null) {
                                rc.setIndicatorDot(rc.adjacentLocation(bestDir), 255, 255, 255);
                                rc.depositDirt(bestDir);
                                System.out.println("deposit in " + bestDir);
                            }
                        } else {
                            int bestQuality = -1;
                            Direction bestDir = null;
                            for (Direction dir : directions) {
                                MapLocation loc = rc.adjacentLocation(dir);
                                if (!onTheMap(loc) || loc.isAdjacentTo(hqLoc)) {
                                    continue;
                                }
                                RobotInfo robot = rc.senseRobotAtLocation(loc);
                                if ((robot != null && robot.team == rc.getTeam() && robot.type != RobotType.DELIVERY_DRONE)
                                        || !rc.canDigDirt(dir)) {
                                    continue;
                                }
                                int quality = 1;
                                if (loc.distanceSquaredTo(hqLoc) == 4) {
                                    quality = 2;
                                }
                                if (loc.distanceSquaredTo(hqLoc) >= 9) {
                                    quality = 3;
                                }
                                if (quality > bestQuality) {
                                    bestQuality = quality;
                                    bestDir = dir;
                                }
                            }
                            if (bestDir != null) {
                                rc.digDirt(bestDir);
                                System.out.println("dig in " + bestDir);
                            }
                        }
                    } else {
                        tryMoveTowards(hqLoc);
                    }
                    return true;
                }
            }
        }

        // amTurtle == true
        // Do turtling
        // TODO: smarter dig direction choice
        // TODO: smarter deposit direction choice
        if (rc.getDirtCarrying() > 0) {
            // TODO: deal with HQ in a crater scenario
            int lowestElevation = rc.senseElevation(rc.getLocation());
            Direction bestDir = Direction.CENTER;
            for (Direction dir : directions) {
                MapLocation loc = rc.adjacentLocation(dir);
                if (!onTheMap(loc) || loc.equals(hqLoc) || !rc.canDepositDirt(dir) || !hqLoc.isAdjacentTo(loc)) {
                    continue;
                }
                int elevation = rc.senseElevation(loc);
                if (roundNum < 1500 && elevation >= 0 && elevation < 100 && roundNum < water_level_round[elevation] - 100) {
                    continue;
                }
                if (elevation < lowestElevation) {
                    lowestElevation = elevation;
                    bestDir = dir;
                }
            }
            rc.setIndicatorDot(rc.adjacentLocation(bestDir),255,255,255);
            rc.depositDirt(bestDir);
            System.out.println("deposit in " + bestDir);
        } else {
            int bestQuality = -1;
            Direction bestDir = null;
            for (Direction dir : directions) {
                MapLocation loc = rc.adjacentLocation(dir);
                if (!onTheMap(loc) || loc.isAdjacentTo(hqLoc)) {
                    continue;
                }
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if ((robot != null && robot.team == rc.getTeam() && robot.type != RobotType.DELIVERY_DRONE)
                        || !rc.canDigDirt(dir)) {
                    continue;
                }
                int quality = 1;
                if (loc.distanceSquaredTo(hqLoc) == 4) {
                    quality = 2;
                }
                if (loc.distanceSquaredTo(hqLoc) >= 9) {
                    quality = 3;
                }
                if (quality > bestQuality) {
                    bestQuality = quality;
                    bestDir = dir;
                }
            }
            if (bestDir != null) {
                rc.digDirt(bestDir);
                System.out.println("dig in " + bestDir);
            }
        }
        return true;
    }
}
