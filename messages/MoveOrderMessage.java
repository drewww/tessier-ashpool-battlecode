package team035.messages;

import battlecode.common.MapLocation;

public class MoveOrderMessage extends BaseMessage {

	public static final String type = "MOVE_ORDER";
	
	public MapLocation moveTo;
	
	public MoveOrderMessage(MapLocation l) {
		this.moveTo = l;
	}
	
	@Override
	public String getType() {
		return MoveOrderMessage.type;
	}

}
