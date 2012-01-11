package team035;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class BoidRobot extends BaseRobot {

	public static final int DESIRED_MIN_SEPARATION = 6;
	
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
		Vector2d cohesion = new Vector2d();
		Vector2d alignment = new Vector2d();
		
		Vector2d averageNeighborPosition = new Vector2d();
		
		Robot[] nearbyRobots = this.rc.senseNearbyGameObjects(Robot.class);
		for(Robot r : nearbyRobots) {
			if(r.getTeam()==this.rc.getTeam()) {
				detectedNeighbors++;
				// if it's a friendly robot, get its position.
				
				// we're basically going to calculate a vector, then normalize it and discretize
				// it to figure out what direction we should be facing.

				
				// separation vectors should be from the other nearby units, towards our unit. 
				try {
					RobotInfo friendlyRobotInfo = this.rc.senseRobotInfo(r);
//					System.out.println("sensedRobotId: " + friendlyRobotInfo.location + " myLoc: " + this.rc.getLocation());
					double distance = friendlyRobotInfo.location.distanceSquaredTo(this.rc.getLocation());
					
					// -------------- SEPARATION ---------------- //
					if(distance < DESIRED_MIN_SEPARATION) {
						// if they're within our desired separation distance, add in a vector that points away from
						// the friendly robot scaled with how close they are to us (closer is worse)

						Vector2d separationComponent = Vector2d.sub(this.rc.getLocation(), friendlyRobotInfo.location);

						separationComponent.normalize();
						
						// scale it so if they're really close (e.g. low distance) then we'll have a strong
						// desire to get away.
						separationComponent.scale(DESIRED_MIN_SEPARATION - distance);
//						System.out.println("distance: " + distance + "; separationComponent: " + separationComponent);
						separation.add(separationComponent);
					}
					
					
					// ----------------- COHESION ---------------- //
					
					// we want to average the positions of all the people we see. add the up, divide by the count.
					averageNeighborPosition.add(new Vector2d(friendlyRobotInfo.location));
					System.out.println("averagePos: " + averageNeighborPosition);
					
					// ----------------- ALIGNMENT --------------- //
					
					
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				
				

			}
		}
		
		if(nearbyRobots.length > 0) {
			averageNeighborPosition.scale(1.0/nearbyRobots.length);
		} else {
			// if we don't have any neighbors, there's no cohesion factor
			averageNeighborPosition = new Vector2d(this.rc.getLocation());
		}
		
		System.out.println("myPos: " + this.rc.getLocation() + "; neighborPos: " + averageNeighborPosition);
		// now we can figure out the cohesion factor by getting a vector from our current
		// position to there.
		cohesion = Vector2d.sub(averageNeighborPosition, new Vector2d(this.rc.getLocation()));
		cohesion.scale(1.0);
//		separation.normalize();
		
		System.out.println("---- sep: " + separation + "; cohesion: " +cohesion);
		
		Vector2d finalVector = separation;
		finalVector.add(cohesion);
		
		// add some downward pressure.
		finalVector.add(new Vector2d(0, 2));
		
		
//		finalVector.add(alignment);
		
		// we want to see if we're pointed in the right direction. if we are, then move forward. if not, point
		// in the right direction and assume we'll need to go in that direction again next cycle and will move
		// at that point.
		if(finalVector.mag() < 2) {
			System.out.println("pausing here - vector isn't enough pressure yet");
			return;
		}
		
		Direction targetDirection = finalVector.getDirection();
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
