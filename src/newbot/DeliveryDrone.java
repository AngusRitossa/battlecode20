package newbot;

import battlecode.common.*;
import java.util.*;

public class DeliveryDrone extends RobotPlayer {

    public static void runDeliveryDrone() throws GameActionException {
    	readBlockchain(9000);
        doAction();
        readBlockchain(2000);
    }
    public static int hangAroundHQ = -1;
    public static int lateGamePickUpUnits = -1;
    public static int hangAroundDistanceFromHQ = 75;
    public static final int swarmRound = 2100;
    public static RobotInfo unitCarrying = null; // ensure this is updates when we pick up/drop a unit
    public static ArrayList<MapLocation> knownWater = new ArrayList<MapLocation>();
    public static void doAction() throws GameActionException {
        checkEnemyHQLocs();
    	if (hangAroundHQ == -1 && hqLoc != null) {
            hangAroundHQ = (int) (Math.random() * 420);
            hangAroundHQ += rc.getLocation().x + rc.getLocation().y + Clock.getBytecodesLeft();
            hangAroundHQ %= 3;
            if (hangAroundHQ != 0) {
                hangAroundHQ = 1;
            }
        }
        if (lateGamePickUpUnits == -1) {
            lateGamePickUpUnits = (int) (Math.random() * 420);
            lateGamePickUpUnits += rc.getLocation().x + rc.getLocation().y + Clock.getBytecodesLeft();
            lateGamePickUpUnits %= 3;
            if (lateGamePickUpUnits != 0) {
                lateGamePickUpUnits = 1;
            }
        }
        if (rc.getRoundNum() > water_level_round[lowerTurtleHeight]+50 && rc.getRoundNum() < swarmRound) {
        	if (hangAroundHQ == 0) {
        		hangAroundHQ = 1;
        	}
        	hangAroundDistanceFromHQ = 15;
        }
        if (!rc.isReady()) {
            return;
        }
        if (rc.isCurrentlyHoldingUnit()) {
        	if (unitCarrying.team != rc.getTeam()) {
        		if (tryDropUnitIntoWater()) {
        			return;
        		}
        	} else {
                if (unitCarrying.type == RobotType.MINER) {
                    if (tryDropUnitOntoTurtle()) {
                    return;
                    }
                    if (deliveryDroneTryMoveTowards(hqLoc)) {
                        return;
                    }
                } else if (unitCarrying.type == RobotType.LANDSCAPER) {
                    if (tryDropLandscaperNextToEnemyHq()) {
                        return;
                    }
                }
        	}
        	if (deliveryDroneTryMoveRandomly()) {
        		return;
        	}
        } else { 
        	if (tryPickUpUnit((hangAroundHQ == 1 && rc.getRoundNum() < water_level_round[lowerTurtleHeight]-250) ? hangAroundDistanceFromHQ : 99999)) {
        		return;
        	}
        	if (rc.getRoundNum() > swarmRound) {
        		hangAroundHQ = 0;
        		if (swarmEnemyHQ()) {
        			return;
        		}
        	}
        	if (hangAroundHQ == 1 && rc.getLocation().distanceSquaredTo(hqLoc) > hangAroundDistanceFromHQ) {
        		if (deliveryDroneTryMoveTowards(hqLoc)) {
        			return;
        		}
        	}
        	if (rc.getRoundNum() > 800 && tryMoveIntoTurtleGap()) {
        		return;
        	}
        	if (deliveryDroneTryMoveRandomly()) {
        		return;
        	}
        }
    }
    public static boolean tryPickUpUnit(int distanceFromHQ) throws GameActionException {
    	if (rc.isCurrentlyHoldingUnit()) {
    		return false;
    	}
    	// finds the closest enemy unit to try to pick up, or cows if enemies are further away 
    	// if we are defending the hq, doesn't stray too far from it
    	// prioritise saving our miners over killing enemies, bc our miners are important
    	RobotInfo robots[] = rc.senseNearbyRobots();
    	RobotInfo bestRobot = null;
    	for (RobotInfo robot: robots) {
    		if (robot.team == rc.getTeam() && robot.type == RobotType.MINER && rc.getRoundNum() > startTurtlingHQRound) {
    			// if our miner is not on the turtle: rescue it :)
    			if (rc.canSenseLocation(robot.getLocation()) && rc.senseElevation(robot.getLocation()) < lowerTurtleHeight) {
    				if (bestRobot != null && bestRobot.team == rc.getTeam()) {
    					if (bestRobot.getLocation().distanceSquaredTo(rc.getLocation()) > robot.getLocation().distanceSquaredTo(rc.getLocation())) {
    						bestRobot = robot;
    					}
    				} else {
    					bestRobot = robot;
    				}
    			}
    		}
            if (lateGamePickUpUnits != 1 && robot.team == rc.getTeam() && robot.type == RobotType.LANDSCAPER && 
                rc.getRoundNum() > water_level_round[lowerTurtleHeight]-100 && robot.getLocation().distanceSquaredTo(hqLoc) > 4 && 
                (enemyHqLoc == null || robot.getLocation().distanceSquaredTo(enemyHqLoc) > 4)) {
                // towards the end, pick up our landscapers, but not the ones defending the hq
                if (bestRobot == null || bestRobot.getLocation().distanceSquaredTo(rc.getLocation()) > robot.getLocation().distanceSquaredTo(rc.getLocation())) {
                    bestRobot = robot;
                }
            }
    		if (robot.team == rc.getTeam() || robot.type.isBuilding() || robot.type == RobotType.DELIVERY_DRONE || 
    			robot.getLocation().distanceSquaredTo(hqLoc) > distanceFromHQ ||
    			(bestRobot != null && bestRobot.team == rc.getTeam())) {
    			continue;
    		}
    		if (bestRobot == null || bestRobot.type == RobotType.COW || 
    			(robot.type != RobotType.COW && bestRobot.getLocation().distanceSquaredTo(rc.getLocation()) > robot.getLocation().distanceSquaredTo(rc.getLocation()))) {
    			bestRobot = robot;
    		} 
    	}
    	if (bestRobot != null) {
  			if (rc.getLocation().isAdjacentTo(bestRobot.getLocation())) {
  				if (rc.canPickUpUnit(bestRobot.getID())) {
    				rc.pickUpUnit(bestRobot.getID()); // om nom nom
    				unitCarrying = bestRobot;
    				System.out.println("om nom nommed a unit");
    				return true;
    			} else {
    				drawError("next to unit but can't pick up");
    			}
  			} else {
  				System.out.println("trying to move towards unit to pick up");
  				if (deliveryDroneTryMoveTowards(bestRobot.getLocation())) {
  					return true;
  				}
  			}
    	}
    	return false;
    }

