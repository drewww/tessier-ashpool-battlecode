package team035.modules;

import team035.robots.BaseRobot;
import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class RadarController {
	protected BaseRobot r;
	protected RobotController rc;
	
	protected StateCache cache;
	
	public RadarController (BaseRobot r) {
		this.r = r;
		this.rc = this.r.getRc();
	}
	
	public void scan() {
		this.cache = r.getCache();
		
		// looks around the robot and updates the state cache
		for(Robot robot : this.rc.senseNearbyGameObjects(Robot.class)) {
			// this will be all robots, friendly and enemy
		
			try {
				RobotInfo info = this.rc.senseRobotInfo(robot);
				
				this.cache.addRobot(info);
			} catch (GameActionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
