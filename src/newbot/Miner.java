package newbot;

import battlecode.common.*;
import java.util.*;

public class Miner extends RobotPlayer {
    public static void runMiner() throws GameActionException {
    	readBlockchain(9000);
    	findHQ();
        doAction();
        readBlockchain(2000);
    }
    public static int hangAroundHQ = -1;
    public static void doAction() throws GameActionException {
        updateKnownRefineries();
        updateKnownDesignSchools();
        updateKnownFulfillmentCenters();
        updateKnownNetGuns();
        updateKnownVaporators();
        lookForNewBuildings();
        checkEnemyHQLocs();

        if (hangAroundHQ == -1 && hqLoc != null) {
            hangAroundHQ = (int) (Math.random() * 420);
            hangAroundHQ += rc.getLocation().x + rc.getLocation().y + Clock.getBytecodesLeft();
            hangAroundHQ %= 3;
            if (hangAroundHQ != 0) {
                hangAroundHQ = 1;
            }
        }
        if (rc.getRoundNum() > startTurtlingHQRound && hangAroundHQ == 0 && 
            rc.canSenseLocation(rc.getLocation()) && rc.senseElevation(rc.getLocation()) == lowerTurtleHeight) {
            hangAroundHQ = 1;
        }
        if (!rc.isReady()) {
            return;
        }
        if (enemyHqLoc != null && rc.getRoundNum() > swarmGoAllInRound) {
            if (tryDoMinerNextToEnemyHq()) {
                return;
            }
        }
        if (tryBuildNetGunIfScared()) {
            return;
        }
        if (minerTryRunAwayFromDrone()) {
            return;
        }
        if (hangAroundHQ == 1 && knownVaporators.size() < 2 && rc.getLocation().distanceSquaredTo(hqLoc) > 20 && rc.getTeamSoup() > 500 && soupReserve() == 500) {
        	// long statement, but basically we want a vaporator, so move towards hq to build one
        	if (tryMoveTowards(hqLoc)) {
        		return;
        	}
        }
        if (hangAroundHQ == 1 && rc.getRoundNum() >= startTurtlingHQRound) {
            if (tryBeTurtleMiner()) {
                return;
            }
        }
        if (tryBuildBuilding(false, RobotType.DESIGN_SCHOOL)) {
            return;
        }
        if (rc.getRoundNum() > 175 && tryBuildBuilding(false, RobotType.FULFILLMENT_CENTER)) {
            return;
        }
        if (tryBuildVaporator()) {
        	return;
        }
        if (tryMineSoup()) {
        	return;
        }
        if (tryMoveTowardsSoup()) {
        	return;
        }
        if (tryMoveTowardsRefinery()) {
        	return;
        }
        if (tryMoveRandomly()) {
        	return;
        }
    }

    public static int numberTurtleDesignSchoolsWanted() throws GameActionException {
        /*if (rc.getRoundNum() < 800) {
            return 3;
        }*/
        return 2;
    }
    public static int numberTurtleFulfillmentCentersWanted() throws GameActionException {
        /*if (rc.getRoundNum() < 800) {
            return 2;
        }*/
        if (rc.getRoundNum() < 1200) {
            return 1;
        } else if (rc.getRoundNum() < 1500) {
            return 2;
        } else {
            return 3;
        }
    }

