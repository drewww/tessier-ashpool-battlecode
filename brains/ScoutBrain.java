package team035.brains;

import team035.messages.LowFluxMessage;
import team035.messages.MessageAddress;
import team035.messages.MessageWrapper;
import team035.messages.MoveOrderMessage;
import team035.messages.RobotInfosMessage;
import team035.messages.SRobotInfo;
import team035.messages.ScoutOrderMessage;
import team035.modules.NavController;
import team035.modules.RadioListener;
import team035.robots.BaseRobot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class ScoutBrain extends RobotBrain implements RadioListener {

	public enum ScoutState {
		HOLD,
		MOVE,
		ATTACK,
		SEEK_TARGET,
		LOST,
		LOW_FLUX,
		OUT_OF_FLUX,
		WAIT,
		SCOUT
	}

	protected ScoutState state;

	protected final static double PLENTY_OF_FLUX_THRESHOLD = 20.0;
	protected final static double LOW_FLUX_THRESHOLD = 10.0;
	protected final static double OUT_OF_FLUX_THRESHOLD = 5.0;
	protected final static double LOST_THRESHOLD = 10;
	
	protected int turnsHolding = 0;
	protected int turnsSinceLastOutOfFluxMessage = 0;
	
	protected Direction scoutDirection = null;
	protected boolean clockwise = false;

	// order is TOP, RIGHT, BOTTOM, LEFT
	// eg NORTH, EAST, SOUTH, WEST
	protected int[] walls = {-1, -1, -1, -1};
	
	public ScoutBrain(BaseRobot r) {
		super(r);

		state = ScoutState.MOVE;

		r.getRadio().addListener(this, MoveOrderMessage.type);
		r.getRadio().addListener(this, ScoutOrderMessage.type);
		r.getRadar().setEnemyTargetBroadcast(true);
	}

	@Override
	public void think() {
		// do some global environmental state checks in order of precedence.

		if(this.state != ScoutState.SCOUT) {
			if(r.getCache().numEnemyRobotsInAttackRange > 0) {
				this.state = ScoutState.ATTACK;
			} else if(this.r.getRc().getFlux() < OUT_OF_FLUX_THRESHOLD) {
				this.r.getRadio().setEnabled(false);
				this.r.getRadar().setEnabled(false);
				this.state = ScoutState.OUT_OF_FLUX;
				// turn the radio off, shutdown the radar
			} else if(this.r.getRc().getFlux() < ScoutBrain.LOW_FLUX_THRESHOLD) {
				this.state = ScoutState.LOW_FLUX;
			} else if(r.getCache().numRemoteRobots > 0) {

				// we only get here if we don't see any robots ourselves, but 
				// OTHER people see robots to attack. But we prefer to transition
				// into ATTACK if we can.
				r.getLog().println("Setting state to SEEK_TARGET");
				this.state = ScoutState.SEEK_TARGET;
			}
		}

		this.shareFlux();

		// no matter our state, check and see if we should heal
		int unitsToHeal = 0;
		for(RobotInfo friendly : r.getCache().getFriendlyRobots()) {
			if(!friendly.regen && friendly.energon != friendly.type.maxEnergon) {
				unitsToHeal++;
			}
		}
		
		if(unitsToHeal >=2 && this.state != ScoutState.LOW_FLUX && this.state != ScoutState.OUT_OF_FLUX) {
			try {
				this.r.getRc().regenerate();
			} catch (GameActionException e) {
				// TODO Auto-generated catch block
				r.getLog().printStackTrace(e);
			}
		}
		
		//r.getLog().println("state: " + this.state);
		
		this.displayState();

		switch(this.state) {
		case WAIT:
			// this is the more serious do nothing command
			if(Clock.getRoundNum() > ArchonBrain.ATTACK_TIMING) {
				// Break out just in case we missed a message
				this.state = ScoutState.HOLD;
			}
			break;
		case HOLD:
			// do nothing! we're waiting for someone to tell us where to go.
			turnsHolding++;
			
			if(turnsHolding > LOST_THRESHOLD) {
				this.state = ScoutState.LOST;
			}
			
			break;
		case MOVE:
			turnsHolding = 0;
			
			NavController nav = this.r.getNav();
			
			// recalculate the centroid as the new move target
			RobotInfo[] friendlies = r.getCache().getFriendlyRobots();
			MapLocation centroid = new MapLocation(0,0);
			int soldiers = 0;
	    for(RobotInfo friend: friendlies) {
	    	if(friend.type!=RobotType.SOLDIER) {
	    		continue;
	    	}
	    	soldiers++;
	    	MapLocation friendLoc = friend.location; 
	    	centroid = centroid.add(friendLoc.x, friendLoc.y);
	    }
	    if(soldiers==0) {
	    	this.state = ScoutState.LOST;
	    } else {
	    	centroid = new MapLocation(centroid.x / soldiers, centroid.y / soldiers);
		    nav.setTarget(centroid);
	    }
			if(!nav.isAtTarget()) {
				nav.doMove();
			}
			break;
		case SEEK_TARGET:
			
			//if(r.getRc().isMovementActive()) break;
			
			// we don't see anyone bad, but others near us do. 
			// so, turn towards the nearest target.
			
			// maybe eventually we should use the centroid code that
			// archons use for spreading to aim at the center of detected mass?
			int d2 = 100000;
			SRobotInfo target = null;
			for(SRobotInfo enemy : r.getCache().getRemoteRobots()) {
				if(enemy==null) break;
				int distanceToRobot = enemy.location.distanceSquaredTo(r.getRc().getLocation());
				
				if(distanceToRobot < d2) {
					d2 = distanceToRobot;
					target = enemy;
				}
			}
			
			if(target==null) {
				// this shouldn't happen - we won't be in this
				r.getLog().println("Seeking enemy with no valid targets!");
				r.getLog().println("Remote enemies length: " + r.getCache().getRemoteRobots().length);
				r.getLog().println("Remote enemies number: " + r.getCache().numRemoteRobots);
				this.state = ScoutState.HOLD;
				break;
			}
									// state if there are no enemies
			
			// move towards the target
			this.r.getNav().setTarget(target.location, 0);
			this.state = ScoutState.MOVE;
			nav = this.r.getNav();
			if(nav.isAtTarget()) this.state = ScoutState.HOLD;
			nav.doMove();
			
			break;
		case ATTACK:
			
			if(r.getCache().numEnemyRobotsInAttackRange==0) {
				this.state = ScoutState.MOVE;
			}
			
			if(r.getRc().isAttackActive()) {
				return;
			}
			// get target from the radar
			RobotInfo attackTarget = this.r.getRadar().acquireTarget();
			
			// drop out if we don't actually have a target we like
			if(attackTarget == null) return;
			
			RobotLevel level = RobotLevel.ON_GROUND;
			if(attackTarget.type==RobotType.SCOUT) level = RobotLevel.IN_AIR;
			
			try {
				r.getRc().attackSquare(attackTarget.location, level);
			} catch (GameActionException e) {
				r.getLog().printStackTrace(e);
			}
			
			break;
		case LOST:
			MapLocation archonLoc = this.r.getRc().senseAlliedArchons()[0];
			r.getLog().println("Archon loc = " + archonLoc);
			r.getLog().println("My loc = " + this.r.getRc().getLocation());
			this.r.getNav().setTarget(archonLoc, false);
			this.state = ScoutState.MOVE;
			turnsHolding = 0;
			break;
		case LOW_FLUX:
			// if we're in low flux mode, we want to path to our nearest archon
			// and then ask it for flux.
			if(this.r.getRc().getFlux() > ScoutBrain.LOW_FLUX_THRESHOLD) {
				r.getLog().println("someone refueled me!");
				this.state = ScoutState.HOLD;
				break;
			}
			
			MapLocation nearestFriendlyArchon = this.r.getCache().getNearestFriendlyArchon();
			
			if(this.r.getRc().getLocation().distanceSquaredTo(nearestFriendlyArchon)<=2) {
				r.getLog().println("there's an archon nearby that can refuel me!");
				r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.ROBOT_TYPE, RobotType.ARCHON), new LowFluxMessage(this.r.getRc().getRobot(), this.r.getRc().getLocation(), RobotLevel.ON_GROUND));				
			} else {
				
//				r.getLog().println("in low flux mode, moving to " + nearestFriendlyArchon);

				this.r.getNav().setTarget(nearestFriendlyArchon, true);
				// limp towards archon!
				//this.r.getNav().setTarget(target.location, 0);
				this.state = ScoutState.MOVE;
				nav = this.r.getNav();
				if(nav.isAtTarget()) this.state = ScoutState.HOLD;
				nav.doMove();				
			}
			break;
			
		case OUT_OF_FLUX:
			// check to see if our flux level is back up.
			if(this.r.getRc().getFlux() > LOW_FLUX_THRESHOLD) {
				// what should we transition into? move?
				this.r.getRadio().setEnabled(true);
				this.r.getRadar().setEnabled(true);				
				this.state = ScoutState.LOST;
				// turn the radar+radio back on, etc.
			}
			
//			if(turnsSinceLastOutOfFluxMessage >= 30) {
//				r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST), new LowFluxMessage(this.r.getRc().getRobot(), this.r.getRc().getLocation(), RobotLevel.ON_GROUND));
//				turnsSinceLastOutOfFluxMessage = 0;
//			}
//			turnsSinceLastOutOfFluxMessage++;
//			break;
			break;
		case SCOUT:

			if(r.getRc().isMovementActive()) break;
			
			if(!this.r.getRc().getDirection().equals(this.scoutDirection)) {
				try {
					r.getRc().setDirection(this.scoutDirection);
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
			
			// if we're in scout mode, travel in the direction specified unless we see a wall
			// look at MAX RANGE in our scout direction and see if it's a wall. if it's not, 
			// the move.
			
			int scoutRange = (int) java.lang.Math.sqrt(r.getRc().getType().sensorRadiusSquared);
			MapLocation locationToSense = r.getRc().getLocation().add(this.scoutDirection, scoutRange);
			TerrainTile terrain = r.getRc().senseTerrainTile(locationToSense);
			
			if(terrain==TerrainTile.OFF_MAP) {
				// we've found a wall! record it and then pick a new direction to move. 
				
				// (ther's a small side issue here - we should try sensing closer tiles to see if
				// they're ALSO off map. Basically, if we spawn in wall range, we'll misjudge how
				// far off the wall is.
				int index;
				int loc;
				switch(this.scoutDirection) {
				case NORTH:
					index = 0;
					loc = locationToSense.y;
					break;
				case EAST:
					index = 1;
					loc = locationToSense.x;
					break;
				case SOUTH:
					index = 2;
					loc = locationToSense.y;
					break;
				case WEST:
					index = 3;
					loc = locationToSense.x;
					break;
				default: 
					index = -1;
					loc = -1;
				}
				if(index!=-1) {
					this.walls[index] = loc; 
				}
				
				// TODO decide to come home if we've seen enough walls
				
				// always turn right for now.
				if(this.clockwise) {
					this.scoutDirection = this.scoutDirection.rotateRight().rotateRight();					
				} else {
					this.scoutDirection = this.scoutDirection.rotateLeft().rotateLeft();										
				}
				
				try {
					this.r.getRc().setDirection(this.scoutDirection);
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try {
					r.getRc().moveForward();
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			break;
		}
	}

	protected void displayState() {
		String stateString = this.state.toString();
		
		this.r.getRc().setIndicatorString(0, stateString);
	}
	
	
	@Override
	public void handleMessage(MessageWrapper msg) {
		if(msg.msg.getType()==ScoutOrderMessage.type) {
			// if we get a scout order message switch to scout state, and ignore
			// other messages. 
			ScoutOrderMessage som = (ScoutOrderMessage) msg.msg;
			r.getLog().println("--------------------ENTERING SCOUT MODE: " + som.scoutDirection);

			
			this.state = ScoutState.SCOUT;
			this.scoutDirection = som.scoutDirection;
			this.clockwise = som.clockwise;
		} else if(msg.msg.getType()==MoveOrderMessage.type && this.state != ScoutState.SCOUT) {
			MoveOrderMessage mom = (MoveOrderMessage) msg.msg;
			// if we get a move order message, update our move destination.

//			r.getLog().println("updating move target to: " + mom.moveTo);
			this.r.getNav().setTarget(mom.moveTo, 3);
			
			if(this.state==ScoutState.HOLD || this.state==ScoutState.WAIT) {
				this.state = ScoutState.MOVE;
			}
		} else if (msg.msg.getType()==RobotInfosMessage.type && this.state != ScoutState.SCOUT) {
			RobotInfosMessage rlm = (RobotInfosMessage) msg.msg;

			if(!rlm.friendly) {
				for(SRobotInfo r : rlm.robots) {
					this.r.getCache().addRemoteRobot(r);
				}
			}
		}
	}
	
	public boolean isInvulnerableTower(RobotInfo robot) {
		if(robot.type == RobotType.TOWER) {
			MapLocation[] towerLocs = this.r.getCache().senseCapturablePowerNodes();
			MapLocation robotLoc = robot.location;
			for(MapLocation loc : towerLocs) {
				if(loc.equals(robotLoc)) {
					return false;
				}			
			}
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
			double fluxDifference = rc.getFlux() - robot.flux;
			if(fluxDifference > 0.0) {
				if(myLoc.isAdjacentTo(robot.location)) {
					RobotLevel level = RobotLevel.ON_GROUND;
					if(robot.type == RobotType.SCOUT) {
						level = RobotLevel.IN_AIR;
					}
					try {
						rc.transferFlux(robot.location, level, fluxDifference / 2.0);
//						r.getLog().println("Transfered flux!");
					} catch (GameActionException e) {
						// TODO Auto-generated catch block
						r.getLog().printStackTrace(e);
					}
				}
			}
		}
	}
}
