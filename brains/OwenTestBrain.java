package team035.brains;

import team035.modules.NavController;
import team035.robots.BaseRobot;
import battlecode.common.MapLocation;

public class OwenTestBrain extends RobotBrain {

	public OwenTestBrain(BaseRobot r) {
		super(r);
		MapLocation loc =  this.r.getRc().getLocation();
		this.r.getNav().setTarget(new MapLocation(loc.x - 7, loc.y+5));
	}

	@Override
	public void think() {
		System.out.println("At: " + this.r.getRc().getLocation());
		NavController nav = this.r.getNav();
		boolean moved = nav.doMove();
		if(nav.isAtTarget()) {
			System.out.println("Reached target!");
		}
	}

}
