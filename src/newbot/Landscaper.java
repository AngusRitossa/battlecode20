package newbot;

import battlecode.common.*;

public class Landscaper extends RobotPlayer {
    public static void runLandscaper() throws GameActionException {
        readBlockchain(9000);
    	doAction();
    	readBlockchain(1000);
    }

    public static void doAction() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
        if (tryDefendBuilding()) {
        	return;
        }
        if (tryAttackBuilding()) {
        	return;
        }
        if (tryFormHQTurtle()) {
        	return;
        }
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

    public static boolean tryFormHQTurtle() throws GameActionException {
    	if (hqLoc == null) {
    		return false;
    	}
    	if (rc.getLocation().isAdjacentTo(hqLoc)) {
    		if (tryMakeTurtleAround(hqLoc)) {
    			return true;
    		}
    		if (tryDigDirtForTurtle(hqLoc)) {
    			return true;
    		}

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
				// - round is > 1000

				if (rc.senseElevation(loc) < lowestElevation && 
					(rc.senseElevation(loc) < 0 || rc.getRoundNum() > 1000 || 
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

    public static final int[] turtleDigOffsetX    = { -2, 2, 0, 0 };
    public static final int[] turtleDigOffsetY    = { 0, 0, -2, 2 };
    public static boolean tryDigDirtForTurtle(MapLocation turtleLoc) throws GameActionException {
    	// Dig dirt from one of 4 squares near the HQ (turtleDigOffsetX, turtleDigOffsetY)
    	for (int i = 0; i < turtleDigOffsetX.length; i++) {
    		MapLocation loc = turtleLoc.translate(turtleDigOffsetX[i], turtleDigOffsetY[i]);
    		if (rc.getLocation().isAdjacentTo(loc) && rc.onTheMap(loc)) {
    			Direction dir = rc.getLocation().directionTo(loc);
    			if (rc.canDigDirt(dir)) {
    				rc.digDirt(dir);
    				System.out.println("Digging dirt to form turtle");
    				return true;
    			}
    		}
    	}
    	return false;
    }
}
