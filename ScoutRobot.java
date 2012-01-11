package team035;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class ScoutRobot extends Robot {
	
	public ScoutRobot(RobotController myRc) {
		super(myRc);
	}

	public void engage() {
		while(true) {
	    try {
        while (rc.isMovementActive()) {
            rc.yield();
        }

        if (rc.canMove(rc.getDirection())) {
            rc.moveForward();
        } else {
            rc.setDirection(rc.getDirection().rotateRight());
        }
        rc.yield();
    } catch (Exception e) {
        System.out.println("caught exception:");
        e.printStackTrace();
    }			
		}
		
		// DONT COME BACK UNTIL YOU'RE DEAD SON!
	}
	
}