    public static boolean tryMoveIntoTurtleGap() throws GameActionException {
    	if (rc.getRoundNum() > water_level_round[lowerTurtleHeight]+50) {
    		return true;
    	}
    	if (canBeDugForLowerTurtle(rc.getLocation())) {
    		return true; // didn't technically do an action, but we're already in a desired spot
    	}
    	boolean[] dangerousDir = new boolean[8];
        findAdjSquaresNearNetGuns(dangerousDir);
        for (int i = 0; i < directions.length; i++) {
        	if (dangerousDir[i]) {
        		continue;
        	}
    		Direction dir = directions[i];
    		MapLocation loc = rc.getLocation().add(dir);
    		if (rc.canSenseLocation(loc) && canBeDugForLowerTurtle(loc)) {
    			if (rc.canMove(dir)) {
    				System.out.println("Moving onto a gap in the turtle grid");
    				rc.move(dir);
    				return true;
    			}
    		}
    	}
    	return false;
    }

    public static boolean tryMoveTowardsWater() throws GameActionException {
        int sensorRadius = rc.getCurrentSensorRadiusSquared();
        for (int i = 0; i < offsetDist.length; i++) {
            if (offsetDist[i] > sensorRadius) {
                break;
            }
            MapLocation loc = rc.getLocation().translate(offsetX[i], offsetY[i]);
            if (rc.canSenseLocation(loc)) {
                if (rc.senseFlooding(loc)) {
                    System.out.println("moving towards water we can see");
                    return deliveryDroneTryMoveTowards(loc);
                }
            }
        }
        MapLocation nearestKnownWater = null;
        for (MapLocation loc : knownWater) {
            if (nearestKnownWater == null || rc.getLocation().distanceSquaredTo(nearestKnownWater) > rc.getLocation().distanceSquaredTo(loc)) {
                nearestKnownWater = loc;
            }
        }
        if (nearestKnownWater != null) {
            System.out.println("moving towards known water");
            return deliveryDroneTryMoveTowards(nearestKnownWater);
        }
        return false;
    }
    public static boolean tryDropUnitIntoWater() throws GameActionException {
    	// if we are above water, kills whatever we are carrying
    	// assumes we are carying a unit, and we wish death for said unit
    	for (Direction dir : directions) {
    		MapLocation loc = rc.getLocation().add(dir);
    		if (rc.canSenseLocation(loc) && rc.senseFlooding(loc)) {
    			if (rc.canDropUnit(dir)) {
    				rc.dropUnit(dir);
    				unitCarrying = null;
    				System.out.println("Dropping unit into water");
                    if (!knownWater.contains(loc)) {
                        knownWater.add(loc);
                    }
    				return true;
    			}
    		}
    	}
    	return tryMoveTowardsWater();
    }

