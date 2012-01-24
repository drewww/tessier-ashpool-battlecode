package team035.modules;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import team035.robots.BaseRobot;

public class Log {
	BaseRobot robot;
	
	public Log(BaseRobot r) {
		this.robot = r;
	}
	
	public void println(String text) {
		System.out.println(text);
		robot.getRc().addMatchObservation(text + "\n");
	}

	public void print(String text) {
		System.out.print(text);
		robot.getRc().addMatchObservation(text);
	}

	public void printStackTrace(Throwable throwable) {
		Writer result = new StringWriter();
    PrintWriter printWriter = new PrintWriter(result);
    throwable.printStackTrace(printWriter);
    String message = result.toString();
    System.out.println(message);
    robot.getRc().addMatchObservation(message);
	}
	
	
}
