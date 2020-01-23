package newbot;

import battlecode.common.*;

public class Landscaper extends RobotPlayer {
    public static void runLandscaper() throws GameActionException {
        readBlockchain(9000);
        doAction();
        readBlockchain(2000);
    }
    public static final int landscaperBeginTurtleRound = 300;
    public static void doAction() throws GameActionException {
        updateAdjHQSquares();
        checkEnemyHQLocs();
        if (!rc.isReady()) {
            return;
        }
        if (lastRoundBuiltTurtle == 9999) {
            lastRoundBuiltTurtle = rc.getRoundNum();
        }
        if (rc.getRoundNum() > landscaperBeginTurtleRound) {
        	if (tryAttackOrDefendBuilding(true)) {
            	return;
        	}
        	if (rc.getRoundNum() > 800 && tryFormHQTurtle()) {
            	return;
	        }
	        if (tryMakeLowerTurtle()) {
	            return;
	        }
        } else {
        	if (tryBeEarlyGameLandscaper()) {
        		return;
        	}
        }
        if (tryMoveRandomly()) {
            return;
        }
        System.out.println("I'm not doing anything, how sad");
    }

    public static boolean tryBeEarlyGameLandscaper() throws GameActionException {
    	if (tryAttackOrDefendBuilding(true)) {
        	return true;
    	}
    	if (rc.getLocation().distanceSquaredTo(hqLoc) > 15) {
    		return tryMoveTowards(hqLoc);
    	}
    	if (trySaveUpDirt()) {
    		return true;
    	}
    	return false;
    }
    public static boolean tryAttackOrDefendBuilding(boolean move) throws GameActionException {
    	// move is whether or not we want to move towards a building to attack/defend
    	// false if we are next to hq and want to keep our ground
    	// First, check if HQ is being buried
        if (hqLoc != null && rc.getLocation().isAdjacentTo(hqLoc) && rc.canSenseLocation(hqLoc)) {
            RobotInfo robot = rc.senseRobotAtLocation(hqLoc);
            if (robot.getDirtCarrying() > 0) {
                Direction dir = rc.getLocation().directionTo(hqLoc);
                if (rc.canDigDirt(dir)) {
                    System.out.println("Digging dirt off HQ");
                    rc.digDirt(dir);
                    return true;
                }
            }
        }
        // For all nearby buildings, find the 'most important' to attack/defend, and do that
        // prioritises defence over attacking
        RobotInfo bestBuilding = null;
        int disToBestBuilding = 9999;

        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo robot : robots) {
        	if (robot.type.isBuilding()) {
        		int dis = rc.getLocation().distanceSquaredTo(robot.getLocation());
        		if (robot.team == rc.getTeam()) {
        			if (robot.getDirtCarrying() > 0) {
        				if (bestBuilding == null || bestBuilding.team != rc.getTeam() || dis < disToBestBuilding) {
        					bestBuilding = robot;
        					disToBestBuilding = dis;
        				}
        			}
        		} else {
        			if ((bestBuilding == null || bestBuilding.team != rc.getTeam()) && dis < disToBestBuilding) {
        				bestBuilding = robot;
        				disToBestBuilding = dis;
        			}
        		}
        	}
        }
        if (bestBuilding != null) {
        	if (rc.getLocation().isAdjacentTo(bestBuilding.getLocation())) {
        		if (bestBuilding.getTeam() == rc.getTeam()) {
        			if (rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit) {
        				// offload dirt
        				for (Direction dir : directions) {
        					MapLocation loc = rc.getLocation().add(dir);
        					if (rc.canSenseLocation(loc)) {
        						RobotInfo robot = rc.senseRobotAtLocation(loc);
        						if (robot == null || !robot.type.isBuilding() || robot.team != rc.getTeam()) {
        							if (rc.canDepositDirt(dir)) {
        								rc.depositDirt(dir);
        								System.out.println("depositing dirt bc we are full");
        								return true;
        							}
        						}
        					}
        				}
        			} else {
        				Direction dir = rc.getLocation().directionTo(bestBuilding.getLocation());
        				if (rc.canDigDirt(dir)) {
        					rc.digDirt(dir);
        					System.out.println("digging dirt off our building");
        					return true;
        				}
        			}
        		} else {
        			// not our team
        			if (rc.getDirtCarrying() == 0) {
        				// dig dirt to kill the enemy
						return tryDigDirtForAttack();
        			} else {
        				Direction dir = rc.getLocation().directionTo(bestBuilding.getLocation());
        				if (rc.canDepositDirt(dir)) {
        					rc.depositDirt(dir);
        					System.out.println("depositing dirt on enemy building");
        					return true;
        				}
        			}
        		} 
        	} else if (move) { 
    			System.out.println("moving towards building to attack or defend");
    			return tryMoveTowards(bestBuilding.getLocation());
        	}
        }
        return false;
    }
    public static boolean tryDefendBuilding() throws GameActionException {
        // First, check if HQ is being buried
        if (hqLoc != null && rc.getLocation().isAdjacentTo(hqLoc) && rc.canSenseLocation(hqLoc)) {
            RobotInfo robot = rc.senseRobotAtLocation(hqLoc);
            if (robot.getDirtCarrying() > 0) {
                Direction dir = rc.getLocation().directionTo(hqLoc);
                if (rc.canDigDirt(dir)) {
                    System.out.println("Digging dirt off HQ");
                    rc.digDirt(dir);
                    return true;
                }
            }
        }
        for (Direction dir : directions) {
            MapLocation loc = rc.getLocation().add(dir);
            if (rc.canSenseLocation(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot != null && robot.team == rc.getTeam() && robot.type.isBuilding()) {
                    if (robot.getDirtCarrying() > 0) {
                        if (rc.canDigDirt(dir)) {
                            System.out.println("Digging dirt off building (not HQ)");
                            rc.digDirt(dir);
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;   
    }

    public static boolean tryAttackBuilding() throws GameActionException {
        if (rc.getDirtCarrying() == 0) {
            return false;
        }
        Direction attackDir = null;
        for (Direction dir : directions) {
            MapLocation loc = rc.getLocation().add(dir);
            if (rc.canSenseLocation(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot != null && robot.team != rc.getTeam() && robot.type.isBuilding()) {
                    // Prioritise the hq
                    if (robot.type == RobotType.HQ) {
                        if (rc.canDepositDirt(dir)) {
                            System.out.println("Depositing dirt on enemy HQ");
                            rc.depositDirt(dir);
                            return true;
                        }
                    } else {
                        attackDir = dir;
                    }
                }
            }
        }
        if (attackDir != null && rc.canDepositDirt(attackDir)) {
            System.out.println("Depositing dirt on enemy building (not HQ)");
            rc.depositDirt(attackDir);
            return true;
        }
        return false;
    }

    public static int firstTurnMovingTowardHQLoc = 9999;
    public static boolean tryFormHQTurtle() throws GameActionException {
        if (hqLoc == null) {
            return false;
        }
        if (rc.getLocation().isAdjacentTo(hqLoc)) {
            if (tryMakeTurtleAround(hqLoc)) {
                return true;
            }
            if (tryDigDirtForTurtle()) {
                return true;
            }
            return false;
        }
        if (noFreeSquaresAdjHQ()) {
            return false;
        }
        System.out.println("Trying to move towards hq loc");
        if (firstTurnMovingTowardHQLoc == 9999) {
            firstTurnMovingTowardHQLoc = rc.getRoundNum();
        }
        if (rc.getRoundNum() > firstTurnMovingTowardHQLoc+30) {
            System.out.println("gave up moving towards hq loc");
            return false;
        }
        return tryMoveTowards(hqLoc);
    }

    public static boolean tryMakeTurtleAround(MapLocation turtleLoc) throws GameActionException {
        // We should be adjacent to loc whenever this is called
        // Finds the lowest square on the perimeter of the loc that we are adjacent to, and adds dirt to it
        if (rc.getDirtCarrying() == 0) {
            return false;
        }
        MapLocation bestLoc = rc.getLocation();

        
        int lowestElevation = rc.senseElevation(rc.getLocation());
        for (Direction dir : directions) {
            MapLocation loc = turtleLoc.add(dir);
            if (rc.getLocation().isAdjacentTo(loc) && rc.canSenseLocation(loc)) {
                // Conditions for not turtling below us
                // height of this square is the lowest so far
                // - height of adj is < 0
                // - height of adj will be underwater in 5 turns
                // - round is > 1000 and it doesn't have a miner, or round > 1500 regardless of miner
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                boolean hasMiner = false; // don't raise the square if it has a miner on it
                if (robot != null && robot.team == rc.getTeam() && robot.type == RobotType.MINER) {
                    hasMiner = true;
                }
                if (rc.senseElevation(loc) < lowestElevation && 
                    (rc.senseElevation(loc) < 0 || (rc.getRoundNum() > 1500 && !hasMiner) ||
                    (rc.senseElevation(loc) < lowerTurtleHeight && rc.getRoundNum() > startTurtlingHQRound) ||
                    (rc.senseElevation(loc) <= 99 && water_level_round[rc.senseElevation(loc)] <= rc.getRoundNum()+5))) {
                    lowestElevation = rc.senseElevation(loc);
                    bestLoc = loc;
                }
            }
        }
        
        Direction dir = rc.getLocation().directionTo(bestLoc);
        if (rc.canDepositDirt(dir)) {
            rc.depositDirt(dir);
            System.out.println("Depositing dirt to form turtle");
            return true;
        }
        
        return false;
    }

    public static boolean tryDigDirtForTurtle() throws GameActionException {
    	// dig dirt from an allowed location in the turtle
        for (Direction dir : directions) {
            MapLocation loc = rc.getLocation().add(dir);
            if (canBeDugForLowerTurtle(loc) && rc.canDigDirt(dir)) {
                System.out.println("Digging dirt for turtle");
                rc.digDirt(dir);
                return true;
            }
        }
        return false;
    }
    public static boolean tryDigDirtForAttack() throws GameActionException {
    	// dig dirt from any adjacent square, so long as it doesn't result in us saving an enemy building
    	for (Direction dir : directions) {
    		MapLocation loc = rc.getLocation().add(dir);
    		if (rc.canDigDirt(dir) && rc.canSenseLocation(loc)) {
	        	// Don't dig dirt off an enemy building!
	        	RobotInfo robot = rc.senseRobotAtLocation(loc);
	        	if (robot == null || robot.team == rc.getTeam() || !robot.type.isBuilding()) {
	        		System.out.println("Digging dirt for attack");
	            	rc.digDirt(dir);
	            	return true;
	        	}
	        }
    	}
    	return false;
    }

    public static int adjHQSquaresClaimed[] = new int[8]; // stores for each square adjacent to the hq, if it has a unit on it
    public static final int sizeOfLowerTurtle = 50; // d^2 around hq to make lower turtle
    public static boolean noFreeSquaresAdjHQ() throws GameActionException {
        for (int i = 0; i < adjHQSquaresClaimed.length; i++) {
            if (adjHQSquaresClaimed[i] == 1) {
                return false;
            }
        }
        return true;
    }
    public static void updateAdjHQSquares() throws GameActionException {
        if (hqLoc == null) {
            return;
        }
        for (int i = 0; i < directions.length; i++) {
            MapLocation loc = hqLoc.add(directions[i]);
            if (rc.canSenseLocation(loc)) {
                // Conditions for square adj HQ being claimed: 
                // - > 3 height above us
                // - has a unit on it (note this doesn't check for unit type or team)
                if (rc.senseElevation(loc)-3 > rc.senseElevation(rc.getLocation())
                    || rc.senseRobotAtLocation(loc) != null) {
                    adjHQSquaresClaimed[i] = 2;
                } else {
                    adjHQSquaresClaimed[i] = 1;
                }
            }
        }
    }
    public static boolean shouldBeRaisedForLowerTurtle(MapLocation loc) throws GameActionException {
        // Checks that the square isn't too low (if its too low, just give up on it)
        // Important: Assumes we can sense the location
        if ((rc.senseElevation(loc) < -20 && !loc.isAdjacentTo(hqLoc)) || rc.senseElevation(loc) >= lowerTurtleHeight || canBeDugForLowerTurtle(loc)) {
            return false;
        }
        RobotInfo robot = rc.senseRobotAtLocation(loc);
        if (robot != null && robot.team == rc.getTeam() && robot.type.isBuilding()) {
            return false;
        }
        return true;
    }

    public static int lastRoundBuiltTurtle = 9999; // If a landscaper is unable to move to the square it wants to raise it can basically say fuck it and just raises the nearest square it can
    public static int lastRoundGaveUpBuildingTurtleProperly = -999; // If a landscaper is unable to move to the square it wants to raise it can basically say fuck it and just raises the nearest square it can
    public static boolean tryMakeLowerTurtle() throws GameActionException {
        if (canBeDugForLowerTurtle(rc.getLocation())) {
            return false; // we really don't want to trap ourselves
        }
        if (rc.getDirtCarrying() == 0) {
            // Dig for the lower turtle
            return tryDigDirtForTurtle();
        } else {
            // Find lowest square we can see that needs raising - raise it or walk towards it
            int sensorRadius = rc.getCurrentSensorRadiusSquared();
            MapLocation bestLoc = null;
            if (rc.getRoundNum() >= water_level_round[lowerTurtleHeight]-2) {
                // fuck it, just save ourself
                bestLoc = rc.getLocation();
            } else {
                if (tryHelpMinerJoinTurtle()) {
                    return true;
                }

                for (int i = 0; i < offsetDist.length; i++) {
                    if (offsetDist[i] > sensorRadius) {
                        break;
                    }
                    MapLocation loc = rc.getLocation().translate(offsetX[i], offsetY[i]);
                    if (rc.onTheMap(loc) && shouldBeRaisedForLowerTurtle(loc)) {
                        if (loc.isAdjacentTo(hqLoc) && rc.senseElevation(loc) < 0) {
                            bestLoc = loc;
                            break;
                        }
                        if (bestLoc == null || 
                            (hqLoc.distanceSquaredTo(bestLoc) > hqLoc.distanceSquaredTo(loc))) {
                            bestLoc = loc;
                            if (rc.getRoundNum() > lastRoundBuiltTurtle+15) {
                                // Fuck it, just do this square
                                System.out.println("Can't reach desired square when building turtle");
                                lastRoundGaveUpBuildingTurtleProperly = rc.getRoundNum();
                                break;
                            }
                            if (lastRoundGaveUpBuildingTurtleProperly+30 > rc.getRoundNum()) {
                                break;
                            }
                        }
                    }
                }
            }
            if (bestLoc != null) {
                if (rc.getLocation().isAdjacentTo(bestLoc)) {
                    Direction dir = rc.getLocation().directionTo(bestLoc);
                    if (rc.canDepositDirt(dir)) {
                        rc.depositDirt(dir);
                        lastRoundBuiltTurtle = rc.getRoundNum();
                        System.out.println("depositing dirt on lower turtle");
                        return true;
                    }
                } else {
                    System.out.println("moving towards bestloc " + Clock.getBytecodesLeft());
                    tryMoveTowards(bestLoc);
                    return true;
                }
            } 
        }
        return false;
    }

    public static boolean tryHelpMinerJoinTurtle() throws GameActionException {
        for (Direction dir : directions) {
            MapLocation loc = rc.getLocation().add(dir);
            if (rc.canSenseLocation(loc)) {
                if (rc.senseElevation(loc) < lowerTurtleHeight) {
                    // check if a miner is on here
                    RobotInfo robot = rc.senseRobotAtLocation(loc);
                    if (robot != null && robot.type == RobotType.MINER && robot.team == rc.getTeam()) {
                        System.out.println("helping miner join turtle");
                        rc.depositDirt(dir);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static final int saveDirtLimit = 15;
    public static boolean trySaveUpDirt() throws GameActionException {
    	if (rc.getDirtCarrying() > 15) {
    		return false;
    	}
    	System.out.println("digging dirt bc I have nothing else to do");
    	return tryDigDirtForTurtle();
    }
}
