package team035.modules;

import team035.messages.MessageAddress;
import team035.messages.RobotInfosMessage;
import team035.robots.BaseRobot;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class RadarController {
	protected BaseRobot r;
	protected RobotController rc;

	protected StateCache cache;

	protected boolean enabled = true;
	protected boolean enemyTargetBroadcast = false;

	public RadarController (BaseRobot r) {
		this.r = r;
		this.rc = this.r.getRc();
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setEnemyTargetBroadcast(boolean enemyTargetBroadcast) {
		this.enemyTargetBroadcast = enemyTargetBroadcast;
	}

	public void scan() {
		if(!enabled) return;

		this.cache = r.getCache();

		// looks around the robot and updates the state cache
		for(Robot robot : this.rc.senseNearbyGameObjects(Robot.class)) {
			// this will be all robots, friendly and enemy

			try {
				RobotInfo info = this.rc.senseRobotInfo(robot);
				this.cache.addRobot(info);
			} catch (GameActionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if(this.enemyTargetBroadcast && this.cache.numEnemyRobotsInRange > 0) {
			RobotInfosMessage msg = new RobotInfosMessage(this.cache.getEnemyRobots());
			this.r.getRadio().addMessageToTransmitQueue(new MessageAddress(MessageAddress.AddressType.BROADCAST), msg);
		}
	}

	// returns the MapLocation of the highest value target that's in range of our
	// weapons. This depends on the unit type - they each have different targetting
	// priorities.
	public RobotInfo acquireTarget() {
		System.out.println("acquiring target!");
		// first, get a list of targets in our attack range.
		RobotInfo[] potentialTargets = r.getCache().getEnemyRobotsInAttackRange();
//		double[] targetScores = new double[potentialTargets.length];
		
		// check some really simple cases
		if(potentialTargets.length==0) return null;
		if(potentialTargets.length==1) return potentialTargets[0];

		int i= 0;
		int curMaxScore = -1;
		int curMaxIndex = 0;
		for(RobotInfo potentialTarget : potentialTargets) {
			double score;
			
			switch(r.getRc().getType()) {
			case SCOUT:
				score = this.calculateScoreScout(potentialTarget);
				break;
			case DISRUPTER:			
				score = this.calculateScoreDisrupter(potentialTarget);
				break;
			case SOLDIER:
				score = this.calculateScoreSoldier(potentialTarget);
				break;
				// prefers archons
				// prefers robots with low flux, exclude robots with no flux
				// prefers scorchers
				// prefers soldiers
				// prefers 
				// prefers robots with flux, picks randomly
			default:
				score = 0;
				break;
			}	
			
			if(score > curMaxScore) {
				curMaxIndex = i;
			}
		}
		
		return potentialTargets[curMaxIndex];
	}
	
	protected double calculateScoreScout(RobotInfo potentialTarget) {
		// scouts prefer archons above all, and then 
		// targets with >0 flux, but low flux.
		double score = 0;
		if(potentialTarget.flux < 0.5) return score;

		switch(potentialTarget.type) {
		case ARCHON:
			score += 1000;
			break;
		case SCORCHER:
			score += 300;
			break;
		case SOLDIER:
		case DISRUPTER:
		case SCOUT:
			score+= 100;
			break;
		case TOWER:
			score=0;
			break;
		}
		
		
		// now add a bonus for low flux 
		score += 3*(potentialTarget.type.maxFlux - potentialTarget.flux);
		
		// add a bonus for high energon
		score += potentialTarget.energon;
		
		return score;
	}
	
	protected double calculateScoreDisrupter(RobotInfo potentialTarget) {
		double score = 0;
		if(potentialTarget.flux < 0.5) return score;

		switch(potentialTarget.type) {
		case SCORCHER:
			score += 100;
			break;
		case SOLDIER:
			score += 50;
			break;
		case DISRUPTER:
		case SCOUT:
			score+= 100;
			break;
		case ARCHON:
		case TOWER:
			score=-1000;
			break;
		}
		
		// add a random bonus. This helps disrupters spread their fire when
		// they have multiple targets.
		// (randomness is unreliable atm, so we'll just assume that there's
		// some randomness inherent in the order in which we traverse the list
		// and we're returning the first one to break ties)
		
		return score;
	}
	
	protected double calculateScoreSoldier(RobotInfo potentialTarget) {
		// given a robot info, figure out its score.
		// prefers robots with low energon
		double score = 0;

		if(potentialTarget.flux < 0.5) return score;
		// prefers archons
		// prefers scorchers

		switch(potentialTarget.type) {
		case ARCHON:
			score += 1000;
			break;
		case SCORCHER:
			score += 500;
			break;
		case SOLDIER:
			score += 300;
			break;
		case SCOUT:
			score += 100;
			break;
		case DISRUPTER:
			score += 100;
			break;
		case TOWER:
			break;
		}
		
		// add in bonus for low health and high flux
		score += potentialTarget.type.maxEnergon - potentialTarget.energon;
		
		score += potentialTarget.flux;
		
		System.out.println("score: " + score);
		return score;
	}
}
