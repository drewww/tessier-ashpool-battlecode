package team035.brains;

import team035.messages.LowFluxMessage;
import team035.messages.MessageAddress;
import team035.messages.MessageWrapper;
import team035.messages.MoveOrderMessage;
import team035.messages.RobotInfosMessage;
import team035.messages.SRobotInfo;
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

public class ScorcherBrain extends RobotBrain implements RadioListener {

	public enum ScorcherState {
		HOLD,
		MOVE,
		ATTACK,
		SEEK_TARGET,
		LOST,
		LOW_FLUX,
		OUT_OF_FLUX,
		WAIT
	}

	protected ScorcherState state;

	protected final static double PLENTY_OF_FLUX_THRESHOLD = 20.0;
	protected final static double LOW_FLUX_THRESHOLD = 10.0;
	protected final static double OUT_OF_FLUX_THRESHOLD = 5.0;
	protected final static double LOST_THRESHOLD = 10;
	
	protected int turnsHolding = 0;
	protected int turnsSinceLastOutOfFluxMessage = 0;
	
	public ScorcherBrain(BaseRobot r) {
		super(r);

		state = ScorcherState.WAIT;

		r.getRadio().addListener(this, MoveOrderMessage.type);
		r.getRadio().addListener(this, LowFluxMessage.type);
		r.getRadio().addListener(this, RobotInfosMessage.type);
	}

	@Override
	public void think() {
		// do some global environmental state checks in order of precedence.
		if(r.getCache().numEnemyRobotsInAttackRange > 0) {
			this.state = ScorcherState.ATTACK;
		} else if(this.r.getRc().getFlux() < OUT_OF_FLUX_THRESHOLD) {
			this.r.getRadio().setEnabled(false);
			this.r.getRadar().setEnabled(false);
			this.state = ScorcherState.OUT_OF_FLUX;
			// turn the radio off, shutdown the radar
		} else if(this.r.getRc().getFlux() < ScorcherBrain.LOW_FLUX_THRESHOLD) {
			this.state = ScorcherState.LOW_FLUX;
		} else if(r.getCache().numRemoteRobots > 0) {
			
			// we only get here if we don't see any robots ourselves, but 
			// OTHER people see robots to attack. But we prefer to transition
			// into ATTACK if we can.
			r.getLog().println("Setting state to SEEK_TARGET");
			this.state = ScorcherState.SEEK_TARGET;
		}


		r.getLog().println("state: " + this.state);
		
		this.displayState();

		switch(this.state) {
		case WAIT:
			// this is the more serious do nothing command
			if(Clock.getRoundNum() > ArchonBrain.ATTACK_TIMING) {
				// Break out just in case we missed a message
				this.state = ScorcherState.HOLD;
			}
			break;
		case HOLD:
			// do nothing! we're waiting for someone to tell us where to go.
			turnsHolding++;
			
			if(turnsHolding > LOST_THRESHOLD) {
				this.state = ScorcherState.LOST;
			}
			
			break;
		case MOVE:
			turnsHolding = 0;
			
			NavController nav = this.r.getNav();
			if(nav.isAtTarget()) {
				r.getLog().println("Scorcher reached target");
				r.getLog().println("Target: " + nav.getTarget());
				r.getLog().println("Location: " + this.r.getRc().getLocation());
				this.state = ScorcherState.HOLD;
			} else {
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
				this.state = ScorcherState.HOLD;
				break;
			}
									// state if there are no enemies
			
			// move towards the target
			this.r.getNav().setTarget(target.location, 0);
			this.state = ScorcherState.MOVE;
			nav = this.r.getNav();
			if(nav.isAtTarget()) this.state = ScorcherState.HOLD;
			nav.doMove();
			
			break;
		case ATTACK:
			
			if(r.getCache().numEnemyRobotsInAttackRange==0) {
				this.state = ScorcherState.MOVE;
			}
			
			if(r.getRc().isAttackActive()) {
				return;
			}
			RobotInfo[] enemies = r.getCache().getEnemyRobots();
			
			// now target selection is really dumb - pick the first one in range
			for(RobotInfo enemy : enemies) {
				if(r.getRc().canAttackSquare(enemy.location)) {
					// skip if it's a tower and there are other baddies around or if
					// it's a tower not connected to the graph
					if((enemy.type == RobotType.TOWER && r.getCache().numEnemyAttackRobotsInRange > 0) ||
							isInvulnerableTower(enemy)) {
						continue;
					}
					RobotLevel l = RobotLevel.ON_GROUND;
					if(enemy.type == RobotType.SCOUT)
						l = RobotLevel.IN_AIR;
 
					
					try {
						// face the enemy if possible
						RobotController rc = this.r.getRc();
						Direction towardsEnemy = rc.getLocation().directionTo(enemy.location);
						if(rc.getDirection() != towardsEnemy) {
							if(!rc.isMovementActive()) {
								rc.setDirection(towardsEnemy);
							}
						}
						// BLAM!
						r.getRc().attackSquare(enemy.location, l);
						break;
					} catch (GameActionException e) {
						// TODO Auto-generated catch block
						r.getLog().printStackTrace(e);
					}
					
				}
			}	
			RobotController rc = this.r.getRc();
			enemies = r.getCache().getEnemyAttackRobotsInRange();
			int optimalDistance = RobotType.SCORCHER.attackRadiusMaxSquared;
			//MapLocation centroid = new MapLocation(0,0);
	    //centroid = new MapLocation(centroid.x / enemies.length, centroid.y / enemies.length);
			MapLocation closestLoc = null;
			MapLocation myLoc = r.getRc().getLocation();
			double closestDistance = Double.MAX_VALUE;
			for(RobotInfo enemy : enemies) {
				double distance = myLoc.distanceSquaredTo(enemy.location);
				if(distance < optimalDistance) {
					closestLoc = enemy.location;
					closestDistance = distance;
				}
			}
			this.moveAwayFrom(closestLoc);
			break;
		case LOST:
			MapLocation archonLoc = this.r.getRc().senseAlliedArchons()[0];
			r.getLog().println("Archon loc = " + archonLoc);
			r.getLog().println("My loc = " + this.r.getRc().getLocation());
			this.r.getNav().setTarget(archonLoc, false);
			this.state = ScorcherState.MOVE;
			turnsHolding = 0;
			break;
		case LOW_FLUX:
			// if we're in low flux mode, we want to path to our nearest archon
			// and then ask it for flux.
			if(this.r.getRc().getFlux() > ScorcherBrain.LOW_FLUX_THRESHOLD) {
				r.getLog().println("someone refueled me!");
				this.state = ScorcherState.HOLD;
				break;
			}
			
			MapLocation nearestFriendlyArchon = this.r.getCache().getNearestFriendlyArchon();
			
			if(this.r.getRc().getLocation().distanceSquaredTo(nearestFriendlyArchon)<=2) {
				r.getLog().println("there's an archon nearby that can refuel me!");
				r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.ROBOT_TYPE, RobotType.ARCHON), new LowFluxMessage(this.r.getRc().getRobot(), this.r.getRc().getLocation(), RobotLevel.ON_GROUND));				
			} else {
				
				r.getLog().println("in low flux mode, moving to " + nearestFriendlyArchon);

				this.r.getNav().setTarget(nearestFriendlyArchon, true);
				// limp towards archon!
				//this.r.getNav().setTarget(target.location, 0);
				this.state = ScorcherState.MOVE;
				nav = this.r.getNav();
				if(nav.isAtTarget()) this.state = ScorcherState.HOLD;
				nav.doMove();				
			}
			break;
			
