package com.sbg.lwc;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

public class Item {		
	
	State state = State.Alive;
	int elapsedTime = 0;
	
	// Dying State Animations	
	final float DYING_GROWTH_FACTOR = 0.5f;
	public int durationOfDyingState = 500; // milliseconds
	public ScaleType scaleTypeDying = ScaleType.Grow; // Grow, Shrink or None
	public boolean fadeOutDying = true; 
	float angleChangeDying = 0;
	float scaleBeforeDying = 1;
	float opacityBeforeDying = 1;
		
	// Matrix
	Matrix matrix = new Matrix();	
	
	// Properties
	float opacity = 1; 
	float scale = 1;
	float angle = 0; // current angle in degrees	
	float angleChange = 0; // angle change per update in degrees
	Vector velocity = new Vector(); // holds direction and magnitude components of velocity vector	
	
	// Position & Size
	RectF bounds = new RectF();
	RectF scaledBounds = new RectF();
	Rect textBounds = new Rect();		
	
	// Item Type	
	ItemType itemType = ItemType.Bitmap;
	Bitmap bitmap;
	Movie movie;
	String text = ":)";		
	Paint paint = new Paint();		
	int streakCounter = 0;
	
	public boolean hasEnteredScreen = false;
	boolean streakItem;
	int rotationPeriod = 0;
	public int resourceIndex = 0;
	
	// constructor
	public Item() {}	
	
	public void setDrawable(Object item) {
		if (item == null) return;
		
		if (item instanceof Bitmap) {
			itemType = ItemType.Bitmap;
			bitmap = (Bitmap)item;
			bounds.set(0,0,bitmap.getWidth(), bitmap.getHeight());
		}
		else if (item instanceof Movie) {
			itemType = ItemType.Gif;
			movie = (Movie)item;		
			bounds.set(0,0,movie.width(), movie.height());				
		}
		else if (item instanceof String) {
			itemType = ItemType.Text;
			text = item.toString();
			calcTextBounds();			
			bounds.set(-textBounds.left, -textBounds.top + textBounds.bottom, -textBounds.left + textBounds.width(), -textBounds.top + textBounds.bottom + textBounds.height());			
		}
		calcScaledBounds();
	}

	public Vector getVelocity() {
		return velocity;
	}
	
	private void calcTextBounds() {		
		this.paint.getTextBounds(this.text, 0, this.text.length(), textBounds);	
	}
	
	public void setPaint(Paint paint) {		
		this.paint.set(paint);	
		this.paint.setAntiAlias(true);
		this.paint.setFilterBitmap(true);
	}
	
	public void setSpeed(float speed) {
		this.velocity.setMagnitude(speed);
	}
	
	public void setPosition(float x, float y) {
		bounds.offsetTo(x, y);
		calcScaledBounds();
	}
	
	public void setDurationOfDyingState(int duration) {
		this.durationOfDyingState = duration;
	}		

	public int getDurationOfDyingState() {  
		return this.durationOfDyingState;
	}
	
	public void setScale(float scale) {
		this.scale = scale;		
		calcScaledBounds();
	}	
	
	private void calcScaledBounds() {
		float scaledX = bounds.left + (bounds.width() * (1-scale)) / 2;
		float scaledY = bounds.top + (bounds.height() * (1-scale)) / 2;		
		scaledBounds.set(scaledX, scaledY, scaledX + bounds.width() * scale, scaledY + bounds.height() * scale);	
	}
	
	public RectF getScaledBounds() { return scaledBounds; }
	
	public void setAngle(float angle) {
		this.angle = angle;
	}
	
	public void setRotationPeriod(int rotationPeriod) {
		this.rotationPeriod = rotationPeriod;
		if (rotationPeriod == 0) { angleChange = 0; return; }
		this.angleChange = 360f / (rotationPeriod / SBLiveWallpaper.UPDATE_PERIOD);
	}
	
	public void setRotationPeriodDying(int rotationPeriod) {
		if (rotationPeriod == 0) { angleChangeDying = 0; return; }
		this.angleChangeDying = 360f / (rotationPeriod / SBLiveWallpaper.UPDATE_PERIOD);
	}
	
	public void setOpacity(float opacity) {
		this.opacity = opacity;
		paint.setAlpha((int)(255 * opacity));
	}
		
