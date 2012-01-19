package team035.brains;

import java.util.Random;

import team035.messages.ClaimNodeMessage;
import team035.messages.MessageAddress;
import team035.messages.MessageAddress.AddressType;
import team035.messages.MessageWrapper;
import team035.messages.MoveOrderMessage;
import team035.modules.NavController;
import team035.modules.RadioListener;
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

public class ArchonBrain extends RobotBrain implements RadioListener {
	protected final static double NODE_DETECTION_RADIUS_SQ = 16;
	protected final static double INITIAL_ROBOT_FLUX = 30;


	protected enum ArchonState {
		LOITERING, MOVING, BUILDING
	}

	protected ArchonState state;
	protected MapLocation nodeBuildLocation;
	protected PowerNode targetPowerNode;
	
	protected boolean fluxTransferQueued = false;
	
	public ArchonBrain(BaseRobot r) {
		super(r);
		this.state = ArchonState.LOITERING;
		
		r.getRadio().addListener(this, ClaimNodeMessage.type);
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
        this.r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST), new MoveOrderMessage(r.getNav().getTarget()));
        
        this.state = ArchonState.MOVING;
        System.out.println("Archon loitering->moving");
        this.nodeBuildLocation = nearNodeLoc;
        this.move();
	}
	
	protected void build() {
		NavController nav = this.r.getNav();
		RobotController rc = this.r.getRc();
		try {
			if(nav.isAtTarget()) {
				
				r.getRadio().addMessageToTransmitQueue(new MessageAddress(AddressType.BROADCAST), new ClaimNodeMessage());
				
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
			this.state = ArchonState.BUILDING;
			System.out.println("Archon moving->building");			
			build();
		}
	}	
	
//	protected void moveToRandomNodeNeighbor(PowerNode node) {
//		MapLocation[] nodeNeighbors = node.neighbors();
//		System.out.println("Found " + nodeNeighbors.length + " neighbors");
//		Random rng = new Random(Clock.getRoundNum() + r.getRc().getRobot().getID());
//		int rNum = rng.nextInt(nodeNeighbors.length);
//		System.out.println("Heading for neighbor " + rNum);
//		MapLocation target = nodeNeighbors[rNum];
//		// Move south of the target so we're not standing on it...
//		target = target.add(Direction.SOUTH);
//		this.r.getNav().setTarget(target);
//		this.state = ArchonState.MOVING;
//		System.out.println("Archon loitering->moving");
//		this.move();
//	}
	
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

	@Override
	public void handleMessage(MessageWrapper msg) {
//		if(msg.msg.getType()==ClaimNodeMessage.type) {
//			this.state = ArchonState.LOITERING;
//		}
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
