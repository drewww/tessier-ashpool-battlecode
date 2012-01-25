package team035.modules;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import team035.robots.BaseRobot;

public class Log {
	BaseRobot robot;
	boolean recording = false; 
	
	public Log(BaseRobot r) {
		this.robot = r;
	}
	
	public void println(String text) {
		System.out.println(text);
		if(recording) {
			robot.getRc().addMatchObservation(text + "\n");
		}
	}

	public void print(String text) {
		System.out.print(text);
		if(recording) {
			robot.getRc().addMatchObservation(text);
		}
	}

	public void printStackTrace(Throwable throwable) {
		Writer result = new StringWriter();
    PrintWriter printWriter = new PrintWriter(result);
    throwable.printStackTrace(printWriter);
    String message = result.toString();
    System.out.println(message);
    if(true) {
    	robot.getRc().addMatchObservation(message);
    }
	}
	
	
}
