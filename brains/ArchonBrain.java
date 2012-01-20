package team035.brains;

import java.util.Random;

import team035.messages.ClaimNodeMessage;
import team035.messages.LowFluxMessage;
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
	protected ArchonState[] stateStack;
	protected int stateStackTop;


	protected enum ArchonState {
		LOITERING, MOVING, BUILDING
	}

	protected ArchonState state;
	protected MapLocation nodeBuildLocation;
	protected PowerNode targetPowerNode;
	
	protected boolean fluxTransferQueued = false;
	protected MapLocation fluxTransferLoc = null;
	protected RobotLevel fluxTransferLevel = null;
	protected double fluxTransferAmount = 0.0;
	
	public ArchonBrain(BaseRobot r) {
		super(r);
		
		r.getRadio().addListener(this, ClaimNodeMessage.type);
		r.getRadio().addListener(this, LowFluxMessage.type);
		
		this.initStateStack();
	}

	@Override
	public void think() {
		
		if(fluxTransferQueued) {
			
			GameObject go;
			try {
				go = this.r.getRc().senseObjectAtLocation(this.r.getRc().getLocation().add(this.r.getRc().getDirection()), RobotLevel.ON_GROUND);
				if(go!=null) {
					this.r.getRc().transferFlux(fluxTransferLoc, fluxTransferLevel, fluxTransferAmount);
				}
			} catch (GameActionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			fluxTransferQueued = false;
		} 
		
		switch(this.getState()) {
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
        MapLocation nearNodeLoc = getRandomCapturableNode();

        this.r.getNav().setTarget(nearNodeLoc, true);
        this.r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST), new MoveOrderMessage(r.getNav().getTarget()));
        
        this.popState();
        this.pushState(ArchonState.MOVING);
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
						this.popState();
						this.pushState(ArchonState.LOITERING);
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
			this.popState();
			this.pushState(ArchonState.BUILDING);
			System.out.println("Archon moving->building");			
			build();
		}
	}	
	
	protected void queueFluxTransfer(MapLocation loc, RobotLevel level, double amount) {
		this.fluxTransferLoc = loc;
		this.fluxTransferLevel = level;
		this.fluxTransferAmount = amount;
		this.fluxTransferQueued = true;
	}
	
	protected boolean spawnRobotIfPossible() {
		
		if(!r.getRc().isMovementActive() && r.getRc().getFlux() > RobotType.SOLDIER.spawnCost + INITIAL_ROBOT_FLUX) {
			
			if(r.getRc().canMove(r.getRc().getDirection())) {
				try {
					r.getRc().spawn(RobotType.SOLDIER);
					this.queueFluxTransfer(this.r.getRc().getLocation().add(this.r.getRc().getDirection()), RobotLevel.ON_GROUND, 0.9*INITIAL_ROBOT_FLUX);;
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
		
		if(msg.msg.getType()==LowFluxMessage.type) {
			LowFluxMessage lowFluxMessage = (LowFluxMessage) msg.msg;
			
			if(this.r.getRc().getLocation().distanceSquaredTo(lowFluxMessage.loc)<=2) {
				// do a transfer.
				if(!this.fluxTransferQueued) {
					double amountToTransfer = INITIAL_ROBOT_FLUX*0.75;
					
					if(amountToTransfer > this.r.getRc().getFlux()) {
						amountToTransfer = this.r.getRc().getFlux();
					}
					
					this.queueFluxTransfer(lowFluxMessage.loc, lowFluxMessage.level, amountToTransfer);
				}
			}
		}
		
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
	
	// Helper stuffs
	protected MapLocation getRandomCapturableNode() {
		StateCache cache = r.getCache();		
		RobotController rc = r.getRc();
		MapLocation[] capturableNodes = cache.senseCapturablePowerNodes();
		Random rng = new Random(this.r.getRc().getRobot().getID());
//		System.out.println("Seed: " + this.r.getRc().getRobot().getID());
//		System.out.println("Length: " + capturableNodes.length + " rng: " + rng.nextInt(capturableNodes.length-1));
		return capturableNodes[rng.nextInt(capturableNodes.length)];
	}

	protected ArchonState getState() {
		if(this.stateStackTop < 0) {
			return ArchonState.LOITERING;
		}
		return this.stateStack[this.stateStackTop];
	}
	
	protected void pushState(ArchonState state) {
		this.stateStackTop++;
		// Check to see if we need to expand the stack size
		if(this.stateStack.length >= this.stateStackTop) {
			ArchonState[] newStack = new ArchonState[this.stateStack.length*2];
			for(int i = 0; i < this.stateStack.length; ++i) {
				newStack[i] = this.stateStack[i];
			}
			this.stateStack = newStack;
		}
		this.stateStack[this.stateStackTop] = state;
	}

	protected ArchonState popState() {
		ArchonState state = this.getState();
		if(this.stateStackTop >= 0) {
			this.stateStackTop--;	
		}
		// Push the default loitering state if somehow we popped the last state
		return state;
	}
	
	protected void initStateStack() {
		this.stateStack = new ArchonState[10];
		this.stateStackTop = 0;
		this.pushState(ArchonState.LOITERING);
	}
	
}
