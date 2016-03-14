package com.sbg.lwc;

import java.util.ArrayList;

/*
 * RandomValueList Class: Helps manage string-defined lists of integer values. 
 * Values can be entered using range-value notation. The purpose of this class is 
 * to be able to retrieve particular values from such a list at random.
 * For example: "0:5, 7, 9, 10:20".
 * This list contains 19 values in total. Thus the probability of returning 
 * a particular value is 1/19. Values and ranges can overlap, thus 
 * increasing the probability of selection. A range is specified using a semicolon.
 */

// TODO: Rewrite Interval Class to allow reverse intervals (min > max)
public class RandomValueList {

	private String listOfValues;
	private ArrayList<Interval> intervals = new ArrayList<Interval>();
	private int valueCount = 0;
	private boolean empty = true;
	
	public RandomValueList() { }	
	public RandomValueList(String listOfValues) {
		set(listOfValues);
	}	
	
	public void set(String listOfValues) {
		this.listOfValues = listOfValues;	
		if (listOfValues.trim().equals("")) {
			empty = true;
			return;
		}
		parseValues();	
		empty = false;
	}
	
	public int getCount() {
		return valueCount;
	}
	
	public boolean isEmpty() {
		return empty;	
	}
	
	public int getRandomValue() {	
		return getRandomValue(0);
	}	
	
	public int getRandomValue(int defaultValue) {	
		
		if (empty) {
			return defaultValue;
		}
		
		int ordinal = SBLiveWallpaper.randomInt(1, valueCount);		
		int counter = 0;
		int min;
		int max;
		
		for(Interval i : intervals)	{		
			if (ordinal <= i.getIntervalSize()) return i.getMin() + ordinal - 1;
			else ordinal -= i.getIntervalSize();			
		}		
		return 0;		
	}	
	
	public Interval getRandomInterval() {
		return intervals.get(SBLiveWallpaper.randomInt(0, intervals.size() - 1));
	}
	
	public ArrayList<Interval>  getIntervalList() { return intervals; }
	
	private void parseValues() {
		
		listOfValues = listOfValues.replace(" ", ""); // remove spaces	
		String[] parts = listOfValues.split(","); // split into parts
		
		int min;
		int max;
		
		for (String p : parts) {			
			if (p.contains("-")) { // indicates a range of values
				String[] subParts = p.split("-");				
				min = Integer.parseInt(subParts[0]);
				max = Integer.parseInt(subParts[1]);
			}
			else { // single value
				min = Integer.parseInt(p);
				max = min;
			}		
			
			intervals.add(new Interval(min,max));				
			valueCount += Math.abs(max - min) + 1; // increment count
		}
	}
	
	public class Interval {
		
		private int min;
		private int max;
		private int intervalSize;
		
		public Interval(int min, int max) {
			//int temp;
			this.min = min;
			this.max = max;
			//if (min > max) { temp = min; min = max; max = min; }	
			this.intervalSize = Math.abs(max - min) + 1;
		}	
		
		public void setMin(int min) { this.min = min; }
		public int getMin() { return this.min; }
		public int getMax() { return this.max; }
		public void setMax(int max) { this.max = max; }
		public int getIntervalSize() { return this.intervalSize; }
	}
}
