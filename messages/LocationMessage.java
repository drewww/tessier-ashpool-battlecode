package team035.messages;

import battlecode.common.MapLocation;
import battlecode.common.Robot;

public class LocationMessage extends BaseMessage {
	private static final long serialVersionUID = 8203251897804898196L;

	public MapLocation loc;
	public SerializableRobot robot;
	
	public LocationMessage(Robot robot, MapLocation location) {
		this.robot = new SerializableRobot(robot);
		this.loc = location;
	}

	public String toString() {
		return "{loc: " + loc + ", info: " + robot + "}";
	}
}
