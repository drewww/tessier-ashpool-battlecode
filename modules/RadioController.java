package team035.modules;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Vector;

import team035.messages.MessageAddress;
import team035.messages.MessageWrapper;
import team035.messages.RobotMessage;
import team035.robots.BaseRobot;
import battlecode.common.GameActionException;
import battlecode.common.Message;

public class RadioController {

	protected static final String salt = "a23fo9anaewvaln32faln3falf3";

	protected BaseRobot r;


	protected int numMessagesInQueue = 0;
	protected MessageWrapper[] outgoingMessageQueue;

	public static final int MAX_MESSAGES_PER_TURN = 20;


	// there's one of these for each message type, which has N objects that
	// want to be notified about messages of that type.
	protected HashMap<String, Vector<RadioListener>> listeners = new HashMap<String, Vector<RadioListener>>();

	public RadioController(BaseRobot r) {
		this.r = r;

		this.newRound();
	}


	public void addListener(RadioListener listener, String messageClass) {
		String[] classes = new String[1];
		classes[0] = messageClass;
		this.addListener(listener, classes);
	}

	public void addListener(RadioListener listener, String[] messageClasses) {
		Vector<RadioListener> listenersForClass;

		System.out.println("adding listener: " + listener + " for classes: " + messageClasses);

		for(String c : messageClasses) {
			if(c==null) break;

			System.out.println("adding listener for class: " + c);
			if(listeners.get(c)==null) {
				listenersForClass = new Vector<RadioListener>();
			} else {
				listenersForClass = listeners.get(c);
			}
			listenersForClass.add(listener);
			listeners.put(c, listenersForClass);
		}
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

			int messagesWritten = 0;
			for(MessageWrapper wrapper : this.outgoingMessageQueue) {
				if(wrapper==null) {
					break;
				}

				System.out.println("about to send: " + wrapper);
				out.writeObject(wrapper);
				messagesWritten++;
			}
			Message outgoingMessage = new Message();

			String[] s = {new String(bytesOut.toByteArray())};
			outgoingMessage.strings = s;
			if(messagesWritten > 0) {
				this.r.getRc().broadcast(outgoingMessage);
			}

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
			
			// skip invalid messages
			if(!this.validMessage(m)) continue;
			
			ByteArrayInputStream byteIn = new ByteArrayInputStream(m.strings[0].getBytes());
			try {
				ObjectInputStream in = new ObjectInputStream(byteIn);
				Object nextObject = in.readObject();
	
				if(nextObject.getClass() == MessageWrapper.class) {
					MessageWrapper msg = (MessageWrapper)nextObject;
					if(msg.isForThisRobot()) {
						for(RadioListener l : this.listeners.get(msg.msg.getType())) {
							l.handleMessage(msg);
						}
					}
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
	
	protected boolean validMessage(Message msg) {
		// these tests ensure that the number of fields is right
		// to make this a probable message from our team. 
		if(msg.ints==null) return false;
		if(msg.strings==null) return false;
		if(msg.locations != null) return false;
		
		if(msg.ints.length!=1) return false;
		if(msg.strings.length != 1) return false;

		// now do the more rigorous check - does the hashcode of the message string
		// match the int in the ints field?
		StringBuilder builder = new StringBuilder();
		builder.append(msg.strings[0]);
		builder.append(RadioController.salt);
		if(builder.toString().hashCode()!=msg.ints[0]) return false;

		return true;
	}
}
