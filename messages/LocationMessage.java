package team035.messages;

import battlecode.common.MapLocation;
import battlecode.common.Robot;

public class LocationMessage extends BaseMessage {
	private static final long serialVersionUID = 8203251897804898196L;

	public static final String type = "LOC";
	public MapLocation loc;
	public SRobot robot;
	
	public LocationMessage(Robot robot, MapLocation location) {
		this.robot = new SRobot(robot);
		this.loc = location;
	}

	public String toString() {
		return "{loc: " + loc + ", info: " + robot + "}";
	}

	@Override
	public String getType() {
		return type;
	}
}
