package team035.messages;

import battlecode.common.RobotInfo;

public class RobotInfosMessage extends BaseMessage {
	private static final long serialVersionUID = 1207748991833906049L;

	public static final String type = "ROBOT_LOCATIONS";
	
	public boolean friendly;
	
	// going to have to make a wrapper for this to make it work with messages.
	public SRobotInfo[] robots; 
	
	public RobotInfosMessage (RobotInfo[] robots) {
		this.robots = new SRobotInfo[robots.length];
		int i=0;
		for(RobotInfo ri : robots) {
			this.robots[i] = new SRobotInfo(ri);
			i++;
		}
	}
	
	public String getType() {
		// TODO Auto-generated method stub
		return type;
	}
}
