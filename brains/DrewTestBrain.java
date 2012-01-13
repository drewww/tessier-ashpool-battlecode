package team035.brains;

import team035.messages.LocationMessage;
import team035.messages.MessageAddress;
import team035.robots.BaseRobot;

/**
 * A dummy brain where you can just throw function calls in for some trivial testing.
 * 
 * @author drew
 *
 */
public class DrewTestBrain extends RobotBrain {

	public DrewTestBrain(BaseRobot r) {
		super(r);
	}

	@Override
	public void think() {
		// Leaving this empty in the repo for people to add testing code as they so desire.
		
		// this is some extra test brain content blah blah
		this.r.getCache().getEnemyRobots();
		this.r.getCache().getFriendlyRobots();
		this.r.getCache().getRobots();
		
		// do some message stuff
		this.r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST), new LocationMessage(this.r.getRc().getRobot(), this.r.getRc().getLocation()));
	}
}
