package team035.brains;

import team035.messages.LowFluxMessage;
import team035.messages.MessageAddress;
import team035.messages.MessageWrapper;
import team035.messages.MoveOrderMessage;
import team035.modules.NavController;
import team035.modules.RadioListener;
import team035.robots.BaseRobot;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

public class SoldierBrain extends RobotBrain implements RadioListener {

	public enum SoldierState {
		HOLD,
		MOVE,
		ATTACK,
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
	}

	@Override
	public void think() {
		// short circuit the whole process with out of flux
		if(this.r.getRc().getFlux() < OUT_OF_FLUX) {
			this.state = SoldierState.OUT_OF_FLUX;
		}

		if(this.state == SoldierState.OUT_OF_FLUX) {
			// check to see if our flux level is back up.
			if(this.r.getRc().getFlux() > OUT_OF_FLUX) {
				// what should we transition into? move?
				this.state = SoldierState.HOLD;
				
				// turn the radio back on, etc.
			}
			
			if(turnsSinceLastOutOfFluxMessage >= 20) {
				r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST), new LowFluxMessage(this.r.getRc().getRobot(), this.r.getRc().getLocation()));
				turnsSinceLastOutOfFluxMessage = 0;
			}
			turnsSinceLastOutOfFluxMessage++;
			return;
		}
		
		System.out.println("state: " + this.state);

		if(r.getCache().numEnemyRobotsInRange > 0) {
			this.state = SoldierState.ATTACK;
		} 
	
		
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
		}
	}

	@Override
	public void handleMessage(MessageWrapper msg) {
		System.out.println("Got message! " + msg);
		if(msg.msg.getType()==MoveOrderMessage.type) {
			MoveOrderMessage mom = (MoveOrderMessage) msg.msg;
			// if we get a move order message, update our move destination.

			System.out.println("updating move target to: " + mom.moveTo);
			this.r.getNav().setTarget(mom.moveTo, 3);
			
			if(this.state==SoldierState.HOLD) {
				this.state = SoldierState.MOVE;
			}
		}
	}

}
