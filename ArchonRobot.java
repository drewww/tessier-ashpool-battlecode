package team035;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

public class ArchonRobot extends BaseRobot {
	protected enum State {
		SETUP, SPAWNING 
	}

	protected State state;

	public ArchonRobot(RobotController myRc) {
		super(myRc);
		state = State.SETUP;
	}

	public void engage() {
		while(true) {
			switch(state) {
			case SETUP:
				// Shuffle away from other archons
				MapLocation[] archonLocs = rc.senseAlliedArchons();
				MapLocation myLoc = rc.getLocation();
				boolean hasAdjacent = false;
				for(MapLocation loc: archonLocs) {
					if(loc.isAdjacentTo(myLoc)) {
						try {
							if(myLoc.directionTo(loc) == Direction.NORTH)  {
								hasAdjacent = true;
								while(rc.isMovementActive()) {
									rc.yield();
								}
								if(rc.senseObjectAtLocation(myLoc.add(Direction.SOUTH), RobotLevel.ON_GROUND) == null) {
									rc.setDirection(Direction.SOUTH);
									rc.yield();
									rc.moveForward();
									rc.yield();
								}
							} else if(myLoc.directionTo(loc) == Direction.WEST) {
								hasAdjacent = true;
								while(rc.isMovementActive()) {
									rc.yield();
								}
								if(rc.senseObjectAtLocation(myLoc.add(Direction.EAST), RobotLevel.ON_GROUND) == null) {
									rc.setDirection(Direction.EAST);
									rc.yield();
									rc.moveForward();
									rc.yield();
								}
							}
						}catch (GameActionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							rc.yield();						
						}
					} 
				}
				if(!hasAdjacent) {
					state = State.SPAWNING;
				}
				break;
			case SPAWNING:
				// Produce a scout with enough flux to power it
				if(rc.getFlux() > RobotType.SCOUT.spawnCost + 40) {
					try {
						rc.spawn(RobotType.SCOUT);
						rc.yield();
						rc.transferFlux(rc.getLocation().add(rc.getDirection()), RobotLevel.IN_AIR, 40);

					} catch (GameActionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				break;
			}
			rc.yield();
		}

		// DONT COME BACK UNTIL YOU'RE DEAD SON!
	}

}
