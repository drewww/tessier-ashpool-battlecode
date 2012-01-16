package team035.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class MessageWrapper implements Serializable {
	private static final long serialVersionUID = -4253382043883526501L;

	public MessageAddress adr;
	public RobotMessage msg;

	public MessageWrapper(MessageAddress newAdr, RobotMessage newMsg) {
		adr = newAdr;
		msg = newMsg;
	}

	public String toString() {
		return adr + " " + msg;
	}
	
	public boolean isForThisRobot() {
		return this.adr.isForThisRobot();
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		// we know that when deserializing this object there will be two objects in a row.
		// the first object is the messageaddress, which will tell us if we need to de-serialize
		// the second one. 
		System.out.println("Trying to read a messagewrapper");
		this.adr = (MessageAddress) in.readObject();
		System.out.println("Got addr: " + this.adr);
		if(this.adr.isForThisRobot()) {
			this.msg = (RobotMessage) in.readObject();
		} else {
			in.skipBytes(in.available());
			this.msg = null;
		}
	}
}
