package team035;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class Vector2d {
	public double x;
	public double y;
	
	public Vector2d(double x,double y) {
		this.x = x;
		this.y = y;
	}
	
	public Vector2d() {
		this.x = 0;
		this.y = 0;
	}
	
	public Vector2d(MapLocation l) {
		this.x = l.x;
		this.y = l.y;
	}
	
	public static Vector2d sub(Vector2d a, Vector2d b) {
		return new Vector2d(a.x - b.x, a.y-b.y);
	}
	
	public static Vector2d sub(MapLocation a, MapLocation b) {
		return new Vector2d(a.x - b.x, a.y-b.y);
	}
	
	public void add(Vector2d v) {
		this.x += v.x;
		this.y += v.y;
	}
	
	public void sub(Vector2d v) {
		this.x -= v.x;
		this.y -= v.y;
	}
	
	public void normalize() {
		// might be cheaper to do atan rather than sqrt + 2 powers, but we'll see
		//	double angle = Math.atan(this.y/this.x);
		
		double mag = this.mag();
		
		this.x = this.x / mag;
		this.y = this.y / mag;
	}
	
	public void scale(double factor) {
		this.x *= factor;
		this.y *= factor;
	}
	
	public Direction getDirection() {
		double angle = Math.atan2(this.y,this.x);
		
		if(angle == Double.NaN) {
			return Direction.NONE;
		}
		
		if(angle < 0) {
			angle += Math.PI*2;
		}
		
		double increment = Math.PI/8;
		
		// this feels like a stupid way to do this but it's the first thing that came to mind,
		// sooooo yeah. whatever.
		
		for(int i=0; i< 16; i++) {
			if(angle >= (increment*i) && angle < (increment*(i+1))) {
				switch(i) {
				case 15:
				case 0:
					return Direction.EAST;
				case 1:
				case 2:
					return Direction.SOUTH_EAST;
				case 3:
				case 4:
					return Direction.SOUTH;
				case 5:
				case 6:
					return Direction.SOUTH_WEST;
				case 7:
				case 8:
					return Direction.WEST;
				case 9:
				case 10:
					return Direction.NORTH_WEST;
				case 11:
				case 12:
					return Direction.NORTH;
				case 13:
				case 14:
					return Direction.NORTH_EAST;
				default:
					return Direction.NONE;
				}
			}	
		}
		
		return Direction.NONE;
	}
	
	public String toString() {
		return "{" + this.x + ", " + this.y + "}";
	}
	
	public double mag() {
		return Math.sqrt(Math.pow(this.x, 2) + Math.pow(this.y, 2));
	}
	
	public static void main(String args[]) {
		// do some simple tests
		for(int i=0; i<36; i++) {
			double angle = Math.toRadians(i*10);
			Vector2d v = new Vector2d(Math.cos(angle), Math.sin(angle));
			System.out.println(angle + ": " + v.getDirection());
		}
		
	}
}
