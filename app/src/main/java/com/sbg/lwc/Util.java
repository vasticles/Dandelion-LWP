package com.sbg.lwc;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Debug;
import android.util.DebugUtils;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;


public class Util {
	
	private static Context context;	
	private static int width;
	private static int height;	
	
	private static LinkedHashMap<String,String> debugOutput = new LinkedHashMap<String,String>();
	private static Paint whitePaint = new Paint();
	
	public static void init(Context mcontext, int cwidth, int cheight) {
		context = mcontext;
		width = cwidth;
		height = cheight;	
		
		debugOutput.clear();
		initPaint();
	}		
	
	private static void initPaint() {
		whitePaint.setColor(Color.WHITE);
		whitePaint.setTextSize(20);		
	}	

    public static Bitmap decodeSampledBitmapFromResource(int resId, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), resId, options);

        // Calculate inSampleSize
        float sampleSize = calculateInSampleSize(options, reqWidth, reqHeight);        
         
        Log.d("----> Sample Size:" , sampleSize + "");
        if (sampleSize < 1) { // return a scaled UP version of image
        	Bitmap bg = BitmapFactory.decodeResource(context.getResources(), resId);
        	Bitmap scaledBitmap = Bitmap.createScaledBitmap(bg, (int)(bg.getWidth() / sampleSize), (int)(bg.getHeight() / sampleSize), true);
        	bg.recycle();
        	return scaledBitmap; 
        }
        else { // return a scaled DOWN version of image
        	options.inSampleSize = Math.round(sampleSize);
	        // Decode bitmap with inSampleSize set
	        options.inJustDecodeBounds = false;
	        return BitmapFactory.decodeResource(context.getResources(), resId, options);
        }        
    }
    
    public static float calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
	    // Raw height and width of image
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    float inSampleSize = 1;
	
	    if (height >= reqHeight || width >= reqWidth) {
	        if (width > height) {
	            inSampleSize = height / (float)reqHeight;
	        } 
	        else {
	            inSampleSize = width / (float)reqWidth;
	        }
	        
	        
	    }
	    else {	    	
	    	inSampleSize =  Math.min((float)width / reqWidth, (float)height / reqHeight);
	    }
	    return inSampleSize;
	}
		
	// returns a random integer between min and max inclusive
	public static int randomInt(int min, int max) {
		return min + (int) (Math.random() * (max - min + 1));
	}

	public static int randomInt(float min, float max) {
		return (int) min + (int) (Math.random() * ((int) max - (int) min + 1));
	}
	
	public static boolean intersects(float x, float y, RectF rect) {
    	return (rect.left <= x && rect.right >= x && rect.top <= y && rect.bottom >= y);    		
	}
	
	public static boolean intersects(PointF point, RectF rect) {
		return intersects(point.x, point.y, rect);
	}
	
	// Convert the dips to pixels
	public static int dipToPixels(float dipSize) {				
		float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dipSize * scale + 0.5f);			
	}
			
	// helper function - returns a String array of filenames of all files inside a given asset folder
	public static String[] getAssetFilenames(String assetFolderPath) {			
        try {
			return context.getAssets().list(assetFolderPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   	
        return new String[0];
	}	
	
	public static String getStringResource(String name) {
		String str = "";

		try {
			str = context.getResources().getString(
					context.getResources().getIdentifier(name, "string",
							context.getPackageName()));
		} catch (Exception ex) {
		}

		return str;
	}
	
	// this method displays the debug output on the screen
	public static void drawDebugOutput(Canvas c) {	
		int vertical_spacing = 0;		
		
		for (String key : debugOutput.keySet()) {			
			c.drawText(key + ": " + debugOutput.get(key), 20, 200 + vertical_spacing, whitePaint);			
			vertical_spacing += whitePaint.getTextSize();			
		}	
	}
	public static  void log(String key, double value) {
		removeLog(key);
		debugOutput.put(key, value + "");
	}	
	
	public static  void log(String key, String value) {
		removeLog(key);
		debugOutput.put(key, value);
	}	
	
	public static  void log(String key, Object value) {
		removeLog(key);
		debugOutput.put(key, value.toString());
	}	

	public static void log(String key, int[] ar) {
		debugOutput.put(key, Arrays.toString(ar));
	}	
	
	public static void log(String key, String[] ar) {
		debugOutput.put(key, Arrays.toString(ar));
	}		
	
	public static void log(String key, RectF rect) {
		debugOutput.put(key, rect.left + ", " + rect.top + ", " + rect.right + ", " + rect.bottom + " : " + rect.width() + ", " + rect.height());
	}	
	
	public static void log(String key, Rect rect) {
		debugOutput.put(key, rect.left + ", " + rect.top + ", " + rect.right + ", " + rect.bottom + " : " + rect.width() + ", " + rect.height());
	}	
	
	public static void removeLog(String key) {
		debugOutput.remove(key);
	}
	
    // helper function that returns an integer array when given a comma delimited string of integers
	public static int[] getIntArray(String str) {    	
    	if (str.length() == 0) return new int[0];
    	
    	String[] list = str.split(",");
    	int[] ar = new int[list.length];
    	int i=0;
    	for (String s : list) {
    		ar[i++] = Integer.parseInt(s); 
    	}
    	return ar;
    }
	
    // helper function that returns a float array when given a comma delimited string of floats
	public static float[] getFloatArray(String str) {
    	String[] list = str.split(",");
    	float[] ar = new float[list.length];
    	int i=0;
    	for (String s : list) {
    		ar[i++] = Float.parseFloat(s); 
    	}
    	return ar;
    } 
	
	// returs an ArrayList<PointF> object with points parsed from a given string.
	// example string format: "x1,y1 ; x2,y2 ; x3,y3" (white space is allowed because it's ignored by the parser)
	public static ArrayList<PointF> getPointsList(String sPointsObject) {
		
		ArrayList<PointF> pointsList = new ArrayList<PointF>();
		
		sPointsObject = sPointsObject.replace(" ", ""); // replace all spaces with nothing
    	if (sPointsObject.length() == 0) return pointsList; // if string is empty return empty array    	
    	String[] sPoints = sPointsObject.split(";"); // split string into points    	
    	
    	for (String sPoint : sPoints) {
        	String[] coords = sPoint.split(","); // split point into coords    		
        	if (coords.length != 2) continue; // if point is not split into 2 coords then it is in wrong format so skip it        	
        	pointsList.add(new PointF(Float.parseFloat(coords[0]), Float.parseFloat(coords[1])));   		    		
    	}
    	
    	return pointsList;
	}
	
	// given size dimensions of target rectangle and a ratio, returns a rectangle that fits inside the target dimensions while maintaining given ratio.
	// the returned rectangle will be aligned to the centre of the target rectangle.
	public static Rect getFittingRectangle(float targetWidth, float targetHeight, float ratio) {
		
		//float targetRatio = targetWidth / targetHeight;
		
		float scaledWidth = targetWidth;
		float scaledHeight = scaledWidth / ratio;
		
		if (scaledHeight > targetHeight) {
			scaledHeight = targetHeight;
			scaledWidth = scaledHeight * ratio;
		}
		int left = (int)((targetWidth - scaledWidth) / 2);
		int top = (int)((targetHeight - scaledHeight) / 2);
		
		return new Rect(left, top, left + (int)scaledWidth, top + (int)scaledHeight);				
	}
	
}
