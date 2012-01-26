package team035.messages;

import battlecode.common.Direction;

public class ScoutOrderMessage extends BaseMessage {

	public static final String type = "SCOUT_ORDER";
	
	// This message is to tell a scout which way to scout. This is
	// specified as a wall and a direction. 
	
	public Direction scoutDirection;
	public boolean clockwise = false;
	
	public ScoutOrderMessage(Direction scout, boolean clockwise) {
		this.scoutDirection = scout;
		this.clockwise = clockwise;
	}
	
	@Override
	public String getType() {
		return type;
	}

}
