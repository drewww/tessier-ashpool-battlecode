package team035.brains;

import team035.robots.BaseRobot;

public abstract class RobotBrain {
	
	protected BaseRobot r;
	
	public RobotBrain(BaseRobot r) {
		this.r = r;
	}
	
	public abstract void think();
}
