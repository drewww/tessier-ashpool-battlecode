package team035.messages;

import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotLevel;


public class LowFluxMessage extends BaseMessage {
	private static final long serialVersionUID = -2576649751669705786L;

	public static final String type = "LOW_FLUX";
	
	public SerializableRobot from;
	public MapLocation loc;
	public RobotLevel level;
	
	public LowFluxMessage(Robot robot, MapLocation loc, RobotLevel level) {
		this.from = new SerializableRobot(robot);
		this.loc = loc;
		this.level = level;
	}
	
	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return type;
	}
	
}
