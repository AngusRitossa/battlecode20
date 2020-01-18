package newbot;

import battlecode.common.*;
import java.util.*;

public class Miner extends RobotPlayer {
    public static void runMiner() throws GameActionException {
    	readBlockchain(9000);
    	findHQ();
        doAction();
        readBlockchain(1000);
    }
    public static int hangAroundHQ = -1;
    public static void doAction() throws GameActionException {
        updateKnownRefineries();
        updateKnownDesignSchools();
        if (!rc.isReady()) {
            return;
        }
        if (rc.getRoundNum() >= 400 && tryBeTurtleMiner()) {
            return;
        }
        if (tryBuildDesignSchool()) {
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

    public static boolean tryBeTurtleMiner() throws GameActionException {
        // If we're not on the turtle, try to get on it
        // Otherwise, build a refinery or move randomly
        if (hqLoc == null) {
            return false;
        }
        // Miners who hang around on the turtle to build the necessary buildings
        if (rc.senseElevation(rc.getLocation()) < lowerTurtleHeight) {
            // walk towards hq
            System.out.println("Trying to get onto turtle");
            return tryMoveTowards(hqLoc);
        }
        if (tryBuildVaporator()) {
            return true;
        }
        return false;
    }

    public static boolean isSuitableLocationForVaporator(MapLocation loc) throws GameActionException {
        // Currently just checks elevation == lowerTurtleHeight && does not contain an adjacent building
        if (!rc.canSenseLocation(loc)) {
            return false;
        }
        if (rc.senseElevation(loc) != lowerTurtleHeight) {
            return false;
        }
        for (Direction dir : directions) {
            MapLocation adjLoc = loc.add(dir);
            if (rc.canSenseLocation(adjLoc)) {
                RobotInfo robot = rc.senseRobotAtLocation(adjLoc);
                if (robot != null && robot.type.isBuilding()) {
                    return false;
                }
            }
        }
        return true;
    }
    public static boolean tryBuildVaporator() throws GameActionException {
        // TODO: Be smart?
        for (Direction dir : directions) {
            MapLocation loc = rc.getLocation().add(dir);
            if (isSuitableLocationForVaporator(loc)) {
                if (tryBuildInDir(RobotType.VAPORATOR, dir, true)) {
                    return true;
                }
            }
        }   
        return false;
    }
    public static boolean tryMineSoup() throws GameActionException {
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

    public static void updateKnownRefineries() throws GameActionException {
    	for (int i = knownRefineries.size() - 1; i >= 0; i--) {
            if (rc.canSenseLocation(knownRefineries.get(i))) {
               	MapLocation loc = knownRefineries.get(i);
               	RobotInfo robot = rc.senseRobotAtLocation(loc);
               	if (robot == null || robot.team != rc.getTeam() || robot.type != RobotType.REFINERY) {
               		knownRefineries.remove(i);
               		knownRefineriesWithSoup.remove(loc);
               		// send message proclaiming the death
               		int[] message = new int[7];
                    message[0] = MESSAGE_TYPE_REFINERY_IS_DEAD;
                    message[1] = loc.x * MAX_MAP_SIZE + loc.y;
                    sendBlockchain(message, 1);
               	}
            }
        }
    }

    public static void updateKnownDesignSchools() throws GameActionException {
        for (int i = knownDesignSchools.size() - 1; i >= 0; i--) {
            if (rc.canSenseLocation(knownDesignSchools.get(i))) {
                MapLocation loc = knownDesignSchools.get(i);
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot == null || robot.team != rc.getTeam() || robot.type != RobotType.DESIGN_SCHOOL) {
                    knownDesignSchools.remove(i);
                    // send message proclaiming the death
                    int[] message = new int[7];
                    message[0] = MESSAGE_TYPE_DESIGN_SCHOOL_IS_DEAD;
                    message[1] = loc.x * MAX_MAP_SIZE + loc.y;
                    sendBlockchain(message, 1);
                }
            }
        }
    }

    public static boolean tryBuildDesignSchool() throws GameActionException {
        // Currently, near (but >= distance 9) to the HQ will build a design school
        if (knownDesignSchools.size() == 0) {
            if (hqLoc != null && rc.getLocation().distanceSquaredTo(hqLoc) <= 40) {
                // Double check there are no nearby design schools
                RobotInfo[] robots = rc.senseNearbyRobots(9999, rc.getTeam());
                for (RobotInfo robot : robots) {
                    if (robot.type == RobotType.DESIGN_SCHOOL) {
                        knownDesignSchools.add(robot.location);      
                        return false;
                    }
                }

                System.out.println("I want to build a design school!");
                Direction bestDir = null;
                for (Direction dir : directions) {
                    if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir) && rc.adjacentLocation(dir).distanceSquaredTo(hqLoc) >= 16 &&
                            (bestDir == null || rc.senseElevation(rc.adjacentLocation(dir)) > rc.senseElevation(rc.adjacentLocation(bestDir))) &&
                            !canBeDugForLowerTurtle(rc.adjacentLocation(dir))) {
                        bestDir = dir;
                    }
                }
                if (bestDir != null && tryBuildInDir(RobotType.DESIGN_SCHOOL, bestDir, true)) {
                    return true;
                }
            }
        }
        return false;
    }

}