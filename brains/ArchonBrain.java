package team035.brains;

import java.util.Random;

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
	
	protected enum ArchonState {
		LOITERING, MOVING, BUILDING
	}

	protected ArchonState state;
	protected MapLocation nodeBuildLocation;
	
	public ArchonBrain(BaseRobot r) {
		super(r);
		this.state = ArchonState.LOITERING;
	}

	@Override
	public void think() {
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
		MapLocation[] emptyNodes = cache.senseCapturablePowerNodes();
		for(MapLocation nodeLoc: emptyNodes) {
			System.out.println("Detected node at: " + nodeLoc);
			if(nodeLoc.distanceSquaredTo(myLoc) < NODE_DETECTION_RADIUS_SQ) {
				this.r.getNav().setTarget(nodeLoc.add(Direction.SOUTH));				
				this.nodeBuildLocation = nodeLoc;
				this.state = ArchonState.BUILDING;
				System.out.println("Archon loitering->building");
				this.build();
				return;
			}
		}
		
		// Look at where we're connected to and try to go there!
		PowerNode nearestNode = getNearestAlliedNode();
		MapLocation[] nodeNeighbors = nearestNode.neighbors();
		System.out.println("Found " + nodeNeighbors.length + " neighbors");
		Random rng = new Random(Clock.getRoundNum() + rc.getRobot().getID());
		int rNum = rng.nextInt(nodeNeighbors.length);
		System.out.println("Heading for neighbor " + rNum);
		MapLocation target = nodeNeighbors[rNum];
		// Move south of the target so we're not standing on it...
		target = target.add(Direction.SOUTH);
		this.r.getNav().setTarget(target);
		this.sendMoveOrder(target);
		this.state = ArchonState.MOVING;
		System.out.println("Archon loitering->moving");
		this.move();
		return;
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
		NavController nav = this.r.getNav();
		nav.doMove();
		if(nav.isAtTarget()) {
			this.state = ArchonState.LOITERING;
			System.out.println("Archon moving->loitering");			
			loiter();
		}
	}	
	
	protected void sendMoveOrder(MapLocation target) {
		
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
	

}