    public static boolean tryDropUnitOntoTurtle() throws GameActionException {
    	// checks that we are close enough to HQ (75) and that the height is the height of the hq turtle
    	// check there are no nearby enemy drones
    	RobotInfo[] robots = rc.senseNearbyRobots(15, rc.getTeam().opponent());
    	for (RobotInfo robot : robots) {
    		if (robot.type == RobotType.DELIVERY_DRONE) {
    			return false; // don't drop miners if theres an enemy drone nearby
    		}
    	}
    	for (Direction dir : directions) {
    		MapLocation loc = rc.getLocation().add(dir);
    		if (rc.canSenseLocation(loc)) {
    			if (loc.distanceSquaredTo(hqLoc) <= 75 && rc.senseElevation(loc) == lowerTurtleHeight) {
    				if (rc.canDropUnit(dir)) {
    					rc.dropUnit(dir);
    					return true;
    				}
    			} 
    		}
    	}
    	return false;
    }

    public static boolean tryDropLandscaperNextToEnemyHq() throws GameActionException {
        if (enemyHqLoc == null) {
            return false;
        } 
        if (rc.getLocation().isAdjacentTo(enemyHqLoc)) {
            rc.disintegrate();
        }
        for (Direction dir : directions) {
            MapLocation loc = rc.getLocation().add(dir);
            if (rc.canSenseLocation(loc) && loc.isAdjacentTo(enemyHqLoc)) {
                if (rc.canDropUnit(dir)) {
                    rc.dropUnit(dir);
                    System.out.println("Dropping enemy landscaper onto hq");
                    return true;
                }
            }
        }
        if (rc.getRoundNum() < swarmRound+20) {
            return deliveryDroneTryMoveTowards(hqLoc);
        } else {
            return swarmEnemyHQ();
        }
    }

    public static void findAdjSquaresNearNetGuns(boolean[] dangerousDir) throws GameActionException {
        if (rc.getRoundNum() > swarmRound) {
            // yolo
            return;

        }
    	// dangerous dir must be an array of size 8
    	// if arr[i] is true, that means directions[i] is in range of a net gun
        ArrayList<MapLocation> nearbyNetGuns = new ArrayList<MapLocation>();
        if (enemyHqLoc != null) nearbyNetGuns.add(enemyHqLoc);
        RobotInfo robots[] = rc.senseNearbyRobots(9999, rc.getTeam().opponent());
        for (RobotInfo robot : robots) {
        	if (robot.type == RobotType.NET_GUN || robot.type == RobotType.HQ) {
        		nearbyNetGuns.add(robot.getLocation());
        	}
        }
        for (int i = 0; i < directions.length; i++) {
        	MapLocation adjLoc = rc.getLocation().add(directions[i]);
        	for (MapLocation netGunLoc : nearbyNetGuns) {
        		if (netGunLoc.distanceSquaredTo(adjLoc) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
        			dangerousDir[i] = true;
        			break;
        		}
        	}
        }
    }
    public static boolean deliveryDroneTryMoveTowards(MapLocation loc) throws GameActionException {

        // This is the tryMoveTowards that all other units use
        // Which is from okbot
        // Except I've modified it to not care about elevation, water, or enemy drones

        // Stores whether there is an enemy drone adjacent to this dir

        boolean[] dangerousDir = new boolean[8];
        findAdjSquaresNearNetGuns(dangerousDir);

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
                if (rc.onTheMap(next) && !dangerousDir[dir] && rc.canMove(directions[dir])) {
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

    public static boolean deliveryDroneTryMoveRandomly() throws GameActionException {
        // Picks a random square on the board and moves towards it
        if (randomSquareMovingTowards == null || rc.getRoundNum() - turnStartedLastMovement >= 50 || rc.getLocation().distanceSquaredTo(randomSquareMovingTowards) < 20) {
            int x = (int) (Math.random() * rc.getMapWidth());
            int y = (int) (Math.random() * rc.getMapHeight());
            randomSquareMovingTowards = new MapLocation(x, y);
            turnStartedLastMovement = rc.getRoundNum();
        }
        System.out.println("Moving randomly towards: " + randomSquareMovingTowards.x + " " + randomSquareMovingTowards.y);
        return deliveryDroneTryMoveTowards(randomSquareMovingTowards);
    }

    public static boolean swarmEnemyHQ() throws GameActionException {
    	if (enemyHqLoc == null && possibleEnemyHQLocs.size() == 0) {
    		return false;
    	}
        if (enemyHqLoc == null) {
            deliveryDroneTryMoveTowards(possibleEnemyHQLocs.get(0));
        } else {
            deliveryDroneTryMoveTowards(enemyHqLoc);
        }   
    	return true;
    }
}
