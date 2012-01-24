package team035.brains;

import team035.modules.NavController;
import team035.robots.BaseRobot;
import battlecode.common.MapLocation;

public class OwenTestBrain extends RobotBrain {

	public OwenTestBrain(BaseRobot r) {
		super(r);
		MapLocation loc =  this.r.getRc().getLocation();
		this.r.getNav().setTarget(new MapLocation(loc.x, loc.y+25));
	}

	@Override
	public void think() {
		r.getLog().println("At: " + this.r.getRc().getLocation());
		NavController nav = this.r.getNav();
		boolean moved = nav.doMove();
		if(nav.isAtTarget()) {
			r.getLog().println("Reached target!");
		}
	}

}
