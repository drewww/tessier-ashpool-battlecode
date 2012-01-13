package team035.messages;

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
}
