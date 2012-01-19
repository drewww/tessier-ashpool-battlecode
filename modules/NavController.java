package team035.modules;

import team035.robots.BaseRobot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class NavController {
	protected enum Mode {
		PATHING, BUGGING, DOCKING
	}

	protected BaseRobot r;
	protected MapLocation target;
	protected MapLocation dockingTarget;
	protected Mode mode;
	protected Mode preBuggingMode;
	protected Direction bugDirection;
	
	protected static final Direction[] compass = {
			Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, 
			Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST,
			Direction.WEST, Direction.NORTH_WEST
		};
	
	protected int nextDockingLoc;


	public NavController(BaseRobot r) {
		this.r = r;
		this.mode = Mode.PATHING;
		this.bugDirection = Direction.NONE;
	}

	protected void resetDocking() {
		this.nextDockingLoc = 0;
		this.target = this.dockingTarget.add(NavController.compass[this.nextDockingLoc]);
	}
	
	protected void setNextDockingTarget() {
		this.nextDockingLoc++;
		this.nextDockingLoc %= NavController.compass.length;
		this.target = this.dockingTarget.add(NavController.compass[this.nextDockingLoc]);
	}
	
	public void setTarget(MapLocation loc) {
		this.setTarget(loc, false);
	}
	
	public void setTarget(MapLocation loc, boolean isDocking) {
			if(true == isDocking) {
				this.mode = Mode.DOCKING;
				this.dockingTarget = loc;
				this.resetDocking();
			} else {
				this.mode = Mode.PATHING;
				this.target = loc;
			}
	}
	
	
	
	public Direction getDesiredHeading() {
		MapLocation myLoc = this.r.getRc().getLocation();
		return myLoc.directionTo(this.target);
	}

	public boolean doMove() {
		// fail fast if we're already there
		if(this.isAtTarget()) {
			return false;			
		}
		boolean moved = false;
		RobotController rc = this.r.getRc();

		if(!rc.isMovementActive()) {
			switch(this.mode) {
			case PATHING:
				moved = this.doPathingMove();
				break;
			case DOCKING:
				moved = this.doDockingMove();
				break;
			case BUGGING:
				moved = this.doBuggingMove();
				break;
			}
		}


		return moved;
	}

	public boolean isAtTarget() {
		return target.equals(this.r.getRc().getLocation());
	}

	protected boolean doPathingMove() {
		RobotController rc = this.r.getRc();
		StateCache cache = this.r.getCache();
		Direction moveHeading = getDesiredHeading();
		try {
			if(rc.getDirection() != moveHeading) {
				rc.setDirection(moveHeading);
				return true;
			}
			else {
				if(cache.canMove(moveHeading)) {
					rc.moveForward();
					return true;
				} else {
					// enter bugging mode
					this.preBuggingMode = this.mode;
					this.mode = Mode.BUGGING;
					return this.setInitialBuggingDirection();
				}

			}
		} catch (GameActionException gae) {
			gae.printStackTrace();
		}
		return false;

	}
	
	protected boolean doDockingMove() {
		RobotController rc = this.r.getRc();
		boolean success = this.doPathingMove();
		MapLocation robotLoc = rc.getLocation(); 
		if(robotLoc.distanceSquaredTo(this.target) == 1) {
			if(!rc.canMove(robotLoc.directionTo(this.target))) {
				this.setNextDockingTarget();
			}
		}
		return success;
	}
	
	protected boolean setInitialBuggingDirection() {
		RobotController rc = this.r.getRc();
		StateCache cache = this.r.getCache();
		// we're entering this mode because forward motion is blocked. 
		// scan every direction for the best unblocked move
		MapLocation robotLoc = rc.getLocation();
		Direction baseHeading = rc.getDirection();
		Direction bestDirection = Direction.NONE;
		double bestDistance = Double.MAX_VALUE;
		for(Direction direction : Direction.values()) {
			if(direction == Direction.NONE || direction == Direction.OMNI) {
				continue;
			}
			MapLocation testLocation = robotLoc.add(direction);
			double distance = testLocation.distanceSquaredTo(this.target);
			if(distance < bestDistance) {
				if(rc.canMove(direction)) {
					bestDirection = direction;
					bestDistance = distance;
				}
			}
		}
		try {
			rc.setDirection(bestDirection);
			this.bugDirection = bestDirection;
			return true;
		} catch (GameActionException gae) {
			gae.printStackTrace();
		}
		return false;
	}
	
	protected boolean doBuggingMove() {
		RobotController rc = this.r.getRc();
		StateCache cache = this.r.getCache();
		Direction targetHeading = getDesiredHeading();
		try {
			if(cache.canMove(targetHeading)) {
				rc.setDirection(targetHeading);
				this.mode = this.preBuggingMode;
				return true;
			}
			else {
				Direction heading = rc.getDirection();
				if(cache.canMove(heading)) {
					rc.moveForward();
					return true;
				} else {
					// At the moment I'm not checking whether this could put us
					// in an infinite loop, which it could well in some pretty
					// trivial cases.
					return setInitialBuggingDirection();
				}

			}
		} catch (GameActionException gae) {
			gae.printStackTrace();
		}
		return false;
	}


}
