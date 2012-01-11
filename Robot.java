package team035;

import battlecode.common.RobotController;

public abstract class Robot {
	RobotController myRc;
	
	public Robot(RobotController myRc) {
		this.myRc = myRc;
	}
	
	public abstract void engage();
	
}
