package team035.modules;

import team035.robots.BaseRobot;

public class RadarController {
	protected BaseRobot r;
	
	public RadarController (BaseRobot r) {
		this.r = r;
		
		// this will probably also grab the state cache so it can push data into it.
	}
	
	public void scan() {
		// looks around the robot and updates the state cache
	}
}
