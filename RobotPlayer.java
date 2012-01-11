package team035;

import battlecode.common.*;
import java.util.Random;

public class RobotPlayer {
//	static int counter;

    public static void run(RobotController myRC) {
    	BaseRobot robot;
    	switch(myRC.getType()) {
    	case ARCHON:
    		robot = new BoidRobot(myRC);
    		robot.engage();
    		break;
    	case SCOUT:
    		robot = new ScoutRobot(myRC);
    		robot.engage();
    		break;    		
    	default:
    		while(true) {
    			myRC.yield();
    		}
    	
    	}
//    	counter++;
//    	while(true) {
//
//	    	myRC.setIndicatorString(0, "Counter is: " + counter);
//    	}
//    	
//    	switch(myRC.getType()) {
//    	default:
//        while (true) {
//            try {
//                while (myRC.isMovementActive()) {
//                    myRC.yield();
//                }
//
//                if (myRC.canMove(myRC.getDirection())) {
//                    myRC.moveForward();
//                } else {
//                    myRC.setDirection(myRC.getDirection().rotateRight());
//                }
//                myRC.yield();
//            } catch (Exception e) {
//                System.out.println("caught exception:");
//                e.printStackTrace();
//            }
//        }
//    	}
    }
}
