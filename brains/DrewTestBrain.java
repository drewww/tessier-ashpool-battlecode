package team035.brains;

import team035.messages.LocationMessage;
import team035.messages.MessageAddress;
import team035.messages.MessageWrapper;
import team035.messages.MoveOrderMessage;
import team035.modules.RadioListener;
import team035.robots.BaseRobot;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

/**
 * A dummy brain where you can just throw function calls in for some trivial testing.
 * 
 * @author drew
 *
 */
public class DrewTestBrain extends RobotBrain implements RadioListener {

	public boolean spawnedLastTurn = false;
	
	public DrewTestBrain(BaseRobot r) {
		super(r);
		
		r.getRadio().addListener(this, LocationMessage.type);
	}

	@Override
	public void think() {
//		// this is some extra test brain content blah blah
//		this.r.getCache().getEnemyRobots();
//		this.r.getCache().getFriendlyRobots();
//		this.r.getCache().getRobots();
//		
//		// do some message stuff
//		this.r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST), new LocationMessage(this.r.getRc().getRobot(), this.r.getRc().getLocation()));
		
		
		if(spawnedLastTurn) {
		GameObject go;
		try {
			go = this.r.getRc().senseObjectAtLocation(this.r.getRc().getLocation().add(this.r.getRc().getDirection()), RobotLevel.ON_GROUND);
			if(go!=null) {
				this.r.getRc().transferFlux(this.r.getRc().getLocation().add(this.r.getRc().getDirection()), RobotLevel.ON_GROUND, 40);
			}
		} catch (GameActionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		}
		
		spawnedLastTurn = false;
		
		
		
		if(this.r.getRc().getFlux() > RobotType.SOLDIER.spawnCost+40) {
			try {
				this.r.getRc().spawn(RobotType.SOLDIER);
				spawnedLastTurn = true;
			} catch (GameActionException e) {
				// TODO Auto-generated catch block
				r.getLog().printStackTrace(e);
			}
		}
		
		// spin!
		try {
			if(!this.r.getRc().isMovementActive() && !spawnedLastTurn) {
				this.r.getRc().setDirection(this.r.getRc().getDirection().rotateRight());
			}
		} catch (GameActionException e) {
			// TODO Auto-generated catch block
			r.getLog().printStackTrace(e);
		}
		
		MapLocation l = this.r.getRc().getLocation().add(-20, -60);
		this.r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST), new MoveOrderMessage(l));
	}

	@Override
	public void handleMessage(MessageWrapper msg) {
		r.getLog().println("Received message: " + msg);
	}
}
