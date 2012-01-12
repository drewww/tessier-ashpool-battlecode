package team035.modules;

import battlecode.common.RobotInfo;
import team035.robots.BaseRobot;

public class StateCache {
	public static final int MAX_ROBOTS = 64;
	
	protected BaseRobot r;
	
	
	
	// this will eventually contain
	// 1. arrays of known robots (enemy + friendly)
	// 2. terrain
	// 3. map boundaries
	// 4. tower knowledge (what's open
	// 5. friendly archon states
	// 6. overall team strategy
	
	protected RobotInfo[] robots;
	protected RobotInfo[] friendlyRobots;
	protected RobotInfo[] enemyRobots;
	public int numRobotsInRange;
	public int numFriendlyRobotsInRange;
	public int numEnemyRobotsInRange;
	
	public StateCache(BaseRobot r) {
		this.r = r;
	}
	
	public void newRound() {

		// for now, flush all our state each round.
		robots = new RobotInfo[MAX_ROBOTS];
		numRobotsInRange = 0;
		numFriendlyRobotsInRange = 0;
		numEnemyRobotsInRange = 0;
		
		friendlyRobots = new RobotInfo[MAX_ROBOTS];;
		enemyRobots = new RobotInfo[MAX_ROBOTS];;
	}

	public void addRobot(RobotInfo r) {
		this.robots[numRobotsInRange] = r;
		
		numRobotsInRange++;
		
		
		if(r.team==this.r.getRc().getTeam()) {
			this.friendlyRobots[numFriendlyRobotsInRange] = r;
			numFriendlyRobotsInRange++;
		} else {
			this.enemyRobots[numEnemyRobotsInRange] = r;
			 numEnemyRobotsInRange++;
		}
	}
	
	public RobotInfo[] getRobots() {
		RobotInfo[] out = new RobotInfo[numRobotsInRange];
		System.arraycopy(this.robots, 0, out, 0, numRobotsInRange);
		
		return out;
	}
	
	public RobotInfo[] getFriendlyRobots() {
		RobotInfo[] out = new RobotInfo[numFriendlyRobotsInRange];
		System.arraycopy(this.friendlyRobots, 0, out, 0, numFriendlyRobotsInRange);
		
		return out;
	}
	
	public RobotInfo[] getEnemyRobots() {
		RobotInfo[] out = new RobotInfo[numEnemyRobotsInRange];
		System.arraycopy(this.enemyRobots, 0, out, 0, numEnemyRobotsInRange);
		
		return out;

	}
	
}
