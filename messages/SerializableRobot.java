package team035.messages;

import java.io.Serializable;

import battlecode.common.Robot;
import battlecode.common.RobotLevel;
import battlecode.common.Team;

public class SerializableRobot implements Serializable {
	private static final long serialVersionUID = 159853186226292139L;
	
	public Team team = null;
	public RobotLevel level = null;
	public int id = -1;

	public SerializableRobot (Robot r) {
		
		if(r==null) return;
		
		this.team = r.getTeam();
		this.level = r.getRobotLevel();
		this.id = r.getID();
	}
}
