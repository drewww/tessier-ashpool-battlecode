package team035.messages;

import battlecode.common.MapLocation;

public class AssaultOrderMessage extends BaseMessage {

	public static final String type = "ASSAULT_ORDER";
	
	public MapLocation moveTo;
	
	public AssaultOrderMessage(MapLocation l) {
		this.moveTo = l;
	}
	
	@Override
	public String getType() {
		return AssaultOrderMessage.type;
	}

}
