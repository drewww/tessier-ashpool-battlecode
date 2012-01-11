package team035;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class ArchonRobot extends Robot {
	
	public ArchonRobot(RobotController myRc) {
		super(myRc);
	}

	public void engage() {
		while(true) {
			if(myRc.getFlux() > RobotType.SCOUT.spawnCost) {
				try {
					myRc.spawn(RobotType.SCOUT);
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		// DONT COME BACK UNTIL YOU'RE DEAD SON!
	}
	
}
