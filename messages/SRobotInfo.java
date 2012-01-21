package team035.messages;

import java.io.Serializable;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class SRobotInfo implements Serializable {
	/**
	 * foo
	 */
	private static final long serialVersionUID = 1L;
	public Direction direction;
	public double energon;
	public double flux;
	public MapLocation location;
	public SRobot robot;
	public RobotType type;
	public Team team;
	
	public SRobotInfo(RobotInfo info) {
		this.direction = info.direction;
		this.energon = info.energon;
		this.flux = info.flux;
		this.location = info.location;
		this.robot = new SRobot(info.robot);
		this.type = info.type;
	}
	
	public RobotInfo toRobotInfo() {
		return new RobotInfo(null, location, energon, energon, direction, type, team, false);
	}
}
