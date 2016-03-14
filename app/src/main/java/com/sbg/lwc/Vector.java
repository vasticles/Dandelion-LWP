package com.sbg.lwc;

/*
 * Vector Class: manages angle and magnitude of a vector. Contains helpful methods for accessing x,y components of it.
 */
public class Vector {

	private float x, y; // unit components of vector	
	private float angle = 0; 
	private float angleInRad;
	private float magnitude;
		
	public Vector() {	
		calcVectorComponents();
	} 
	
	public Vector(float angle, float magnitude) {
		setAngle(angle);
		setMagnitude(magnitude);	
	}	
	 
	public float getAngle() { return this.angle; }
	
	public void setAngle(float angle) {
		if (angle == this.angle) return;
		this.angle = angle;		
		angleInRad = (float)(angle * Math.PI / 180f);
		calcVectorComponents();
	}
	
	public float getMagnitude() { return this.magnitude; }	
	
	public void setMagnitude(float magnitude) {
		if (magnitude == this.magnitude) return;
		this.magnitude = magnitude;
		// TODO: convert to density independent pixels per second
		calcVectorComponents();
	}
	
	private void calcVectorComponents() {
		this.x = (float)Math.cos(angleInRad) * magnitude;
		this.y = -(float)Math.sin(angleInRad) * magnitude;
	}
	
	public float getAngleInRadians() { return angleInRad; }
	
	public float getX() { return this.x; }
	public float getY() { return this.y; }
}
