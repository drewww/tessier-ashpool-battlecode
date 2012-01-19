package team035.messages;

public class ClaimNodeMessage extends BaseMessage {

	private static final long serialVersionUID = 3896770736151563370L;
	public static final String type = "CLAIM_NODE";
	
	
	@Override
	public String getType() {
		return type;
	}

}
