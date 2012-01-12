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

	public BaseRobot(RobotController myRc) {
		this.rc = myRc;
	}

	// This is my helpful comment about this function!
	public void engage() {
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
		this.radar.scan();
		this.radio.receive();
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


	public NavController getMc() {
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
