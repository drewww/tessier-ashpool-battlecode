package team035.modules;

import team035.robots.BaseRobot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class NavController {
	protected enum Mode {
		PATHING, BUGGING
	}

	protected BaseRobot r;
	protected MapLocation target;
	protected Mode mode;
	protected Direction bugDirection;
	private int epsilon;


	public NavController(BaseRobot r) {
		this.r = r;
		this.mode = Mode.PATHING;
		this.bugDirection = Direction.NONE;
	}

	public void setTarget(MapLocation loc) {
		this.setTarget(loc, 0);
	}
	
	public void setTarget(MapLocation loc, int epsilon) {
		this.target = loc;
		this.epsilon = epsilon;
	}
	
	public MapLocation getTarget() {
		return this.target;
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
			case BUGGING:
				moved = this.doBuggingMove();
				break;
			}
		}


		return moved;
	}

	int withinEpsilon = 0;
	public boolean isAtTarget() {
		
		int distance =target.distanceSquaredTo(this.r.getRc().getLocation());
		
		if(distance==0) {
			withinEpsilon = 0;
			return true;
		}
		if(distance <= this.epsilon*this.epsilon) {
			withinEpsilon++;
			
			if(withinEpsilon > 3) {
				withinEpsilon = 0;
				return true;
			}
		} else {
			withinEpsilon = 0;
		}
		
		return false;
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
				if(canMoveForward()) {
					rc.moveForward();
					return true;
				} else {
					// enter bugging mode
					this.mode = Mode.BUGGING;
					return this.setInitialBuggingDirection();
				}

			}
		} catch (GameActionException gae) {
			gae.printStackTrace();
		}
		return false;

	}
	
	protected boolean canMoveForward() {
		boolean canMove = false;
		switch (r.getRc().getType()) {
		case SOLDIER:
			MapLocation[] nodeLocs = r.getRc().senseCapturablePowerNodes();
			MapLocation frontLoc = r.getRc().getLocation().add(r.getRc().getDirection());
			for (MapLocation loc :nodeLocs) {
				if(frontLoc.equals(loc.add(Direction.SOUTH))) {
					return false;
				}
			}
			if(!r.getCache().canMove(r.getRc().getDirection())) {
				return false;
			}
			break;
		case ARCHON:
			if(!r.getCache().canMove(r.getRc().getDirection())) {
				return false;
			}
		}
		return true;
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
				this.mode = Mode.PATHING;
				return true;
			}
			else {
				if(canMoveForward()) {
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
