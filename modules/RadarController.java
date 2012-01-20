package team035.modules;

import team035.messages.MessageAddress;
import team035.messages.RobotInfosMessage;
import team035.robots.BaseRobot;
import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class RadarController {
	protected BaseRobot r;
	protected RobotController rc;
	
	protected StateCache cache;
	
	protected boolean enabled = true;
	protected boolean enemyTargetBroadcast = false;
	
	public RadarController (BaseRobot r) {
		this.r = r;
		this.rc = this.r.getRc();
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public void setEnemyTargetBroadcast(boolean enemyTargetBroadcast) {
		this.enemyTargetBroadcast = enemyTargetBroadcast;
	}

	public void scan() {
		if(!enabled) return;
		
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
		
		if(this.enemyTargetBroadcast && this.cache.numEnemyRobotsInRange > 0) {
			RobotInfosMessage msg = new RobotInfosMessage(this.cache.getEnemyRobots());
			this.r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST), msg);
		}
	}
}
