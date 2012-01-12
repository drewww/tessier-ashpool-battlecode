package team035.modules;

import team035.robots.BaseRobot;

public class StateCache {
	protected BaseRobot r;
	
	// this will eventually contain
	// 1. arrays of known robots (enemy + friendly)
	// 2. terrain
	// 3. map boundaries
	// 4. tower knowledge (what's open
	// 5. friendly archon states
	// 6. overall team strategy
	
	public StateCache(BaseRobot r) {
		this.r = r;
	}
	
}
