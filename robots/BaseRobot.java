package team035.robots;

import team035.brains.RobotBrain;
import team035.modules.NavController;
import team035.modules.RadarController;
import team035.modules.RadioController;
import team035.modules.StateCache;
import battlecode.common.RobotController;

public abstract class BaseRobot {
	RobotController rc;

	protected NavController nav;
	protected RobotBrain brain;
	protected StateCache cache;
	protected RadioController radio; 
	protected RadarController radar;
	
	public static BaseRobot robot;

	public BaseRobot(RobotController myRc) {
		this.rc = myRc;
		
		BaseRobot.robot = this;
	}

	// This is my helpful comment about this function!
	public void engage() {
		// We need to do this here for the first turn. Subsequent turns' starts
		// will happen inside this.yield().
		this.startTurn();
		while(true) {
			try {
				brain.think();
			} catch (Exception e) {
				e.printStackTrace();
			}

			this.yield();
		}
	}

	protected void startTurn() {
		try{
			this.cache.newRound();
			this.radar.scan();
			this.radio.receive();
		} catch (Exception e) {
			// This is bad! It'll short circuit an entire turn startup.
			e.printStackTrace();
		}
	}

	protected void endTurn() {
		this.radio.transmit();
	}

	protected void yield() {
		try {
			this.endTurn();
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.rc.yield(); 

		try {
			this.startTurn();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public RobotController getRc() {
		return rc;
	}


	public NavController getNav() {
		return nav;
	}


	public RobotBrain getBrain() {
		return brain;
	}


	public StateCache getCache() {
		return cache;
	}


	public RadioController getRadio() {
		return radio;
	}


	public RadarController getRadar() {
		return radar;
	}
}
