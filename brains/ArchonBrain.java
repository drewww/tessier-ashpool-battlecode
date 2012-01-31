package team035.brains;

import java.util.Random;

import team035.messages.AssaultOrderMessage;
import team035.messages.ClaimNodeMessage;
import team035.messages.LowFluxMessage;
import team035.messages.MessageAddress;
import team035.messages.MessageAddress.AddressType;
import team035.messages.MessageWrapper;
import team035.messages.MoveOrderMessage;
import team035.messages.RobotInfosMessage;
import team035.messages.SRobotInfo;
import team035.messages.ScoutOrderMessage;
import team035.modules.NavController;
import team035.modules.RadioListener;
import team035.modules.StateCache;
import team035.robots.BaseRobot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.PowerNode;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class ArchonBrain extends RobotBrain implements RadioListener {
	protected final static int BUILDING_COOLDOWN_VALUE = 8;
	protected final static int SPREADING_COOLDOWN_VALUE = 3;
	protected final static int REFUEL_FLUX = 30;
	protected final static int REFUEL_THRESHOLD = 20;
	protected final static int MOVE_FAIL_COUNTER = 100;
	protected final static int TOO_MANY_FRIENDLIES = 5;
	protected final static int RECENCY_THRESHOLD = 50;
	protected final static int SCOUT_SPAWN_FREQUENCY = 500;
	public final static int ATTACK_TIMING = 160;
	
	protected ArchonState[] stateStack;
	protected int stateStackTop;
	protected int archonNumber;
	
	protected enum ArchonState {
		LOITERING, MOVING, BUILDING, SPREADING, REFUELING, EVADING, 
		BUILDUP, DISPATCH_SCOUT, DISPATCH_ATTACKER, MOVE_OUT
	}

	
	protected ArchonState state;
	
	protected MapLocation fluxTransferLoc = null;
	protected RobotLevel fluxTransferLevel = null;
	protected double fluxTransferAmount = 0.0;
	protected int spreadingCooldown;
	protected int buildingCooldown;
	protected int moveFailCooldown;
	protected int refuelingCooldown;
	protected boolean refuelOnStack;
	protected Random rng;
	protected int turnsSinceLastEnemySeen;
	protected int turnsSinceLastScorcherSeen;
	protected int turnsSinceLastScoutMade;
	
	public ArchonBrain(BaseRobot r) {
		super(r);
		
		r.getRadio().addListener(this, ClaimNodeMessage.type);
		r.getRadio().addListener(this, LowFluxMessage.type);
		r.getRadio().addListener(this, RobotInfosMessage.type);
		r.getRadio().addListener(this, AssaultOrderMessage.type);
		
		r.getRadar().setEnemyTargetBroadcast(true);
		this.initCooldownsAndCounters();
		this.initStateStack();
		this.pushState(ArchonState.BUILDUP);
		this.setArchonNumber();
		this.refuelOnStack = false;
		rng = new Random(this.r.getRc().getRobot().getID()+1);
	}

	

	@Override
	public void think() {
		this.setArchonNumber();
		this.scanForEnemies();
		this.updateCooldownsAndCounters();
		this.shareFlux();
		
		this.displayState();
		switch(this.getState()) {
		case BUILDUP:
			buildup();
			break;
		case LOITERING:
			loiter();
			break;
		case BUILDING:
			build();
			break;
		case MOVING:
			move();
			break;
		case SPREADING:
			spread();
			break;
		case REFUELING:
			refuel();
			break;
		case EVADING:
			evade();
			break;
		case DISPATCH_SCOUT:
			dispatchScout();
			break;
		case DISPATCH_ATTACKER:
			dispatchAttacker();
			break;
		case MOVE_OUT:
			moveOut();
			break;
		}
	}
	
	protected void displayState() {
		String stateString = this.getState().toString();
		this.r.getRc().setIndicatorString(0, stateString);
	}
	
	
	protected void moveOut() {
		MapLocation target = this.r.getNav().getTarget();
		this.r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST), new AssaultOrderMessage(target));
		this.r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST), new MoveOrderMessage(target));
		this.popState();
		this.pushState(ArchonState.MOVING);
	}
	
	protected void buildup() {
		// Launch scout!
		if((this.archonNumber == 0 || this.archonNumber == 1) &&
			 (this.r.getRc().getFlux() > RobotType.SCOUT.spawnCost + RobotType.SCOUT.maxFlux)) {
			this.pushState(ArchonState.DISPATCH_SCOUT);
			spawnRobotIfPossible(RobotType.SCOUT);
		}
		
    if(Clock.getRoundNum() > ATTACK_TIMING) {
    	r.getLog().println("Triggering attack!");
    	this.popState();
    	return;
    }
    
		if(this.isNearArchons() && spreadingCooldown == 0) {
    	r.getLog().println("Archon loitering->spreading");
    	this.pushState(ArchonState.SPREADING);
    	spread();
    	return;
    }
    
    RobotController rc = this.r.getRc();
    if(canSpawn()) {
    	spawnRobotIfPossible();	
    } else if(!rc.isMovementActive()) {
    	MapLocation myLoc = rc.getLocation();
    	for(Direction heading: Direction.values()) {
    		if(heading != Direction.NONE && heading != Direction.OMNI) {
    			if(canSpawn(heading)) {
    				try {
      				rc.setDirection(heading);
      				return;
    				} catch (GameActionException e) {
    					r.getLog().printStackTrace(e);
    					return;
    				}
    			}
    		}
    	}
    	// we're crowded in, so send a move command
    	MapLocation closeNode = getRandomCapturableNode();
      this.r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST), new MoveOrderMessage(closeNode));
    }
	}
	
	protected void dispatchScout() {
		// Now check and see if we have an adjacent wall. If we do, send scouts towards it.
		// If we don't, I guess we don't scout at all? or we pick a direction randomly?
		
		Direction spottedWall = Direction.NORTH;
		int range = (int) java.lang.Math.sqrt(r.getRc().getType().sensorRadiusSquared);
		for(int i=0; i<3; i++) {
			TerrainTile t = r.getRc().senseTerrainTile(r.getRc().getLocation().add(spottedWall, range));
			if(t.equals(TerrainTile.OFF_MAP)) {
				break;
			}
			spottedWall = spottedWall.rotateRight().rotateRight();
		}
		
		spottedWall = spottedWall.rotateRight().rotateRight().rotateRight();
		
		this.r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST_DISTANCE, 2, r.getRc().getLocation()), new ScoutOrderMessage(spottedWall, this.archonNumber==0));
		this.popState();
	}
	
	protected void dispatchAttacker() {
		MapLocation myLoc = r.getRc().getLocation();
		this.r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST_DISTANCE, 2, r.getRc().getLocation()), new MoveOrderMessage(myLoc));
		this.popState();
	}
	
	protected void evade() {
		if(spawnRobotIfPossible()) {
			return;
		}
		
		RobotController rc = this.r.getRc();
		RobotInfo[] enemies = r.getCache().getEnemyAttackRobotsInRange();
		MapLocation closestLoc = null;
		MapLocation myLoc = r.getRc().getLocation();
		double closestDistance = Double.MAX_VALUE;
		for(RobotInfo enemy : enemies) {
			double distance = myLoc.distanceSquaredTo(enemy.location);
			if(distance < closestDistance) {
				closestLoc = enemy.location;
				closestDistance = distance;
			}
		}
		this.moveAwayFrom(closestLoc);
		return;

	}



	protected void scanForEnemies() {
		if(r.getCache().numEnemyRobotsInRange > 0) {
			this.turnsSinceLastEnemySeen = 0;
		}
		for(RobotInfo robot : r.getCache().getEnemyRobots()) {
			if(robot.type == RobotType.SCORCHER) {
				this.turnsSinceLastScorcherSeen = 0;
				break;
			}
		}
		
		if(r.getCache().numEnemyAttackRobotsInRange > 0) {
			boolean noThreats = true;
			for(RobotInfo enemy : r.getCache().getEnemyAttackRobotsInRange()) {
				if(enemy.flux > 0.5) {
					noThreats = false;
					break;
				}
			}
			if(noThreats) {
			}
			
			if(noThreats == false) {
				if(this.getState() != ArchonState.EVADING) {
					this.pushState(ArchonState.EVADING);
				}
				return;
			}
		}
		if(this.getState() == ArchonState.EVADING) {
				this.popState();
		}
	}



	protected void refuel() {
		GameObject go;
		try {
			if(this.r.getRc().canSenseSquare(fluxTransferLoc)) {
				go = this.r.getRc().senseObjectAtLocation(fluxTransferLoc, fluxTransferLevel);
				if(go!=null) {
					r.getLog().println("Refueled a robot!");
					double myFlux = r.getRc().getFlux();
					
					if(myFlux >= fluxTransferAmount) {
						this.r.getRc().transferFlux(fluxTransferLoc, fluxTransferLevel, fluxTransferAmount);	
					} else {
						this.r.getRc().transferFlux(fluxTransferLoc, fluxTransferLevel, myFlux);
					}
					
					
				} else {
					r.getLog().println("Refuel failed!");
				}
			} else {
				r.getLog().println("Refuel failed!");
			}
		}catch (GameActionException e) {
			// TODO Auto-generated catch block
			r.getLog().printStackTrace(e);
			r.getLog().println("Refuel failed!");
		}
		this.popState();
		this.refuelOnStack = false;
	}
	
	
	protected void loiter() {
		MapLocation nearestNode = this.getNearestCapturableNode();
		if(nearestNode.isAdjacentTo(this.r.getRc().getLocation())
				&& this.buildingCooldown == 0) {
			this.pushState(ArchonState.BUILDING);
			build();
			return;
		}
		
		if(refuelRobotsIfPossible()) {
			return;
		}
		
    if(this.isNearArchons() && spreadingCooldown == 0) {
    	r.getLog().println("Archon loitering->spreading");
    	this.pushState(ArchonState.SPREADING);
    	spread();
    	return;
    }
    

    MapLocation nodeLoc;
    // Preferentially capture nodes that make us vulnerable
    MapLocation[] vulnerableHomeLocs = getOpenPowerCoreNeighbors();
    if(vulnerableHomeLocs.length != 0) {
     r.getLog().println("Home locs vulnerable!");
     nodeLoc = vulnerableHomeLocs[this.archonNumber % vulnerableHomeLocs.length];
    } else {
    	// get a node to visit
    	MapLocation[] nodeLocs = r.getRc().senseCapturablePowerNodes();
    	nodeLoc = nodeLocs[this.archonNumber % nodeLocs.length];
    }

    this.r.getNav().setTarget(nodeLoc, true);
    this.r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST), new MoveOrderMessage(r.getNav().getTarget()));
    
    this.pushState(ArchonState.MOVING);
    r.getLog().println("Archon loitering->moving");
    this.move();
    
    
	}
	
	protected void spread() {
    StateCache cache = r.getCache();
    RobotController rc = r.getRc();
    MapLocation myLoc = rc.getLocation();		
    boolean spreadBlocked = false;
    
    MapLocation[] archons = cache.getFriendlyArchonLocs();
    MapLocation centroid = new MapLocation(0,0);
    int nearbyArchons = 0;
    for(MapLocation archonLoc: archons) {
    	if(archonLoc == myLoc) {
    		continue;
    	}
    	if(myLoc.distanceSquaredTo(archonLoc) <= GameConstants.PRODUCTION_PENALTY_R2) {
    		nearbyArchons++;
    		centroid = centroid.add(archonLoc.x, archonLoc.y);
    	}
    }
    if(nearbyArchons > 0) {
    	// check we can move
	    if(!rc.isMovementActive()) {
		    centroid = new MapLocation((int)((float)centroid.x / nearbyArchons), (int)((float)centroid.y / nearbyArchons));
		    Direction towardsCentroid = myLoc.directionTo(centroid);
		    Direction awayFromCentroid = towardsCentroid.opposite();
		    if(towardsCentroid == Direction.OMNI || towardsCentroid == Direction.NONE) {
		    	spreadBlocked = true;
		    } else {
		    	spreadBlocked = !this.moveAwayFrom(centroid);
		    }
	    }

    } else {
    	// there were no nearby archons any more!
    	r.getLog().println("Archon done spreading!");
    	this.popState();
    }
    // something went wrong, so fail and don't try again for 3 turns.	
    if(spreadBlocked) {
    	r.getLog().println("Spread blocked! Starting cooldown.");
    	this.spreadingCooldown = SPREADING_COOLDOWN_VALUE;
    	this.popState();
    }
	}
	


	protected void build() {
		NavController nav = this.r.getNav();
		RobotController rc = this.r.getRc();
		try {
			MapLocation nearestNode = this.getNearestCapturableNode();
			if(nearestNode.isAdjacentTo(rc.getLocation())) {
				r.getRadio().addMessageToTransmitQueue(new MessageAddress(AddressType.BROADCAST), new ClaimNodeMessage(nearestNode));
				
				Direction nodeDirection = rc.getLocation().directionTo(nearestNode);
				if(rc.getDirection() != nodeDirection) {
					if(!rc.isMovementActive()) {
						rc.setDirection(nodeDirection);
						return;
					}			
				}
				GameObject targetContent = rc.senseObjectAtLocation(nearestNode, RobotLevel.ON_GROUND);
				if(targetContent == null) {
					if(rc.getFlux() >= RobotType.TOWER.spawnCost) {
						if(!rc.isMovementActive()) {
							rc.spawn(RobotType.TOWER);					
						}
					}
				} else {
					// this either means we built it or someone messed with us
					this.buildingCooldown = BUILDING_COOLDOWN_VALUE;
					this.popState();
					r.getLog().println("Archon building->loitering");
					return;
				}
			} else {
				this.buildingCooldown = BUILDING_COOLDOWN_VALUE;
				this.popState();
			}
		} catch (GameActionException e) {
			r.getLog().printStackTrace(e);
		}
	}
	
	protected void move() {
		if(r.getRc().getFlux() > r.getRc().getType().maxFlux - 1) {
			if(spawnRobotIfPossible()) {
				return;
			}
		} else {
			if(r.getCache().numFriendlyRobotsInRange < TOO_MANY_FRIENDLIES) {
				if(spawnRobotIfPossible()) {
					return;
				}
				
			}
		}
    
		if(this.isNearArchons() && spreadingCooldown == 0) {
    	r.getLog().println("Archon loitering->spreading");
    	this.pushState(ArchonState.SPREADING);
    	spread();
    	return;
    }
		
		
		if(!this.refuelOnStack) {
			if(refuelRobotsIfPossible()) {
				return;
			}
		}
		
		NavController nav = this.r.getNav();
		nav.doMove();
		if(nav.isAtTarget() || moveFailCooldown <= 0) {
			this.popState();
			if(moveFailCooldown <= 0) {
				r.getLog().println("Move Failed!");
			}
		}
	}	
	
	protected void queueFluxTransfer(MapLocation loc, RobotLevel level, double amount) {
		if(this.refuelOnStack) {
			return;
		}
		this.fluxTransferLoc = loc;
		this.fluxTransferLevel = level;
		this.fluxTransferAmount = amount;

		// push refueling onto the stack
		r.getLog().println("Queuing refuel");
		this.refuelOnStack = true;
		this.pushState(ArchonState.REFUELING);
	}
	
	protected boolean spawnRobotIfPossible() {
		RobotType type = this.getTypeToSpawn();
		return spawnRobotIfPossible(type);
	}
	
	protected boolean spawnRobotIfPossible(RobotType type) {
		if(!r.getRc().isMovementActive() && (r.getRc().getFlux() > type.spawnCost + REFUEL_FLUX ||
				this.r.getRc().getFlux() == RobotType.ARCHON.maxFlux)) {
			if(canSpawn()){
				try {
					r.getRc().spawn(type);
					if(type == RobotType.SCOUT){
						this.turnsSinceLastScoutMade = 0;
					}
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					r.getLog().printStackTrace(e);
				}
				r.getLog().println("Spawned a robot.");
				return true;
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
		if(msg.msg.getType()==ClaimNodeMessage.type) {
			ClaimNodeMessage claimMessage = (ClaimNodeMessage) msg.msg;
			MapLocation claimLocation = claimMessage.loc;
			if(this.getState() == ArchonState.LOITERING || 
				 this.getState() == ArchonState.MOVING) {
				this.buildingCooldown = BUILDING_COOLDOWN_VALUE;
			}
		}
		
		if(msg.msg.getType()==LowFluxMessage.type) {
			LowFluxMessage lowFluxMessage = (LowFluxMessage) msg.msg;
			r.getLog().println("Received a refueling request!");
			
			if(this.r.getRc().getLocation().isAdjacentTo(lowFluxMessage.loc)) {
				
				
				// do a transfer.
				if(this.getState() != ArchonState.REFUELING) {
					double amountToTransfer = REFUEL_FLUX;
					
					if(amountToTransfer > this.r.getRc().getFlux()) {
						amountToTransfer = this.r.getRc().getFlux();
					}
					
					this.queueFluxTransfer(lowFluxMessage.loc, lowFluxMessage.level, amountToTransfer);
					r.getLog().println("Archon refueling!");
				}
			} else {
				r.getLog().println("Requester was out of range.");
			}
		}
		
		if(msg.msg.getType()==RobotInfosMessage.type) {
			RobotInfosMessage scoutingMessage = (RobotInfosMessage) msg.msg;
			if(scoutingMessage.isScoutingReport) {
				if(this.getState() != ArchonState.MOVE_OUT) {
					this.r.getNav().setTarget(scoutingMessage.robots[0].toRobotInfo().location);
					this.pushState(ArchonState.MOVE_OUT);
				}
			}
		}
	
		if(msg.msg.getType()==AssaultOrderMessage.type) {
			AssaultOrderMessage assaultMessage = (AssaultOrderMessage) msg.msg;
			if(this.getState() != ArchonState.MOVE_OUT &&
					this.getState() != ArchonState.MOVING) {
				this.r.getNav().setTarget(assaultMessage.moveTo);
				this.pushState(ArchonState.MOVE_OUT);
			}
		}
	}
	
	// Helper stuffs
	
	protected boolean canSpawn(Direction heading) {
		RobotController rc = this.r.getRc();
		try {
			return r.getRc().canMove(heading) &&
					 r.getRc().senseObjectAtLocation(this.r.getRc().getLocation().add(heading), RobotLevel.IN_AIR) == null &&
					 r.getRc().senseObjectAtLocation(this.r.getRc().getLocation().add(heading), RobotLevel.POWER_NODE) == null;
		} catch (GameActionException e) {
			r.getLog().printStackTrace(e);
			return false;
		}		
	}
	
	protected boolean canSpawn() {
		return canSpawn(this.r.getRc().getDirection());
	}
	
	protected boolean refuelRobotsIfPossible() {
		if(this.refuelOnStack) {
			return false;
		}
		RobotController rc = this.r.getRc();
		StateCache cache = this.r.getCache();
		RobotInfo[] nearBots = cache.getFriendlyRobots();
		for(RobotInfo robot : nearBots) {
			if(this.r.getRc().getFlux() >= REFUEL_FLUX &&
				robot.type != RobotType.ARCHON &&
				robot.type != RobotType.TOWER &&
				robot.flux < REFUEL_THRESHOLD) {
				RobotLevel level = RobotLevel.ON_GROUND;
				if(robot.type == RobotType.SCOUT) {
					level = RobotLevel.IN_AIR;
				}
				this.queueFluxTransfer(robot.location, level,  REFUEL_FLUX);
				this.r.getNav().setTarget(robot.location, true);
				this.pushState(ArchonState.MOVING);
				return true;
			}
		}
		return false;
		
	}
	
	protected boolean isNearArchons() {
    StateCache cache = r.getCache();
    RobotController rc = r.getRc();
    MapLocation myLoc = rc.getLocation();		
    
    MapLocation[] archons = cache.getFriendlyArchonLocs();
    for(MapLocation archonLoc: archons) {
    	if(archonLoc == myLoc) {
    		continue;
    	}
    	if(myLoc.distanceSquaredTo(archonLoc) <= GameConstants.PRODUCTION_PENALTY_R2) {
    		return true;
    	}
    }
		return false;
	}
	
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
		return capturableNodes[rng.nextInt(capturableNodes.length)];
	}
	
	
	// Cooldown stuffs
	
	protected void updateCooldownsAndCounters() {
		if(spreadingCooldown > 0) {
			spreadingCooldown--;
		}
		if(buildingCooldown > 0) {
			buildingCooldown--;
		}
		if(refuelingCooldown > 0) {
			refuelingCooldown--;
		}
		if(moveFailCooldown > 0 && this.getState() == ArchonState.MOVING) {
			moveFailCooldown--;
		}
		turnsSinceLastEnemySeen++;
		turnsSinceLastScorcherSeen++;
		turnsSinceLastScoutMade++;
	}
	
	protected void initCooldownsAndCounters() {
		this.spreadingCooldown = 0;
		this.buildingCooldown = 0;
		this.moveFailCooldown = 0;
		this.refuelingCooldown = 0;
		this.turnsSinceLastEnemySeen = RECENCY_THRESHOLD+1;
		this.turnsSinceLastScorcherSeen = RECENCY_THRESHOLD+1;
		this.turnsSinceLastScoutMade = 0;
	}

	// Stack state stuffs

	protected ArchonState getState() {
		if(this.stateStackTop < 0) {
			return ArchonState.LOITERING;
		}
		return this.stateStack[this.stateStackTop];
	}
	
	protected void pushState(ArchonState state) {
		if(this.stateStackTop < 0) {
			this.stateStackTop = 0;
		} else {
			this.stateStackTop++;
		}
		
		if(state == ArchonState.MOVING) {
			this.moveFailCooldown = MOVE_FAIL_COUNTER;
		}
		
		// Check to see if we need to expand the stack size
		if(this.stateStackTop >= this.stateStack.length) {
			ArchonState[] newStack = new ArchonState[this.stateStack.length*2];
			for(int i = 0; i < this.stateStack.length; ++i) {
				newStack[i] = this.stateStack[i];
			}
			this.stateStack = newStack;
			r.getLog().println("Grew stack:");
			printStack();
		}
		this.stateStack[this.stateStackTop] = state;
		printStack();
	}

	protected ArchonState popState() {
		ArchonState state = this.getState();
		if(this.stateStackTop >= 0) {
			this.stateStackTop--;	
		}
		printStack();
		// Push the default loitering state if somehow we popped the last state
		return state;
	}
	
	protected void initStateStack() {
		this.stateStack = new ArchonState[10];
		this.stateStackTop = -1;
		this.pushState(ArchonState.LOITERING);
	}
	
	protected void printStack() {
		r.getLog().println("State stack top: "  + this.stateStackTop);
		r.getLog().print("Stack: [ ");
		for(int i = 0; i <= this.stateStackTop; ++i) {
			r.getLog().print(" " +  this.stateStack[i]  + " ");
		}
		r.getLog().println(" ]");
	}


	protected RobotType getTypeToSpawn() {
		// Just soldiers for build up stage
		if(this.getState() == ArchonState.BUILDUP) {
			return RobotType.SOLDIER;
		}
		if(this.turnsSinceLastEnemySeen > RECENCY_THRESHOLD && 
				this.turnsSinceLastScoutMade > SCOUT_SPAWN_FREQUENCY) {
			boolean scoutNearby = false;
			for(RobotInfo robot : r.getCache().getFriendlyRobots()) {
				// don't build scouts if we already have one nearby				
				if(robot.type == RobotType.SCOUT) {
					scoutNearby = true;
					break;
				}
			}
			if(!scoutNearby) {
				return RobotType.SCOUT;
			}
		}
		if(this.turnsSinceLastScorcherSeen < RECENCY_THRESHOLD) {
			System.out.println("Turns since last scorcher : " + this.turnsSinceLastScorcherSeen);
			return RobotType.DISRUPTER;
		}
		return RobotType.SOLDIER;
	}
	
	protected boolean spreadIfNecessary() {
    if(this.isNearArchons() && spreadingCooldown == 0) {
    	r.getLog().println("Archon loitering->spreading");
    	this.pushState(ArchonState.SPREADING);
    	spread();
    	return true;
    }
    return false;
	}
	
	public void shareFlux() {
		RobotController rc = r.getRc();
		MapLocation myLoc = rc.getLocation();
		for(RobotInfo robot : r.getCache().getFriendlyRobots()) {
			if(robot.type == RobotType.TOWER || robot.type == RobotType.ARCHON) {
				continue;
			}
			if(robot.flux < REFUEL_THRESHOLD && rc.getFlux() > REFUEL_FLUX) {
				if(myLoc.isAdjacentTo(robot.location)) {
					RobotLevel level = RobotLevel.ON_GROUND;
					if(robot.type == RobotType.SCOUT) {
						level = RobotLevel.IN_AIR;
					}
					try {
						rc.transferFlux(robot.location, level, REFUEL_FLUX);
//						r.getLog().println("Transfered flux!");
					} catch (GameActionException e) {
						// TODO Auto-generated catch block
						r.getLog().printStackTrace(e);
					}
				}
			}
		}
	}
	
	protected boolean moveAwayFrom(MapLocation avoidLoc) {
		RobotController rc = r.getRc();
		if(!rc.isMovementActive()) {
			MapLocation myLoc = rc.getLocation();
	  	try {
	
	    	Direction avoidHeading = rc.getLocation().directionTo(avoidLoc);
	    	if(avoidHeading != Direction.NONE && avoidHeading != Direction.OMNI) {
	    		
	    		Direction bestDir = Direction.NONE;
	    		double bestDistance = myLoc.distanceSquaredTo(avoidLoc);
	    		for(Direction tryDir : Direction.values()){
	    			if(tryDir == Direction.OMNI ||
	    				 tryDir == Direction.NONE ||
	    					!rc.canMove(tryDir)) {
	    				continue;
	    			}
	    			MapLocation tryLoc = myLoc.add(tryDir);
	    			double tryDistance = tryLoc.distanceSquaredTo(avoidLoc);
	    			if(tryDistance > bestDistance) {
	    				bestDir = tryDir;
	    				bestDistance = tryDistance;
	    			}
	    		} 
	    		if(bestDir != Direction.NONE) {
		    		if(rc.getDirection() != bestDir.opposite()) {
		    			rc.setDirection(bestDir.opposite());
		    			return true;
		    		}
		    		rc.moveBackward();
		    		return true;
	    		}
	    	}
	  	} catch (GameActionException e) {
	  		r.getLog().printStackTrace(e);    		
	  	}
    }
	  return false;
	}
	
	protected MapLocation[] getOpenPowerCoreNeighbors() {
		RobotController rc = r.getRc();
		PowerNode home = rc.sensePowerCore();
		MapLocation[] homeNeighbors = home.neighbors();
		MapLocation[] capturableNodes = rc.senseCapturablePowerNodes();
		MapLocation[] openNeighbors = new MapLocation[homeNeighbors.length];
		int i = 0;
		for(MapLocation neighbor : homeNeighbors) {
			for(MapLocation capturableNode : capturableNodes) {
				if(neighbor.equals(capturableNode)) {
					openNeighbors[i] = neighbor;
					i++;
				}
			}
		}
		MapLocation[] result = new MapLocation[i];
		System.arraycopy(openNeighbors, 0, result, 0, i);
		return result;
	}
	
	protected void setArchonNumber() {
		MapLocation[] archonLocs = r.getRc().senseAlliedArchons();
		MapLocation myLoc = r.getRc().getLocation();
		for(int i = 0; i < archonLocs.length; i++) {
			if(myLoc.equals(archonLocs[i])) {
				this.archonNumber = i;
			}
		}
	}
	
}
