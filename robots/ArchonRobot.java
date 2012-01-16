package team035.robots;

import team035.brains.ArchonBrain;
import team035.modules.NavController;
import team035.modules.RadarController;
import team035.modules.RadioController;
import team035.modules.StateCache;
import battlecode.common.RobotController;

public class ArchonRobot extends BaseRobot {
	
	public ArchonRobot(RobotController myRc) {
		super(myRc);
		
		this.radar = new RadarController(this);
		this.radio = new RadioController(this);
		this.cache = new StateCache(this);
		this.nav = new NavController(this);
		
		this.brain = new ArchonBrain(this);
//		this.brain = new DrewTestBrain(this);
//		this.brain = new OwenTestBrain(this);

	}
}
