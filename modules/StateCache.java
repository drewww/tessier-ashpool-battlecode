package team035.modules;

import team035.messages.SRobotInfo;
import team035.robots.BaseRobot;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.PowerNode;
import battlecode.common.RobotInfo;

public class StateCache {
	public static final int MAX_ROBOTS = 64;
	public static final int MAX_ARCHONS = 6;

	protected BaseRobot r;



	// this will eventually contain
	// 1. arrays of known robots (enemy + friendly)
	// 2. terrain
	// 3. map boundaries
	// 4. tower knowledge (what's open
	// 5. friendly archon states
	// 6. overall team strategy



	// ---- robots -------- //
	protected RobotInfo[] robots;
	protected RobotInfo[] friendlyRobots;
	protected RobotInfo[] enemyRobots;
	protected SRobotInfo[] remoteRobots;
	
	public int numRobotsInRange;
	public int numFriendlyRobotsInRange;
	public int numEnemyRobotsInRange;
	public int numRemoteRobots;

	protected MapLocation nearestFriendlyArchon;
	protected MapLocation[] friendlyArchonLocs;

	public StateCache(BaseRobot r) {
		this.r = r;
	}

	public void newRound() {
		// for now, flush all our state each round.
		robots = new RobotInfo[MAX_ROBOTS];
		numRobotsInRange = 0;
		numFriendlyRobotsInRange = 0;
		numEnemyRobotsInRange = 0;
		numRemoteRobots = 0;

		friendlyRobots = new RobotInfo[MAX_ROBOTS];
		enemyRobots = new RobotInfo[MAX_ROBOTS];
		remoteRobots = new SRobotInfo[MAX_ROBOTS];

		friendlyArchonLocs = null;
	}

	public void addRemoteRobot(SRobotInfo robotInfo) {
		this.remoteRobots[numRemoteRobots] = robotInfo;
		numRemoteRobots++;
	}
	
	public SRobotInfo[] getRemoteRobots() {
		return this.remoteRobots;
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

	// ------------ SIMPLE CACHING METHODS ---------------- //
	public MapLocation[] getFriendlyArchonLocs() {
		if(this.friendlyArchonLocs==null) {
			this.friendlyArchonLocs = this.r.getRc().senseAlliedArchons();
		}

		return this.friendlyArchonLocs;
	}

	public MapLocation getNearestFriendlyArchon() {
		if(this.nearestFriendlyArchon == null) {
			MapLocation[] alliedArchons = this.getFriendlyArchonLocs();
			MapLocation ourLocation = this.r.getRc().getLocation();
			double d2 = 10000000;
			for(MapLocation archonLoc : alliedArchons) {
				double distanceToArchon = ourLocation.distanceSquaredTo(archonLoc); 
				if(distanceToArchon < d2) {
					d2 = distanceToArchon;
					this.nearestFriendlyArchon= archonLoc;
				}
			}
		}
		
		return this.nearestFriendlyArchon;
	}



	//TODO Need to cache this
	public boolean canMove(Direction direction) {
		return this.r.getRc().canMove(direction);
	}

	public PowerNode[] senseAlliedPowerNodes() {
		return this.r.getRc().senseAlliedPowerNodes();
	}

	public MapLocation[] senseCapturablePowerNodes() {
		return this.r.getRc().senseCapturablePowerNodes();
	}


}
