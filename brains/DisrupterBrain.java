package team035.brains;

import team035.brains.SoldierBrain.SoldierState;
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

public class DisrupterBrain extends RobotBrain implements RadioListener {

	public enum DisrupterState {
		HOLD,
		MOVE,
		ATTACK,
		SEEK_TARGET,
		LOST,
		LOW_FLUX,
		OUT_OF_FLUX,
		WAIT
	}

	protected DisrupterState state;

	protected final static double PLENTY_OF_FLUX_THRESHOLD = 20.0;
	protected final static double LOW_FLUX_THRESHOLD = 10.0;
	protected final static double OUT_OF_FLUX_THRESHOLD = 5.0;
	protected final static double LOST_THRESHOLD = 10;
	
	protected int turnsHolding = 0;
	protected int turnsSinceLastOutOfFluxMessage = 0;
	
	public DisrupterBrain(BaseRobot r) {
		super(r);

		state = DisrupterState.WAIT;

		r.getRadio().addListener(this, MoveOrderMessage.type);
		r.getRadio().addListener(this, LowFluxMessage.type);
		r.getRadio().addListener(this, RobotInfosMessage.type);
	}

	@Override
	public void think() {
		// do some global environmental state checks in order of precedence.
		if(r.getCache().numEnemyRobotsInAttackRange > 0) {
			this.state = DisrupterState.ATTACK;
		} else if(this.r.getRc().getFlux() < OUT_OF_FLUX_THRESHOLD) {
			this.r.getRadio().setEnabled(false);
			this.r.getRadar().setEnabled(false);
			this.state = DisrupterState.OUT_OF_FLUX;
			// turn the radio off, shutdown the radar
		} else if(this.r.getRc().getFlux() < DisrupterBrain.LOW_FLUX_THRESHOLD) {
			this.state = DisrupterState.LOW_FLUX;
		} else if(r.getCache().numRemoteRobots > 0) {
			
			// we only get here if we don't see any robots ourselves, but 
			// OTHER people see robots to attack. But we prefer to transition
			// into ATTACK if we can.
			r.getLog().println("Setting state to SEEK_TARGET");
			this.state = DisrupterState.SEEK_TARGET;
		}


		r.getLog().println("state: " + this.state);
		
		this.displayState();

		switch(this.state) {
		case WAIT:
			// this is the more serious do nothing command
			if(Clock.getRoundNum() > ArchonBrain.ATTACK_TIMING) {
				// Break out just in case we missed a message
				this.state = DisrupterState.HOLD;
			}
			break;
		case HOLD:
			// do nothing! we're waiting for someone to tell us where to go.
			turnsHolding++;
			
			if(turnsHolding > LOST_THRESHOLD) {
				this.state = DisrupterState.LOST;
			}
			
			break;
		case MOVE:
			turnsHolding = 0;
			
			NavController nav = this.r.getNav();
			if(nav.isAtTarget()) {
				r.getLog().println("Disrupter reached target");
				r.getLog().println("Target: " + nav.getTarget());
				r.getLog().println("Location: " + this.r.getRc().getLocation());
				this.state = DisrupterState.HOLD;
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
				this.state = DisrupterState.HOLD;
				break;
			}
									// state if there are no enemies
			
			// move towards the target
			this.r.getNav().setTarget(target.location, 0);
			this.state = DisrupterState.MOVE;
			nav = this.r.getNav();
			if(nav.isAtTarget()) this.state = DisrupterState.HOLD;
			nav.doMove();
			
			break;
		case ATTACK:
			
			if(r.getCache().numEnemyRobotsInAttackRange==0) {
				this.state = DisrupterState.MOVE;
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
			this.state = DisrupterState.MOVE;
			turnsHolding = 0;
			break;
		case LOW_FLUX:
			// if we're in low flux mode, we want to path to our nearest archon
			// and then ask it for flux.
			if(this.r.getRc().getFlux() > DisrupterBrain.LOW_FLUX_THRESHOLD) {
				r.getLog().println("someone refueled me!");
				this.state = DisrupterState.HOLD;
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
				this.state = DisrupterState.MOVE;
				nav = this.r.getNav();
				if(nav.isAtTarget()) this.state = DisrupterState.HOLD;
				nav.doMove();				
			}
			break;
			
		case OUT_OF_FLUX:
			// check to see if our flux level is back up.
			if(this.r.getRc().getFlux() > LOW_FLUX_THRESHOLD) {
				// what should we transition into? move?
				this.r.getRadio().setEnabled(true);
				this.r.getRadar().setEnabled(true);				
				this.state = DisrupterState.LOST;
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
		String stateString = this.state.toString();
		
		this.r.getRc().setIndicatorString(0, stateString);
	}
	
	
	@Override
	public void handleMessage(MessageWrapper msg) {
		if(msg.msg.getType()==MoveOrderMessage.type) {
			MoveOrderMessage mom = (MoveOrderMessage) msg.msg;
			// if we get a move order message, update our move destination.

			r.getLog().println("updating move target to: " + mom.moveTo);
			this.r.getNav().setTarget(mom.moveTo, 3);
			
			if(this.state==DisrupterState.HOLD || this.state==DisrupterState.WAIT) {
				this.state = DisrupterState.MOVE;
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
}
