package team035;

import team035.robots.ArchonRobot;
import team035.robots.BaseRobot;
import team035.robots.DisrupterRobot;
import team035.robots.ScorcherRobot;
import team035.robots.ScoutRobot;
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
    	case DISRUPTER:
    		robot = new DisrupterRobot(rc);
    		robot.engage();
    		break;
    	case SCOUT:
    		robot = new ScoutRobot(rc);
    		robot.engage();
    		break;
    	case SCORCHER:
    		robot = new ScorcherRobot(rc);
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
