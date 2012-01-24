package team035.modules;

import team035.robots.BaseRobot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class NavController {
	protected enum Mode {
		PATHING, BUGGING, DOCKING
	}

	protected static final Direction[] compass = {
		Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, 
		Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST,
		Direction.WEST, Direction.NORTH_WEST
	};
	protected static final int WITHIN_EPSILON_THRESHOLD = 2;
	
	protected BaseRobot r;
	protected MapLocation target;
	protected MapLocation dockingTarget;
	protected Mode mode;
	protected Mode preBuggingMode;
	protected Direction bugDirection;
	private int epsilon;
	protected MapLocation buggingStartLoc;
	protected static boolean[][][] BLOCK_DIRS;
	protected int[] prohibitedDirs;
	int withinEpsilon = 0;
	private Direction startDesiredDir;
	private boolean hugLeft;
	private boolean goneAround;
	

	public NavController(BaseRobot r) {
		this.r = r;
		this.mode = Mode.PATHING;
		this.bugDirection = Direction.NONE;
		this.target = r.getRc().getLocation();
		this.buggingStartLoc = null;
		this.initBlockDirs();
		this.prohibitedDirs = new int[3];
		reset();
	}

	protected void initBlockDirs () {
		Direction[] directions = Direction.values();
		BLOCK_DIRS = new boolean[directions.length][directions.length][directions.length];
		for(int i = 0; i < directions.length; i++) {
			for(int j = 0; i < directions.length; i++) {
				for(int k = 0; i < directions.length; i++) {
					BLOCK_DIRS[i][j][k] = true;
				}
			}
		}

		for (Direction d: Direction.values()) {
			if (d == Direction.NONE || d == Direction.OMNI)
				continue;
			for (Direction b: Direction.values()) {
				// if d is diagonal, allow all directions
				if (!d.isDiagonal()) {
					// Blocking a dir that is the first prohibited dir, or one
					// rotation to the side
					BLOCK_DIRS[d.ordinal()][b.ordinal()][d.ordinal()] = true;
					BLOCK_DIRS[d.ordinal()][b.ordinal()][d.rotateLeft().ordinal()] = true;
					BLOCK_DIRS[d.ordinal()][b.ordinal()][d.rotateRight().ordinal()] = true;
					// b is diagonal, ignore it
					if (!b.isDiagonal() && b != Direction.NONE && b != Direction.OMNI) {
						// Blocking a dir that is the second prohibited dir, or one
						// rotation to the side
						BLOCK_DIRS[d.ordinal()][b.ordinal()][b.ordinal()] = true;
						BLOCK_DIRS[d.ordinal()][b.ordinal()][b.rotateLeft().ordinal()] = true;
						BLOCK_DIRS[d.ordinal()][b.ordinal()][b.rotateRight().ordinal()] = true;
					}
				}
			}
		}
	}

	protected void resetDocking() {
		this.target = this.dockingTarget.add(Direction.SOUTH);
		this.withinEpsilon = 0;
		this.epsilon = 0;
	}
	
	// use this for changing targets within a docking move
	protected void moveTarget(MapLocation loc) {
		this.target = loc;
	}

	public void setTarget(MapLocation loc) {
		this.setTarget(loc, 0);
	}

	public void setTarget(MapLocation loc, int epsilon) {
		this.target = loc;
		this.epsilon = epsilon;
		this.withinEpsilon = 0;
		this.mode = Mode.PATHING;
	}

	public void setTarget(MapLocation loc, boolean isDocking) {
		if(true == isDocking) {
			this.mode = Mode.DOCKING;
			this.dockingTarget = loc;
			this.resetDocking();
		} else {
			this.mode = Mode.PATHING;
			this.setTarget(loc, 0);
		}
	}

	public MapLocation getTarget() {
		return this.target;
	}

	public Direction getDesiredHeading() {
		MapLocation myLoc = this.r.getRc().getLocation();
		return myLoc.directionTo(this.target);
	}

	public boolean doMove() {
		// fail fast if we're already there
		if(this.isAtTarget()) {
			return false;			
		}
		boolean moved = false;
		RobotController rc = this.r.getRc();
		if(!rc.isMovementActive()) {
			Direction desiredDir = getNextMove();
			if (desiredDir != Direction.NONE && desiredDir != Direction.OMNI) {
				try {

					// Set the direction or move
					if (rc.getDirection() != desiredDir) {
						rc.setDirection(desiredDir);
						return moved;
					} else if (rc.canMove(desiredDir)){
						rc.moveForward();
						return moved;
					}
				} catch(GameActionException e) {
					r.getLog().printStackTrace(e);
					moved = false;
				}
			}
		}
		return moved;
	}


	public boolean isAtTarget() {

		if(mode == Mode.DOCKING ||
			 (mode == Mode.BUGGING && preBuggingMode == Mode.DOCKING)) {
			return this.r.getRc().getLocation().isAdjacentTo(this.dockingTarget); 
		}
		
		int distance = target.distanceSquaredTo(this.r.getRc().getLocation());

		if(distance==0) {
			withinEpsilon = 0;
			return true;
		}
		if(distance <= this.epsilon*this.epsilon) {
			r.getLog().println("Within epsilon. Count: " + withinEpsilon);
			withinEpsilon++;

			if(withinEpsilon >= WITHIN_EPSILON_THRESHOLD) {
				//withinEpsilon = 0;
				return true;
			}
		} else {
			withinEpsilon = 0;
		}

		return false;
	}

	protected boolean isValidMove(Direction dir) {
//		switch (r.getRc().getType()) {
//		case SOLDIER:
			MapLocation[] nodeLocs = r.getRc().senseCapturablePowerNodes();
			MapLocation frontLoc = r.getRc().getLocation().add(dir);
			for (MapLocation loc :nodeLocs) {
				if(frontLoc.equals(loc) || 
					(r.getRc().getType() == RobotType.SOLDIER && frontLoc.equals(loc.add(Direction.SOUTH)))) {
					return false;
				}
			}
			if(!r.getCache().canMove(dir)) {
				return false;
			}
			return true;
//			break;
//		case ARCHON:
//			if(!r.getCache().canMove(dir)) {
//				return false;
//			}
//		}
//		return true;
	}



	protected boolean isClearLoc(MapLocation loc) {
		RobotController rc = this.r.getRc();
		try {
			if(rc.senseObjectAtLocation(loc, RobotLevel.ON_GROUND) == null &&
					rc.senseTerrainTile(loc) == TerrainTile.LAND) {
				return true;
			}
		} catch (GameActionException e) {
			// TODO Auto-generated catch block
			r.getLog().printStackTrace(e);
		}
		return false;
	}


	public Direction getNextMove(){
		RobotController rc = r.getRc();
		MapLocation myLoc = rc.getLocation();
		Direction desiredDir = myLoc.directionTo(target);
		if (desiredDir == Direction.NONE || desiredDir == Direction.OMNI)
			return desiredDir;
		// If we are bugging around an object, see if we have gotten past it
		if (mode == Mode.BUGGING){
			// If we are closer to the target than when we started, and we can
			// move in the ideal direction, then we are past the object
			if (myLoc.distanceSquaredTo(target) < this.buggingStartLoc.distanceSquaredTo(target) && canMove(desiredDir)){
				prohibitedDirs = new int[] {Direction.NONE.ordinal(), Direction.NONE.ordinal()};
				this.goneAround = false;
				mode = this.preBuggingMode;
			}
		}
		// If we are docking, check to see whether our target is blocked, and if so
		// pick an open target next to the docking location
		if(mode == Mode.DOCKING) {
			if(myLoc.distanceSquaredTo(this.target) <= rc.getType().sensorRadiusSquared) {
				if(!isClearLoc(this.target)) {
					for(Direction testDir : compass) {
						MapLocation testLoc = this.dockingTarget.add(testDir);
						if(rc.canSenseSquare(testLoc)) {
							if(isClearLoc(testLoc)) {
								moveTarget(testLoc);
								break;
							}
						}
					}
				}
			}
		}

		
		
		switch(mode){
		case PATHING:
		case DOCKING:
			Direction newDir = flockInDir(desiredDir);
			if (newDir != null)
				return newDir;

			this.preBuggingMode = this.mode;
			mode = Mode.BUGGING;
			buggingStartLoc = myLoc;
			startDesiredDir = desiredDir;
			// intentional fallthrough
		default:
			if (goneAround && (desiredDir == startDesiredDir.rotateLeft().rotateLeft() ||
			desiredDir == startDesiredDir.rotateRight().rotateRight())) {
				prohibitedDirs[0] = Direction.NONE.ordinal();
			}
			if (desiredDir == startDesiredDir.opposite()) {
				prohibitedDirs[0] = Direction.NONE.ordinal();
				goneAround = true;
			}
			Direction moveDir = hug(desiredDir, false);
			if (moveDir == null) {
				moveDir = desiredDir;
			}
			return moveDir;
		}
	}

	private Direction turn(Direction dir){
		return (this.hugLeft ? dir.rotateRight() : dir.rotateLeft());
	}


	private Direction hug (Direction desiredDir, boolean recursed){
		if (canMove(desiredDir)){
			return desiredDir;
		}
		MapLocation myLoc = r.getRc().getLocation();
		Direction myDir = r.getRc().getDirection();

		Direction tryDir = turn(desiredDir);
		MapLocation tryLoc = myLoc.add(tryDir);
		//TODO check for movement off map
		for (int i = 0; i < 8 && !canMove(tryDir) && !isOffMap(tryLoc); i++){
			tryDir = turn(tryDir);
			tryLoc = myLoc.add(tryDir);
		}
		// If the loop failed (found no directions or encountered the map edge)
		//TODO check for movement off map
		if (!canMove(tryDir) || isOffMap(tryLoc)) {
			hugLeft = !hugLeft;
			if (recursed){
				// We've tried hugging in both directions...
				if (prohibitedDirs[0] != Direction.NONE.ordinal() && prohibitedDirs[1] != Direction.NONE.ordinal()) {
					// We were prohibiting certain directions before.
					// try again allowing those directions
					prohibitedDirs[1] = Direction.NONE.ordinal();
					return hug(desiredDir, false);
				} else {
					// Complete failure. Reset the state and start over.
					reset();
					return null;
				}
			}
			// mark recursed as true and try hugging the other direction
			return hug(desiredDir, true);
		}
		// If we're moving in a new cardinal direction, store it.
		if (tryDir != myDir && !tryDir.isDiagonal()) {
			if (turn(turn(Direction.values()[prohibitedDirs[0]])) == tryDir) {
				prohibitedDirs[0] = tryDir.opposite().ordinal();
				prohibitedDirs[1] = Direction.NONE.ordinal();
			} else {
				prohibitedDirs[1] = prohibitedDirs[0];
				prohibitedDirs[0] = tryDir.opposite().ordinal();
			}
		}
		return tryDir;
	}


	private boolean canMove(Direction dir) {
		if (BLOCK_DIRS[prohibitedDirs[0]][prohibitedDirs[1]][dir.ordinal()]) {
			return false;
		}

		//if (r.getRc().canMove(dir)) {
		if(isValidMove(dir)) {
			return true;
		}
		return false;
	}

	private Direction flockInDir(Direction desiredDir){
		Direction[] directions = new Direction[3];
		directions[0] = desiredDir;
		Direction left = desiredDir.rotateLeft();
		Direction right = desiredDir.rotateRight();
		MapLocation myLoc = r.getRc().getLocation();
		boolean leftIsBetter = (myLoc.add(left).distanceSquaredTo(target) < myLoc.add(right).distanceSquaredTo(target));
		directions[1] = (leftIsBetter ? left : right);
		directions[2] = (leftIsBetter ? right : left);
		for (int i = 0; i < directions.length; i++){
			if (canMove(directions[i])){
				return directions[i];
			}
		}
		return null;
	}

	protected void reset() {
		this.prohibitedDirs = new int[3];
		for(int i = 0; i < 3; i++) {
			this.prohibitedDirs[i] = Direction.NONE.ordinal();
		}
	}
	
	protected boolean isOffMap(MapLocation mapLoc) {
		return r.getRc().senseTerrainTile(mapLoc) == TerrainTile.OFF_MAP;
	}
}
