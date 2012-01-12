package team035;

import team035.robots.ArchonRobot;
import team035.robots.BaseRobot;
import battlecode.common.RobotController;

public class RobotPlayer {
    public static void run(RobotController rc) {
    	BaseRobot robot;
    	switch(rc.getType()) {
    	case ARCHON:
    		robot = new ArchonRobot(rc);
    		robot.engage();
    		break;
    	default:
    		while(true) {
    			rc.yield();
    		}
    	
    	}
    }
}
