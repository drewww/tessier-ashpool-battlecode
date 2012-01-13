package team035.modules;

import team035.robots.BaseRobot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class NavController {
	protected BaseRobot r;
	protected MapLocation target;
	
	public NavController(BaseRobot r) {
		this.r = r;
	}
	
	public void setTarget(MapLocation loc) {
		this.target = loc;
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
		StateCache cache = this.r.getCache();
		try {
			if(!rc.isMovementActive()) {
				Direction moveHeading = getDesiredHeading();
				if(rc.getDirection() != moveHeading) {
					rc.setDirection(moveHeading);
					moved = true;
				}
				else if(cache.canMove(moveHeading)) {
					rc.moveForward();
					moved = true;
				}
			}
		} catch (GameActionException gae) {
			gae.printStackTrace();
			moved = false;
		}
		
		return moved;
	}
	
	public boolean isAtTarget() {
		return target.equals(this.r.getRc().getLocation());
	}
	
	
}
