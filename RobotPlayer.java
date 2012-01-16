package team035;

import team035.robots.ArchonRobot;
import team035.robots.BaseRobot;
import team035.robots.SoldierRobot;
import battlecode.common.RobotController;

public class RobotPlayer {
    public static void run(RobotController rc) {
    	BaseRobot robot;
    	switch(rc.getType()) {
    	case ARCHON:
    		robot = new ArchonRobot(rc);
    		robot.engage();
    		break;
    	case SOLDIER:
    		robot = new SoldierRobot(rc);
    		robot.engage();
    		break;
    	default:
    		while(true) {
    			rc.yield();
    		}
    	
    	}
    }
}
