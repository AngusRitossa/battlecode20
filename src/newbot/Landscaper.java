package newbot;

import battlecode.common.*;

public class Landscaper extends RobotPlayer {
    public static void runLandscaper() throws GameActionException {
        readBlockchain(9000);
        doAction();
        readBlockchain(2000);
    }

    public static void doAction() throws GameActionException {
        updateAdjHQSquares();
        if (!rc.isReady()) {
            return;
        }
        if (lastRoundBuiltTurtle == 9999) {
            System.out.println("I am here yay");
            lastRoundBuiltTurtle = rc.getRoundNum();
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
        if (tryMakeLowerTurtle()) {
            return;
        }
        if (tryMoveRandomly()) {
            return;
        }
        System.out.println("I'm not doing anything, how sad");
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
        if (noFreeSquaresAdjHQ()) {
            return false;
        }
        System.out.println("Trying to move towards hq loc");
        tryMoveTowards(hqLoc);
        return true;
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
        if (rc.senseElevation(loc) < -20 || rc.senseElevation(loc) >= lowerTurtleHeight || canBeDugForLowerTurtle(loc)) {
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
        if (rc.getDirtCarrying() == 0) {
            // Dig for the lower turtle
            for (Direction dir : directions) {
                MapLocation loc = rc.getLocation().add(dir);
                if (canBeDugForLowerTurtle(loc) && rc.canDigDirt(dir)) {
                    System.out.println("Digging dirt to form lower turtle");
                    rc.digDirt(dir);
                    return true;
                }
            }
            return false;
        } else {
            // Find lowest square we can see that needs raising - raise it or walk towards it
            int sensorRadius = rc.getCurrentSensorRadiusSquared();
            MapLocation bestLoc = null;
            if (rc.getRoundNum() >= water_level_round[lowerTurtleHeight]-2) {
                // fuck it, just save ourself
                bestLoc = rc.getLocation();
            } else {
                for (int i = 0; i < offsetDist.length; i++) {
                    if (offsetDist[i] > sensorRadius) {
                        break;
                    }
                    MapLocation loc = rc.getLocation().translate(offsetX[i], offsetY[i]);
                    System.out.println(Clock.getBytecodesLeft());
                    if (rc.onTheMap(loc) && shouldBeRaisedForLowerTurtle(loc)) {
                        if (bestLoc == null || 
                            (hqLoc.distanceSquaredTo(bestLoc) > hqLoc.distanceSquaredTo(loc))) {
                            bestLoc = loc;
                            if (rc.getRoundNum() > lastRoundBuiltTurtle+15) {
                                // Fuck it, just do this square
                                System.out.println("Can't reach desired square when building turtle");
                                lastRoundGaveUpBuildingTurtleProperly = rc.getRoundNum();
                                break;
                            }
                            if (lastRoundGaveUpBuildingTurtleProperly+15 > rc.getRoundNum()) {
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
}