    public static int vaporatorsBuilt = 0; // we build a vaporator before anything else, bc money
    public static int netGunsBuilt = 0;
    public static boolean tryBeTurtleMiner() throws GameActionException {
        // If we're not on the turtle, try to get on it
        // Otherwise, build a refinery or move randomly
        if (hqLoc == null) {
            return false;
        }
        // Miners who hang around on the turtle to build the necessary buildings
        if (rc.senseElevation(rc.getLocation()) < lowerTurtleHeight-2) {
            // walk towards hq
            System.out.println("Trying to get onto turtle");
            // if we are adjacent to the turtle, on a square that will be turtled, stay
            boolean nextToTurtle = false;
            for (Direction dir : directions) {
                MapLocation loc = rc.getLocation().add(dir);
                if (rc.canSenseLocation(loc) && rc.senseElevation(loc) == lowerTurtleHeight) {
                    RobotInfo robot = rc.senseRobotAtLocation(loc);
                    if (robot != null && robot.team == rc.getTeam() && robot.type == RobotType.LANDSCAPER) {
                        nextToTurtle = true;
                    }
                }
            }
            if (nextToTurtle && !canBeDugForLowerTurtle(rc.getLocation())) {
                System.out.println("staying still to be lifted onto turtle");
                return true;
            }
            if (rc.getLocation().distanceSquaredTo(hqLoc) > 8) {
            	return tryMoveTowards(hqLoc);
            }
            return false; 
        }
        if (knownVaporators.size() >= knownDesignSchools.size() && knownDesignSchools.size() < numberTurtleDesignSchoolsWanted() && knownDesignSchools.size() <= knownFulfillmentCenters.size()+1) {
            System.out.println("want to build design school on turtle");
            if (tryBuildBuilding(true, RobotType.DESIGN_SCHOOL)) {
                return true;
            } else {
                // we really want to build this design school, so move in the hope of being able to
                if (rc.getLocation().distanceSquaredTo(hqLoc) >= 40) {
                    tryMoveTowards(hqLoc);
                    return true;
                } else {
                    tryMoveRandomly();
                    return true;
                }
            }
        }
        if (knownVaporators.size() >= knownFulfillmentCenters.size() && knownFulfillmentCenters.size() < numberTurtleFulfillmentCentersWanted()) {
        	if (tryBuildBuilding(true, RobotType.FULFILLMENT_CENTER)) {
                return true;
            } else {
                // we really want to build this fulfillment center, so move in the hope of being able to
                if (rc.getLocation().distanceSquaredTo(hqLoc) >= 40) {
                    tryMoveTowards(hqLoc);
                    return true;
                } else {
                    tryMoveRandomly();
                    return true;
                }
            }
        }
        if (vaporatorsBuilt > netGunsBuilt*6 && knownVaporators.size() > 4) {
            if (tryBuildNetGun()) {
                return true;
            }
        }
        if (tryBuildVaporator()) {
            return true;
        }
        return false;
    }

    public static boolean isSuitableLocationForBuilding(MapLocation loc) throws GameActionException {
        // Currently just checks elevation == lowerTurtleHeight
        if (!rc.canSenseLocation(loc)) {
            return false;
        }
        if (rc.senseElevation(loc) < lowerTurtleHeight) {
            return false;
        }
        if (loc.distanceSquaredTo(hqLoc) < 8) {
            return false;
        }
        RobotInfo robot = rc.senseRobotAtLocation(loc);
        if (robot != null) {
            return false;
        }
        return (loc.x - hqLoc.x)%3 != 0 && (loc.y - hqLoc.y)%3 != 0;
    }
    public static boolean isSuitableLocationForVaporator(MapLocation loc) throws GameActionException {
        // considers building off turtle if near enough (15) to hq
        if (!rc.canSenseLocation(loc)) {
            return false;
        }
        if (rc.senseElevation(loc) < lowerTurtleHeight && rc.getLocation().distanceSquaredTo(hqLoc) > 20) {
            return false;
        }
        if (rc.senseElevation(loc) < 50 && (rc.senseElevation(loc) < 0 || rc.senseElevation(loc) >= water_level_round.length || rc.getRoundNum()+300 > water_level_round[rc.senseElevation(loc)])) {
        	return false;
        }
        if (loc.distanceSquaredTo(hqLoc) < 8) {
            return false;
        }
        RobotInfo robot = rc.senseRobotAtLocation(loc);
        if (robot != null) {
            return false;
        }
        return (loc.x - hqLoc.x)%3 != 0 && (loc.y - hqLoc.y)%3 != 0;
    }
    public static int soupReserveForVaporator() {
    	if (knownVaporators.size() < 15) {
    		return 0;
    	}
    	if (knownVaporators.size() < 20) {
    		return 250;
    	}
    	if (knownVaporators.size() < 30) {
    		return 600;
    	}
    	if (knownVaporators.size() < 50) {
    		return 1000;
    	}
    	return 2500;
    }
    public static boolean tryBuildVaporator() throws GameActionException {
        // TODO: Be smart?
        if (rc.getTeamSoup() < RobotType.VAPORATOR.cost + soupReserveForVaporator()) {
        	return false;
        }
        if (rc.getRoundNum()+250 > water_level_round[lowerTurtleHeight]) {
            // a vaporator won't pay itself off, so don't build
            return false;
        }
        for (Direction dir : directions) {
            MapLocation loc = rc.getLocation().add(dir);
            if (isSuitableLocationForVaporator(loc)) {
                if (tryBuildInDir(RobotType.VAPORATOR, dir, true)) {
                	vaporatorsBuilt++;
                    System.out.println("I built a vaporator");
                    return true;
                }
            }
        }   
        return false;
    }
    public static boolean tryBuildNetGun() throws GameActionException {
        // TODO: Be smart?
        for (Direction dir : directions) {
            MapLocation loc = rc.getLocation().add(dir);
            if (isSuitableLocationForBuilding(loc)) {
                if (tryBuildInDir(RobotType.NET_GUN, dir, true)) {
                    netGunsBuilt++;
                    System.out.println("I built a net gun");
                    return true;
                }
            }
        }   
        return false;
    }

