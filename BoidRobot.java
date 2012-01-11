package team035;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class BoidRobot extends BaseRobot {

	public static final int DESIRED_MIN_SEPARATION = 36;
	
	public BoidRobot(RobotController myRc) {
		super(myRc);

	}

	@Override
	public void engage() {
		// TODO Auto-generated method stub
		while(true) {
			this.move();
			this.rc.yield();
		}
	}

	public void move() {
		
		// This implements some simple boid movement. There are three components:
		// 1. separation
		// 2. alignment
		// 3. cohesion
		
		// This is a little tricky to do in this context because alignment is 8 discrete options, not a float

		// get a list of all the nearby friendly units. this is limited to sensor range, and will work okay with archons
		// but we'll probably need to be doing something fancier with messaging to do this with other types
		// of robots who can't always see each other.
		
		// if we're moving, just wait until we're done with a move.
		if(this.rc.isMovementActive()) {
			return;
		}
		
		int detectedNeighbors = 0;
		
		Vector2d separation = new Vector2d();
		
		Robot[] nearbyRobots = this.rc.senseNearbyGameObjects(Robot.class);
		for(Robot r : nearbyRobots) {
			System.out.println("found item: " + r);
			if(r.getTeam()==this.rc.getTeam()) {
				detectedNeighbors++;
				// if it's a friendly robot, get its position.
				
				// we're basically going to calculate a vector, then normalize it and discretize
				// it to figure out what direction we should be facing.

				
				// separation vectors should be from the other nearby units, towards our unit. 
				try {
					RobotInfo friendlyRobotInfo = this.rc.senseRobotInfo(r);
					System.out.println("sensedRobotId: " + friendlyRobotInfo.location + " myLoc: " + this.rc.getLocation());
					double distance = friendlyRobotInfo.location.distanceSquaredTo(this.rc.getLocation());
					
					if(distance < DESIRED_MIN_SEPARATION) {
						// if they're within our desired separation distance, add in a vector that points away from
						// the friendly robot scaled with how close they are to us (closer is worse)

						Vector2d separationComponent = Vector2d.sub(this.rc.getLocation(), friendlyRobotInfo.location);

						separationComponent.normalize();
						
						// scale it so if they're really close (e.g. low distance) then we'll have a strong
						// desire to get away.
						separationComponent.scale(DESIRED_MIN_SEPARATION - distance);
						System.out.println("distance: " + distance + "; separationComponent: " + separationComponent);
						separation.add(separationComponent);
					}
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
				
		separation.normalize();

		// we want to see if we're pointed in the right direction. if we are, then move forward. if not, point
		// in the right direction and assume we'll need to go in that direction again next cycle and will move
		// at that point.
		Direction targetDirection = separation.getDirection();
		System.out.println("Desired direction: " + targetDirection + " (" + detectedNeighbors + " nearby)");

		if(targetDirection==Direction.NONE) {
			return;
		}

		if(this.rc.getDirection()==targetDirection) {
			try {
				// if we can't move the direction we want, just sit here. we're just boned and have to wait
				// for others to move away.
				if(this.rc.canMove(targetDirection)) {
					this.rc.moveForward();
				}
				return;

			} catch (GameActionException e) {
				e.printStackTrace();
			}
		} else {
			try {
				this.rc.setDirection(targetDirection);
			} catch (GameActionException e) {
				// I'm not sure what can go wrong with setDirection, but whatever...
				e.printStackTrace();
			}
			return;
		}
	}
}
