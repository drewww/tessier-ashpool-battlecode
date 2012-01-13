package team035.modules;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import team035.messages.MessageAddress;
import team035.messages.MessageWrapper;
import team035.messages.RobotMessage;
import team035.robots.BaseRobot;
import battlecode.common.GameActionException;
import battlecode.common.Message;

public class RadioController {
	
	protected BaseRobot r;
	
	
	protected int numMessagesInQueue = 0;
	protected MessageWrapper[] outgoingMessageQueue;
	
	public static final int MAX_MESSAGES_PER_TURN = 20;
	
	public RadioController(BaseRobot r) {
		this.r = r;
		
		this.newRound();
	}
	
	public void addMessageToTransmitQueue(MessageAddress adr, RobotMessage m) {
		outgoingMessageQueue[numMessagesInQueue] = new MessageWrapper(adr, m);
		numMessagesInQueue++;
	}
	
	// Sends the messages we've queued up over the course of this turn.
	public void transmit() {
		try {
			ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bytesOut);
		for(MessageWrapper wrapper : this.outgoingMessageQueue) {
			if(wrapper==null) {
				break;
			}

			System.out.println("about to send: " + wrapper);
			out.writeObject(wrapper);	
		}
		Message outgoingMessage = new Message();
		
		String[] s = {new String(bytesOut.toByteArray())};
		outgoingMessage.strings = s;
		
			this.r.getRc().broadcast(outgoingMessage);
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GameActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// clean out the queue
		this.newRound();
	}
	
	private void newRound() {
		this.outgoingMessageQueue = new MessageWrapper[MAX_MESSAGES_PER_TURN];
		numMessagesInQueue=0;
	}
	
	// Handles any incoming messages we received this turn.
	// If they pass basic validation, are addressed to us, and we have
	// any listeners on that message type, alert the listeners to the
	// existence of the message.
	public void receive() {
		Message[] messages = this.r.getRc().getAllMessages();
		System.out.println("received " + messages.length + " messages");
		for(Message m : messages) {
			ByteArrayInputStream byteIn = new ByteArrayInputStream(m.strings[0].getBytes());
			try {
				ObjectInputStream in = new ObjectInputStream(byteIn);
				
				Object nextObject = in.readObject();
				
				if(nextObject.getClass() == MessageWrapper.class) {
					System.out.println("got message: " + (nextObject));
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
