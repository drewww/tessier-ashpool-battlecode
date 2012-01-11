package team035;

import battlecode.common.RobotController;

public abstract class Robot {
	RobotController rc;
	
	public Robot(RobotController myRc) {
		this.rc = myRc;
	}
	
	public abstract void engage();
	
}
