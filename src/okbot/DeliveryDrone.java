package okbot;

import battlecode.common.*;

public class DeliveryDrone extends RobotPlayer {
    public static RobotInfo robotBeingCarried = null; // Used by drones

    public static void runDeliveryDrone() throws GameActionException {
        // TODO: implement delivery drone
        findHQ();

        readBlockchain(9000);

        doDeliveryDroneAction();

        readBlockchain(1500);
    }

    public static void doDeliveryDroneAction() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }

        // TODO: drop unit if more important unit appears
        // TODO: ensure delivery drone's movement takes into account the lack of obstacles, i.e. probably don't need full bugnav

        if (!rc.isCurrentlyHoldingUnit()) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            // TODO: choose between different robot types
            RobotInfo bestRobot = null;
            for (RobotInfo robot : robots) {
                if (robot.team == rc.getTeam() || robot.type.isBuilding() || robot.type == RobotType.DELIVERY_DRONE ||
                        (hqLoc != null && robot.location.distanceSquaredTo(hqLoc) >= 100)) {
                    continue;
                }
                if (bestRobot == null) {
                    bestRobot = robot;
                } else if (robot.team == rc.getTeam().opponent()) {
                    if (bestRobot.type == RobotType.COW ||
                            rc.getLocation().distanceSquaredTo(robot.location) < rc.getLocation().distanceSquaredTo(bestRobot.location)) {
                        bestRobot = robot;
                    }
                } else if (bestRobot.type == RobotType.COW &&
                        rc.getLocation().distanceSquaredTo(robot.location) < rc.getLocation().distanceSquaredTo(bestRobot.location)) {
                    bestRobot = robot;
                }
            }
            if (bestRobot != null) {
                if (rc.canPickUpUnit(bestRobot.ID)) {
                    rc.pickUpUnit(bestRobot.ID);
                    robotBeingCarried = bestRobot;
                    return;
                } else if (tryMoveTowards(bestRobot.location)) {
                    return;
                }
            }
        } else {
            // TODO: memorize flooded locations
            int sensorRadius = rc.getCurrentSensorRadiusSquared();
            // Cannot drop on same square so start from i = 1
            for (int i = 1; i < offsetDist.length; i++) {
                if (offsetDist[i] > sensorRadius) {
                    break;
                }
                MapLocation loc = rc.getLocation().translate(offsetX[i], offsetY[i]);
                if (onTheMap(loc) && rc.senseFlooding(loc)) {
                    Direction dir = rc.getLocation().directionTo(loc);
                    if (rc.adjacentLocation(dir).equals(loc)) {
                        rc.dropUnit(dir);
                        return;
                    } else if (tryMoveTowards(loc)) {
                        return;
                    }
                }
            }

            // Otherwise, if can't see water, just wander around
            if (tendency == null) {
                tendency = randomDirection();
            }
            if (tryMoveToTendency()) {
                return;
            }
        }

        if (hqLoc != null) {
            if (rc.getLocation().distanceSquaredTo(hqLoc) >= 36) {
                // Move towards HQ
                tryMoveTowards(hqLoc);
            } else if (rc.senseFlooding(rc.getLocation())) {
                // Once it starts flooding, surround the HQ so that enemy drones can't pick off friendly workers
                RobotInfo[] allies = rc.senseNearbyRobots(2, rc.getTeam());
                boolean dontmove = false;
                for (RobotInfo ally : allies) {
                    if (ally.type == RobotType.LANDSCAPER) {
                        dontmove = true;
                    }
                }
                // don't move if already next to a landscaper
                if (!dontmove) {
                    tryMoveTowards(hqLoc);
                }
            } else {
                // Stay near HQ
                for (int i = 0; i < 8; i++) {
                    if (rc.adjacentLocation(directions[i]).distanceSquaredTo(hqLoc) >= 36) {
                        tryMoveToTendencyForbidden[i] = true;
                    }
                }
                if (tendency == null) {
                    tendency = randomDirection();
                }
                tryMoveToTendency();
                for (int i = 0; i < 8; i++) {
                    tryMoveToTendencyForbidden[i] = false;
                }
            }
            return;
        }

        if (tendency == null) {
            tendency = randomDirection();
        }
        tryMoveToTendency();
    }
}
