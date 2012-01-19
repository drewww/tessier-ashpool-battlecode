package team035.brains;

import java.util.Random;

import team035.messages.MessageAddress;
import team035.messages.MoveOrderMessage;
import team035.modules.NavController;
import team035.modules.StateCache;
import team035.robots.BaseRobot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.PowerNode;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

public class ArchonBrain extends RobotBrain {
	protected final static double NODE_DETECTION_RADIUS_SQ = 16;
	protected final static double INITIAL_ROBOT_FLUX = 30;


	protected enum ArchonState {
		LOITERING, MOVING, BUILDING
	}

	protected ArchonState state;
	protected MapLocation nodeBuildLocation;
	
	protected boolean fluxTransferQueued = false;
	
	public ArchonBrain(BaseRobot r) {
		super(r);
		this.state = ArchonState.LOITERING;
	}

	@Override
	public void think() {
		
		if(fluxTransferQueued) {
			
			GameObject go;
			try {
				go = this.r.getRc().senseObjectAtLocation(this.r.getRc().getLocation().add(this.r.getRc().getDirection()), RobotLevel.ON_GROUND);
				if(go!=null) {
					this.r.getRc().transferFlux(this.r.getRc().getLocation().add(this.r.getRc().getDirection()), RobotLevel.ON_GROUND, INITIAL_ROBOT_FLUX*0.9);
				}
			} catch (GameActionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			fluxTransferQueued = false;
		} 
		
		switch(this.state) {
		case LOITERING:
			loiter();
			break;
		case BUILDING:
			build();
			break;
		case MOVING:
			move();
			break;
		}
	}
	
	protected void loiter() {
		StateCache cache = r.getCache();
		RobotController rc = r.getRc();
		MapLocation myLoc = rc.getLocation();
		
		// If we can sense a place to build a tower, grab it
		MapLocation nearNodeLoc = getNearestCapturableNode();
		this.r.getNav().setTarget(nearNodeLoc, true);		
		this.state = ArchonState.BUILDING;
		System.out.println("Archon loitering->building");
		this.nodeBuildLocation = nearNodeLoc;
		this.build();

	}
	
	protected void build() {
		NavController nav = this.r.getNav();
		RobotController rc = this.r.getRc();
		try {
			if(nav.isAtTarget()) {
				Direction nodeDirection = rc.getLocation().directionTo(nodeBuildLocation);
				if(rc.getDirection() != nodeDirection) {
					if(!rc.isMovementActive()) {
						rc.setDirection(nodeDirection);
						return;
					}			
				}
				if(rc.getFlux() >= RobotType.TOWER.spawnCost) {
					if(!rc.isMovementActive()) {
						GameObject targetContent = rc.senseObjectAtLocation(nodeBuildLocation, RobotLevel.ON_GROUND);
						if(targetContent == null) {
							rc.spawn(RobotType.TOWER);					
						}
						this.state = ArchonState.LOITERING;
						System.out.println("Archon building->loitering");
						return;
					}

				}
			} else {
				nav.doMove();
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}
	
	protected void move() {
		
		this.spawnRobotIfPossible();
		
		NavController nav = this.r.getNav();
		nav.doMove();
		if(nav.isAtTarget()) {
			this.state = ArchonState.LOITERING;
			System.out.println("Archon moving->loitering");			
			loiter();
		}
		
		this.r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST), new MoveOrderMessage(r.getNav().getTarget()));
	}	
	
	protected void sendMoveOrder(MapLocation target) {
		
	}
	
	
	protected boolean spawnRobotIfPossible() {
		
		if(!r.getRc().isMovementActive() && r.getRc().getFlux() > RobotType.SOLDIER.spawnCost + INITIAL_ROBOT_FLUX) {
			
			if(r.getRc().canMove(r.getRc().getDirection())) {
				try {
					r.getRc().spawn(RobotType.SOLDIER);
					fluxTransferQueued = true;
					return true;
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return false;
	}
	
	// Helper stuffs
	protected PowerNode getNearestAlliedNode() {
		StateCache cache = r.getCache();		
		RobotController rc = r.getRc();
		PowerNode[] alliedNodes = cache.senseAlliedPowerNodes();
		PowerNode nearest = null;
		MapLocation myLoc = rc.getLocation();
		double bestDistance = Double.MAX_VALUE;
		for(PowerNode node : alliedNodes) {
			double distance = node.getLocation().distanceSquaredTo(myLoc);
			if(distance < bestDistance) {
				nearest = node;
				bestDistance = distance;
			}
		}	
		return nearest;
	}
	
	// Helper stuffs
	protected MapLocation getNearestCapturableNode() {
		StateCache cache = r.getCache();		
		RobotController rc = r.getRc();
		MapLocation[] capturableNodes = cache.senseCapturablePowerNodes();
		MapLocation nearest = null;
		MapLocation myLoc = rc.getLocation();
		double bestDistance = Double.MAX_VALUE;
		for(MapLocation loc : capturableNodes) {
			double distance = loc.distanceSquaredTo(myLoc);
			if(distance < bestDistance) {
				nearest = loc;
				bestDistance = distance;
			}
		}	
		return nearest;
	}
	
	
}
