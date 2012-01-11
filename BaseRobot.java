package team035;

import battlecode.common.RobotController;

public abstract class BaseRobot {
	RobotController rc;
	
	public BaseRobot(RobotController myRc) {
		this.rc = myRc;
	}
	
	public abstract void engage();
	
}
