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
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

public class SoldierBrain extends RobotBrain implements RadioListener {

	public enum SoldierState {
		HOLD,
		MOVE,
		ATTACK,
		SEEK_TARGET,
		LOST,
		LOW_FLUX,
		OUT_OF_FLUX,
	}

	protected SoldierState state;

	protected final static double PLENTY_OF_FLUX_THRESHOLD = 20.0;
	protected final static double LOW_FLUX_THRESHOLD = 5.0;
	protected final static double OUT_OF_FLUX = 1.0;
	
	protected int turnsHolding = 0;
	protected int turnsSinceLastOutOfFluxMessage = 0;
	
	public SoldierBrain(BaseRobot r) {
		super(r);

		state = SoldierState.HOLD;

		r.getRadio().addListener(this, MoveOrderMessage.type);
		r.getRadio().addListener(this, LowFluxMessage.type);
		r.getRadio().addListener(this, RobotInfosMessage.type);
	}

	@Override
	public void think() {
		// do some global environmental state checks in order of precedence.
		if(this.r.getRc().getFlux() < OUT_OF_FLUX) {
			this.state = SoldierState.OUT_OF_FLUX;
			
			// turn the radio off, shutdown the radar
		} else if(this.r.getRc().getFlux() < LOW_FLUX_THRESHOLD) {
			this.state = SoldierState.LOW_FLUX;
		} else if(r.getCache().numEnemyRobotsInRange > 0) {
			this.state = SoldierState.ATTACK;
		} else if(r.getCache().numRemoteRobots > 0) {
			
			// we only get here if we don't see any robots ourselves, but 
			// OTHER people see robots to attack. But we prefer to transition
			// into ATTACK if we can.
			
			this.state = SoldierState.SEEK_TARGET;
		}


		System.out.println("state: " + this.state);

		switch(this.state) {
		case HOLD:
			// do nothing! we're waiting for someone to tell us where to go.
			turnsHolding++;
			
			if(turnsHolding > 1000) {
				this.state = SoldierState.LOST;
			}
			
			break;
		case MOVE:
			turnsHolding = 0;
			
			NavController nav = this.r.getNav();
			if(nav.isAtTarget()) this.state = SoldierState.HOLD;
			nav.doMove();
			break;
		case SEEK_TARGET:
			
			if(r.getRc().isMovementActive()) break;
			
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
			
			if(target==null) break; // this shouldn't happen - we won't be in this
									// state if there are no enemies
			
 
			try {
				// now point ourselves at that enemy / move towards them.
				Direction targetDirection =r.getRc().getLocation().directionTo(target.location); 
				if(r.getRc().getDirection().equals(targetDirection)) r.getRc().moveForward();
				else r.getRc().setDirection(targetDirection);
			} catch (GameActionException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			// the way we transition out of this is we see something to attack on
			// our OWN scanners.
			break;
		case ATTACK:
			
			if(r.getCache().numEnemyRobotsInRange==0) {
				this.state = SoldierState.MOVE;
			}
			
			if(r.getRc().isAttackActive()) {
				return;
			}
			RobotInfo[] enemies = r.getCache().getEnemyRobots();
			
			// now target selection is really dumb - pick the first one in range
			for(RobotInfo enemy : enemies) {
				if(r.getRc().canAttackSquare(enemy.location)) {
					RobotLevel l = RobotLevel.ON_GROUND;
					if(enemy.type == RobotType.SCOUT) {
						l = RobotLevel.IN_AIR;
					} 
					
					try {
						r.getRc().attackSquare(enemy.location, l);
						break;
					} catch (GameActionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			break;
		case LOST:
			this.r.getNav().setTarget(this.r.getRc().senseAlliedArchons()[0]);
			this.state = SoldierState.MOVE;
			turnsHolding = 0;
			break;
		case LOW_FLUX:
			// if we're in low flux mode, we want to path to our nearest archon
			// and then ask it for flux.
			if(this.r.getRc().getFlux() > this.LOW_FLUX_THRESHOLD) {
				this.state = SoldierState.HOLD;
				break;
			}
			
			MapLocation nearestFriendlyArchon = this.r.getCache().getNearestFriendlyArchon();
			
			if(this.r.getRc().getLocation().distanceSquaredTo(nearestFriendlyArchon)==2) {
				r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.ROBOT_TYPE, RobotType.ARCHON), new LowFluxMessage(this.r.getRc().getRobot(), this.r.getRc().getLocation(), RobotLevel.ON_GROUND));				
			} else {
				
				System.out.println("in low flux mode, moving to " + nearestFriendlyArchon);

				this.r.getNav().setTarget(nearestFriendlyArchon, true);
			}
			break;
			
		case OUT_OF_FLUX:
			// check to see if our flux level is back up.
			if(this.r.getRc().getFlux() > OUT_OF_FLUX) {
				// what should we transition into? move?
				this.state = SoldierState.HOLD;
				
				// turn the radar+radio back on, etc.
			}
			
			if(turnsSinceLastOutOfFluxMessage >= 30) {
				r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST), new LowFluxMessage(this.r.getRc().getRobot(), this.r.getRc().getLocation(), RobotLevel.ON_GROUND));
				turnsSinceLastOutOfFluxMessage = 0;
			}
			turnsSinceLastOutOfFluxMessage++;
			break;
		}
	}

	@Override
	public void handleMessage(MessageWrapper msg) {
		if(msg.msg.getType()==MoveOrderMessage.type) {
			MoveOrderMessage mom = (MoveOrderMessage) msg.msg;
			// if we get a move order message, update our move destination.

			System.out.println("updating move target to: " + mom.moveTo);
			this.r.getNav().setTarget(mom.moveTo, 3);
			
			if(this.state==SoldierState.HOLD) {
				this.state = SoldierState.MOVE;
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
}
