package team035.messages;

import java.io.Serializable;

import battlecode.common.RobotType;

public class MessageAddress implements Serializable {
	private static final long serialVersionUID = -4788626538734887703L;

	public enum AddressType {
		BROADCAST,
		ROBOT_ID,
		ROBOT_TYPE
	}

	public AddressType type;
	public int id;
	public RobotType robotType;
	
	public MessageAddress(AddressType t, int id) {
		if(t == AddressType.ROBOT_ID) {
			this.type = t;
			this.id = id;
		} else {
			System.out.println("ERR: Tried to make a MessageAddress with an int and an AddressType other than ROBOT_ID");
		}
	}
	
	public MessageAddress(AddressType t, RobotType robotType) {
		if(t == AddressType.ROBOT_TYPE) {
			this.type = t;
			this.robotType = robotType;
		} else {
			System.out.println("ERR: Tried to make a MessageAddress with a RobotType and an AddressType other than ROBOT_TYPE");
		}
	}
	
	public MessageAddress(AddressType t) {
		if(t == AddressType.BROADCAST) {
			this.type = t;
		} else {
			System.out.println("ERR: Tried to make a MessageAddress with no args and an AddressType other than BROADCAST");
		}
	}
	
	public String toString() {
		
		switch(type) {
			case BROADCAST:
				return "@" + type;
			case ROBOT_ID:
				return "@" + type + "." + id;
			case ROBOT_TYPE:
				return "@" + type + "." + robotType;
			default:
				return "@" + type;
		}
	}
}
