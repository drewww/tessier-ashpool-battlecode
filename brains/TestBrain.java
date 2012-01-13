package team035.brains;

import team035.robots.BaseRobot;

/**
 * A dummy brain where you can just throw function calls in for some trivial testing.
 * 
 * @author drew
 *
 */
public class TestBrain extends RobotBrain {

	public TestBrain(BaseRobot r) {
		super(r);
	}

	@Override
	public void think() {
		// call it a few times to make sure cacheing is working.
		this.r.getCache().getFriendlyArchonLocs();
		this.r.getCache().getFriendlyArchonLocs();
		this.r.getCache().getFriendlyArchonLocs();
		
		this.r.getCache().getEnemyRobots();
		this.r.getCache().getFriendlyRobots();
		this.r.getCache().getRobots();
	}
}