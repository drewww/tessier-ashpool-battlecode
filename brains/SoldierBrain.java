package team035.brains;

import team035.messages.MessageWrapper;
import team035.messages.MoveOrderMessage;
import team035.modules.NavController;
import team035.modules.RadioListener;
import team035.robots.BaseRobot;
import battlecode.common.MapLocation;

public class SoldierBrain extends RobotBrain implements RadioListener {

	public enum SoldierState {
		HOLD,
		MOVE,
		ATTACK,
		LOST
	}

	protected SoldierState state;

	public SoldierBrain(BaseRobot r) {
		super(r);

		state = SoldierState.HOLD;

		r.getRadio().addListener(this, MoveOrderMessage.type);
	}

	@Override
	public void think() {

		switch(this.state) {
		case HOLD:
			// do nothing! we're waiting for someone to tell us where to go.
			
			break;
		case MOVE:
			NavController nav = this.r.getNav();
			if(nav.isAtTarget()) this.state = SoldierState.HOLD;
			boolean moved = nav.doMove();
			break;
		case ATTACK:

			break;
		case LOST:

			break;
		}

	}

	@Override
	public void handleMessage(MessageWrapper msg) {
		if(msg.msg.getType()==MoveOrderMessage.type) {
			MoveOrderMessage mom = (MoveOrderMessage) msg.msg;
			// if we get a move order message, update our move destination.

			System.out.println("updating move target to: " + mom.moveTo);
			this.r.getNav().setTarget(mom.moveTo);
		}
	}

}
