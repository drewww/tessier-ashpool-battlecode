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
        while (myRc.isMovementActive()) {
            myRc.yield();
        }

        if (myRc.canMove(myRc.getDirection())) {
            myRc.moveForward();
        } else {
            myRc.setDirection(myRc.getDirection().rotateRight());
        }
        myRc.yield();
    } catch (Exception e) {
        System.out.println("caught exception:");
        e.printStackTrace();
    }			
		}
		
		// DONT COME BACK UNTIL YOU'RE DEAD SON!
	}
	
}