    public static boolean tryMineSoup() throws GameActionException {
        // mine soup at the hq first so we can move away and not crowd it
        if (hqLoc != null && rc.getLocation().isAdjacentTo(hqLoc)) {
            Direction dir = rc.getLocation().directionTo(hqLoc);
            if (rc.canMineSoup(dir)) {
                rc.mineSoup(dir);
                return true;
            }
        }
    	for (Direction dir : Direction.allDirections()) {
    		if (rc.canMineSoup(dir)) {
    			rc.mineSoup(dir);
    			return true;
    		}
    	}
    	return false;
    }

    public static ArrayList<MapLocation> knownSoup = new ArrayList<MapLocation>(); // stores up to 10 known soup locations
    public static boolean tryMoveTowardsSoup() throws GameActionException {
    	if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
    		return false;
    	}
    	numTurnsFullSoup = numTurnsNearRefineryForDeposit = 0;

    	// Remove soupless locations from knownSoup
    	for (int i = knownSoup.size() - 1; i >= 0; i--) {
            if (rc.canSenseLocation(knownSoup.get(i)) && rc.senseSoup(knownSoup.get(i)) == 0) {
                knownSoup.remove(i);
            }
        }

        MapLocation[] nearbySoup = rc.senseNearbySoup();
        if (nearbySoup.length > 0) {
        	// Find the closest soup; move towards it
        	// Add the furthest soup to senseSoup, so we have it for future reference
        	MapLocation closestSoup = null;
        	MapLocation furthestSoup = null;
        	for (MapLocation loc : nearbySoup) {
        		if (closestSoup == null || rc.getLocation().distanceSquaredTo(loc) < rc.getLocation().distanceSquaredTo(closestSoup)) {
        			closestSoup = loc;
        		}
        		if (furthestSoup == null || rc.getLocation().distanceSquaredTo(loc) > rc.getLocation().distanceSquaredTo(furthestSoup)) {
        			furthestSoup = loc;
        		}
        	}
        	if (knownSoup.size() < 10 && !knownSoup.contains(closestSoup)) {
        		knownSoup.add(closestSoup);
        	}
        	if (knownSoup.size() < 10 && !knownSoup.contains(furthestSoup)) {
        		knownSoup.add(furthestSoup);
        	}
        	System.out.println("Moving towards known soup I can see");
        	tryMoveTowards(closestSoup);
        	return true;
        } else if (knownSoup.size() > 0) {
        	MapLocation closestSoup = null;
        	for (MapLocation loc : knownSoup) {
        		if (closestSoup == null || rc.getLocation().distanceSquaredTo(loc) < rc.getLocation().distanceSquaredTo(closestSoup)) {
        			closestSoup = loc;
        		}
        	}
        	System.out.println("Moving towards known soup I can't see");
        	tryMoveTowards(closestSoup);
        	return true;
        } else {
        	// Shame, we have nowhere to go
        	return false;
        }
    }

    public static final int newRefineryBuildDistance = 100;
    public static int numTurnsFullSoup = 0;
    public static int numTurnsNearRefineryForDeposit = 0;
    public static int numTurnsNearRefineryWithSoup = 0;

    public static boolean tryMoveTowardsRefinery() throws GameActionException { // Also deposits soup if possible
    	if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
    		numTurnsFullSoup++;
    		// Find nearest refinery
    		MapLocation nearestRefinery = null;
    		for (MapLocation loc : knownRefineries) {
    			if (nearestRefinery == null || rc.getLocation().distanceSquaredTo(nearestRefinery) > rc.getLocation().distanceSquaredTo(loc)) {
    				nearestRefinery = loc;
    			}
    		}
    		if (nearestRefinery == null || (numTurnsFullSoup < 5 && rc.getLocation().distanceSquaredTo(nearestRefinery) > newRefineryBuildDistance)) {
    			// double check there isn't a nearby refinery
    			RobotInfo[] robots = rc.senseNearbyRobots(newRefineryBuildDistance, rc.getTeam());
                for (RobotInfo robot : robots) {
                    if (robot.type == RobotType.REFINERY || robot.type == RobotType.HQ) {
                        MapLocation loc = robot.location;
                        if (!unreachableRefineries.contains(loc)) {
                            knownRefineries.add(loc);
                            if (nearestRefinery == null || rc.getLocation().distanceSquaredTo(nearestRefinery) > rc.getLocation().distanceSquaredTo(loc)) {
                                nearestRefinery = loc;
                            }
                        }
                    }
                }
                if (nearestRefinery == null || rc.getLocation().distanceSquaredTo(nearestRefinery) > newRefineryBuildDistance) {
                	System.out.println("I want to build a refinery!");
                    Direction bestDir = null;
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.REFINERY, dir) &&
                                (hqLoc == null || rc.adjacentLocation(dir).distanceSquaredTo(hqLoc) > 9) &&
                               	(bestDir == null || rc.senseElevation(rc.adjacentLocation(dir)) > rc.senseElevation(rc.adjacentLocation(bestDir)))) {
                            bestDir = dir;
                        }
                    }
                    if (bestDir != null && tryBuildInDir(RobotType.REFINERY, bestDir, true)) {
                    	return true;
                    }
                }

    		}
    		if (nearestRefinery == null) {
    			return false;
    		}
    		// Try to deposit soup
	    	Direction dir = rc.getLocation().directionTo(nearestRefinery);
	        if (rc.canDepositSoup(dir)) {
	            rc.depositSoup(dir, rc.getSoupCarrying());
	            return true;
	        }

	        // Give up on refinery if its too far away
	        if (rc.getLocation().distanceSquaredTo(nearestRefinery) <= 20) {
                numTurnsNearRefineryForDeposit++;
                if (numTurnsNearRefineryForDeposit >= 20) {
                    numTurnsNearRefineryForDeposit = 0;
                    unreachableRefineries.add(nearestRefinery);
                    knownRefineries.remove(nearestRefinery);
                }
            } else {
                numTurnsNearRefineryForDeposit = 0;
            }
            System.out.println("Moving towards a refinery because I'm full of soup");
	        return tryMoveTowards(nearestRefinery);
    	} else {
    		numTurnsFullSoup = 0;
    		// move towards a refinery with soup
    		MapLocation nearestRefinery = null;
    		for (MapLocation loc : knownRefineriesWithSoup) {
    			if (nearestRefinery == null || rc.getLocation().distanceSquaredTo(nearestRefinery) > rc.getLocation().distanceSquaredTo(loc)) {
    				nearestRefinery = loc;
    			}
    		}

    		if (nearestRefinery != null) {
    			System.out.println("Moving towards a refinery in the hope of finding soup " + numTurnsNearRefineryWithSoup);
    			// If we've been close to this refinery for too long, give up
    			if (rc.getLocation().distanceSquaredTo(nearestRefinery) <= 20) {
    				numTurnsNearRefineryWithSoup++;
    				if (numTurnsNearRefineryWithSoup >= 10) {
    					numTurnsNearRefineryWithSoup = 0;
    					knownRefineriesWithSoup.remove(nearestRefinery);
    				}
    			} else {
    				numTurnsNearRefineryWithSoup = 0;
    			}
    			return tryMoveTowards(nearestRefinery);
    		} else {
    			return false;
    		}
    	}
    }

    

    public static int lastBuiltBuilding;

    public static boolean tryBuildBuilding(boolean isOnTurtle, RobotType buildingType) throws GameActionException {
        int numberOfBuildings = 0;
        if (buildingType == RobotType.DESIGN_SCHOOL) {
            numberOfBuildings = knownDesignSchools.size();
        } else if (buildingType == RobotType.FULFILLMENT_CENTER) {
            numberOfBuildings = knownFulfillmentCenters.size();
        }
        if (numberOfBuildings == 0 || isOnTurtle) {
            if (hqLoc != null && rc.getLocation().distanceSquaredTo(hqLoc) <= 40) {
                // Double check there are no buildings (if not on turtle)
                if (!isOnTurtle) {
                    RobotInfo[] robots = rc.senseNearbyRobots(9999, rc.getTeam());
                    for (RobotInfo robot : robots) {
                        if (robot.type == buildingType) {
                            if (buildingType == RobotType.REFINERY) {
                                knownDesignSchools.add(robot.location);      
                            } else if (buildingType == RobotType.FULFILLMENT_CENTER) {
                                knownFulfillmentCenters.add(robot.location);
                            }
                        }
                    }
                    if ((buildingType == RobotType.DESIGN_SCHOOL && knownDesignSchools.size() > 0) ||
                        (buildingType == RobotType.FULFILLMENT_CENTER && knownFulfillmentCenters.size() > 0)) {
                        return false;
                    }
                }

                System.out.println("I want to build a building! " + buildingType);
                Direction bestDir = null;
                for (Direction dir : directions) {
                    MapLocation loc = rc.getLocation().add(dir);
                    if (rc.canBuildRobot(buildingType, dir) && 
                            (loc.distanceSquaredTo(hqLoc) >= 8) &&
                            (bestDir == null || rc.senseElevation(loc) > rc.senseElevation(rc.adjacentLocation(bestDir))) &&
                            (!isOnTurtle || isSuitableLocationForBuilding(loc))) {
                        bestDir = dir;
                    }
                }
                if (bestDir != null && tryBuildInDir(buildingType, bestDir, true)) {
                    lastBuiltBuilding = rc.getRoundNum();
                    return true;
                } 
            }
        }
        return false;
    }

    public static int closestNetGun(RobotInfo[] robots, MapLocation loc) throws GameActionException {
        int closest = 9999;
        for (RobotInfo robot : robots) {
            if (robot.team == rc.getTeam() && (robot.type == RobotType.NET_GUN || robot.type == RobotType.HQ)) {
                int dis = loc.distanceSquaredTo(robot.getLocation());
                if (dis < closest) {
                    closest = dis;
                }
            }
        }
        for (MapLocation netGunLoc : knownNetGuns) {
        	int dis = loc.distanceSquaredTo(netGunLoc);
            if (dis < closest) {
                closest = dis;
            }
        }
        return closest;
    }

    public static int closestDesignSchool(RobotInfo[] robots, MapLocation loc) throws GameActionException {
        int closest = 9999;
        for (RobotInfo robot : robots) {
            if (robot.team == rc.getTeam() && (robot.type == RobotType.DESIGN_SCHOOL)) {
                int dis = loc.distanceSquaredTo(robot.getLocation());
                if (dis < closest) {
                    closest = dis;
                }
            }
        }
        for (MapLocation designSchoolLoc : knownDesignSchools) {
            int dis = loc.distanceSquaredTo(designSchoolLoc);
            if (dis < closest) {
                closest = dis;
            }
        }
        return closest;
    }

    public static boolean tryBuildNetGunIfScared() throws GameActionException {
        // if we can see an enemy drone, and there is no net gun (or hq) within r^2 9 of us
        // build a net gun
        RobotInfo[] robots = rc.senseNearbyRobots();
        int closestNetGun = 9999;
        int enemyDrones = 0;
        int requiredDistanceToNearestNetGun = 15;
        if (knownVaporators.size() < 5) {
            if (rc.getLocation().distanceSquaredTo(hqLoc) > 25) {
                requiredDistanceToNearestNetGun = 25;
            }
            if (rc.getLocation().distanceSquaredTo(hqLoc) > 50) {
                requiredDistanceToNearestNetGun = 40;
            }
            if (rc.getLocation().distanceSquaredTo(hqLoc) > 80) {
                requiredDistanceToNearestNetGun = 999999;
            }
        } else {
            // when we have enough vaporators, we can spend more
            requiredDistanceToNearestNetGun = 10;
            if (rc.getLocation().distanceSquaredTo(hqLoc) > 50) {
                requiredDistanceToNearestNetGun = 15;
            }
        }
        for (RobotInfo robot : robots) {
            if (robot.type == RobotType.DELIVERY_DRONE && robot.team != rc.getTeam()) {
                enemyDrones++;
            }
        }
        if (enemyDrones > 0) {
        	requiredDistanceToNearestNetGun/=enemyDrones;
        }
        if (requiredDistanceToNearestNetGun < 10) {
        	requiredDistanceToNearestNetGun = 10;
        }

        if (enemyDrones > 0 && closestNetGun(robots, rc.getLocation()) > requiredDistanceToNearestNetGun) {
            System.out.println("trying to build defensive net gun");
            for (Direction dir : directions) {
                if (closestNetGun(robots, rc.getLocation().add(dir)) > requiredDistanceToNearestNetGun && tryBuildInDir(RobotType.NET_GUN, dir, true)) {
                    System.out.println("built defensive net gun");
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean tryDoMinerNextToEnemyHq() throws GameActionException {
        // if no net guns, build one!
        if (rc.getLocation().distanceSquaredTo(enemyHqLoc) > 9) {
            return tryMoveTowards(enemyHqLoc);
        }
        RobotInfo[] robots = rc.senseNearbyRobots();
        int netGunDis = closestNetGun(robots, rc.getLocation());
        if (netGunDis > 20) {
            System.out.println("I want to build a net gun!");
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.NET_GUN, dir)) {
                    rc.buildRobot(RobotType.NET_GUN, dir);
                    return true;
                }
            }
        } 
        int designSchoolDis = closestDesignSchool(robots, rc.getLocation());
        if (designSchoolDis > 20) {
            System.out.println("I want to build a design school!");
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir)) {
                    rc.buildRobot(RobotType.DESIGN_SCHOOL, dir);
                    return true;
                }
            }
        }
        if (!isGoodSquareForMinerDrop(rc.getLocation()) || Math.min(netGunDis, designSchoolDis) < 20) {
            System.out.println("miner job complete - killing myself");
            rc.disintegrate();
        }
        return true;
    }

    public static boolean minerTryRunAwayFromDrone() throws GameActionException {
        // Miners are important (since its hard to produce more) and we *really* don't want to get eaten by an enemy drone
        // So if we are close to an enemy drone, run away
        if (rc.getRoundNum() > swarmRound) {
            return false;
        }
        RobotInfo[] robots = rc.senseNearbyRobots(25, rc.getTeam().opponent());
        boolean tooCloseToEnemyDrone = disToNearestDrone(rc.getLocation(), robots) <= 3;
        if (tooCloseToEnemyDrone) {
            Direction bestDir = null;
            int bestDis = -1;
            for (Direction dir : directions) {
                MapLocation loc = rc.getLocation().add(dir);
                if (rc.canSenseLocation(loc)) {
                    if (!rc.senseFlooding(loc) && rc.canMove(dir)) {
                        int dis = disToNearestDrone(loc, robots);
                        if (dis > bestDis) {
                            bestDis = dis;
                            bestDir = dir;
                        }
                    }
                }
            }
            if (bestDir != null) {
                if (rc.canMove(bestDir)) {
                    System.out.println("Moving away from drone " + bestDis);
                    rc.move(bestDir);
                    return true;
                } else {
                    drawError("unable to run away even after finding appropriate direction");
                }
            }
        }
        return false;
    }

}