	public void setAlive() {		
		elapsedTime = 0;
		state = State.Alive;
		streakItem = false;
	}
	
	public void setInactive() {
		this.state = State.Inactive;
	}
	
	public void setDying(int streakCounter) {		
		scaleBeforeDying = scale;
		opacityBeforeDying = opacity;
		elapsedTime = 0;		
		state = State.Dying;
	}
	
	public void setDead() {		
		state = State.Dead;
	}
		
	public void updateMatrix() {		
		matrix.reset();		
		matrix.preTranslate(bounds.left, bounds.top);			
		matrix.postScale(scale, scale, bounds.centerX(), bounds.centerY());
		matrix.postRotate(angle, bounds.centerX(), bounds.centerY());		
	}
	
	public void update(long elapsedTime) {	
		if (state == State.Dead) return;
		
		// update state timer
		this.elapsedTime += elapsedTime;
				
		if (state == State.Alive) {
			// update position
			this.setPosition(this.bounds.left + velocity.getX(), this.bounds.top + velocity.getY());
			
			// update angle (rotation)
			angle += angleChange;
			
			// update Gif animation
			 if (movie != null && itemType == ItemType.Gif) {						
				//if (this.elapsedTime >= movie.duration()) this.elapsedTime -= movie.duration();		
				if (movie.duration() != 0) movie.setTime(this.elapsedTime % movie.duration());
			 }
		}		
		else if (state == State.Dying) { 

			// check if dying state has expired
			if (this.elapsedTime >= durationOfDyingState)	{
				state = State.Dead;	 
				return;
			}
			
			// update position
			this.setPosition(this.bounds.left + velocity.getX(), this.bounds.top + velocity.getY());
						
			// update angle (rotation)								
			angle += angleChange;
			
			// update scale
			if (scaleTypeDying == ScaleType.Grow) {
				float scale = scaleBeforeDying + (DYING_GROWTH_FACTOR) * this.elapsedTime / (float)durationOfDyingState;
				setScale(scale);
			}
			else if  (scaleTypeDying == ScaleType.Shrink) {
				float scale = scaleBeforeDying*(1 - (this.elapsedTime / (float)durationOfDyingState));
				setScale(scale);
			}	
			
			// update opacity
			if (fadeOutDying) {
				float opacity = opacityBeforeDying * (1 - (this.elapsedTime / (float)durationOfDyingState));
				setOpacity(opacity);
			}			

		}
		
		// update matrix
		updateMatrix();		
	}
	
	public void updateGifTiming() {		
		// update Gif animation
		 if (movie != null && itemType == ItemType.Gif) {						
			//if (this.elapsedTime >= movie.duration()) this.elapsedTime -= movie.duration();		
			 if (movie.duration() != 0) movie.setTime(this.elapsedTime % movie.duration());		
		 }	
	}
	
	public void draw(Canvas c) {
		
		if (state == State.Dead || state == State.Inactive) return;
		//c.drawRect(scaledBounds, this.paint);
		c.save();
		c.concat(matrix);	
		
		switch(itemType) {
			case Bitmap: {
				if (bitmap != null)
					c.drawBitmap(bitmap, 0, 0, paint);
				break;
			}
			case Gif: {
				 if (movie != null)
				 	movie.draw(c, 0, 0, paint);		
				 break;
			}
			case Text: {	
				if (text != null) 						
					c.drawText(this.text, 0, -textBounds.top, this.paint);	
				 break;
			}		
			default: {				
			}
		}				
		c.restore();
	}		
	
	
	public void setDirection(float angle) {			
		velocity.setAngle(angle);
	}
	
	public float getDirection() {
		return velocity.getAngle();
	}
	
	public float getSpeed() {
		return velocity.getMagnitude();
	}
	
	public int getRotationPeriod() {
		return rotationPeriod;
	}
	
	public State getState() {
		return state;
	}	
	
	public int getStreak() {
		return this.streakCounter;
	}
    
	public ItemType getItemType() {
		return this.itemType;
	}	
	
	public int getGifDuration() {
		if (itemType == ItemType.Gif) return movie.duration();
		return 0;
	}
	
    enum ItemType {
    	Bitmap, Gif, Text
    }
    
    enum State {
    	Alive, Dying, Dead, Inactive
    }
    
    enum ScaleType {
    	None, Grow, Shrink
    }
}