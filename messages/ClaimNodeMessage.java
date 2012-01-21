package team035.messages;

import battlecode.common.MapLocation;

public class ClaimNodeMessage extends BaseMessage {

	private static final long serialVersionUID = 3896770736151563370L;
	public static final String type = "CLAIM_NODE";
	public MapLocation loc;
	
	public ClaimNodeMessage(MapLocation loc) {
		this.loc = loc;
	}
	
	public String toString() {
		return "{loc: " + this.loc + "}";
	}
	
	@Override
	public String getType() {
		return type;
	}

}