		case OUT_OF_FLUX:
			// check to see if our flux level is back up.
			if(this.r.getRc().getFlux() > LOW_FLUX_THRESHOLD) {
				// what should we transition into? move?
				this.r.getRadio().setEnabled(true);
				this.r.getRadar().setEnabled(true);				
				this.state = ScorcherState.LOST;
				// turn the radar+radio back on, etc.
			}
			
//			if(turnsSinceLastOutOfFluxMessage >= 30) {
//				r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST), new LowFluxMessage(this.r.getRc().getRobot(), this.r.getRc().getLocation(), RobotLevel.ON_GROUND));
//				turnsSinceLastOutOfFluxMessage = 0;
//			}
//			turnsSinceLastOutOfFluxMessage++;
//			break;
			break;
		}
	}

	protected void displayState() {
		String stateString = "NONE";
		switch(this.state) {
		case ATTACK:
			stateString = "ATTACK";
			break;
		case HOLD:
			stateString = "HOLD";
			break;
		case LOST:
			stateString = "LOST";
			break;
		case LOW_FLUX:
			stateString = "LOW_FLUX";
			break;
		case MOVE:
			stateString = "MOVE";
			break;
		case OUT_OF_FLUX:
			stateString = "OUT_OF_FLUX";
			break;
		case SEEK_TARGET:
			stateString = "SEEK_TARGET";
			break;
		}
		this.r.getRc().setIndicatorString(0, stateString);
	}
	
	
	@Override
	public void handleMessage(MessageWrapper msg) {
		if(msg.msg.getType()==MoveOrderMessage.type) {
			MoveOrderMessage mom = (MoveOrderMessage) msg.msg;
			// if we get a move order message, update our move destination.

			r.getLog().println("updating move target to: " + mom.moveTo);
			this.r.getNav().setTarget(mom.moveTo, 3);
			
			if(this.state==ScorcherState.HOLD || this.state==ScorcherState.WAIT) {
				this.state = ScorcherState.MOVE;
			}
		} else if (msg.msg.getType()==LowFluxMessage.type) {
			
		} else if (msg.msg.getType()==RobotInfosMessage.type) {
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
	
}
