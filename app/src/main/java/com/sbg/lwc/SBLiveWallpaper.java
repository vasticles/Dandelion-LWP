package com.sbg.lwc;

import java.io.IOException;
import java.util.ArrayList;

import crownapps.dandelionlivewallpaper.R;
import com.sbg.lwc.Item.ItemType;
import com.sbg.lwc.Item.ScaleType;
import com.sbg.lwc.Item.State;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class SBLiveWallpaper extends WallpaperService {

	public static final String SHARED_PREFS_NAME = "LiveWallpaperPrefs";
	public static int FPS = 50;
	public static int UPDATE_PERIOD = 1000 / FPS;

	@Override
	public void onCreate() {
		super.onCreate();
		// android.os.Debug.waitForDebugger();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public Engine onCreateEngine() {
		return new LwpEngine();
	}

	enum ActionStyle {
		Image, Text, Both, None
	}

	// this describes the various positions in which items can spawn inside
	// groups
	enum SpawnMode {
		Border, // spawn items along the borders of the group bounding box
		Random, // spawn item at random locations inside the group bounding box
		Touch, // spawn item at touch location
		Center
	}

	class LwpEngine extends Engine implements
			SharedPreferences.OnSharedPreferenceChangeListener {

		private final Handler mHandler = new Handler();

		private float mTouchX = -1;
		private float mTouchY = -1;

		private final Runnable mDrawLWP = new Runnable() {
			public void run() {
				drawFrame();
			}
		};
		private boolean mVisible;
		private SharedPreferences mPrefs;
		private boolean initComplete;
		float numOfItemsMultiplier;

		RectF screenBounds = new RectF();
		int mCanvasWidth = 0;
		int mCanvasHeight = 0;

		boolean isDemo;

		int itemCount = 0;
		int bitmapCount = 0;
		int actionBitmapCount = 0;

		Bitmap background = null;
		PointF mBackgroundOffset = new PointF(0, 0);

		ArrayList<Object> itemResources = new ArrayList<Object>();
		ArrayList<Object> actionItemResources = new ArrayList<Object>();

		ArrayList<Item> items = new ArrayList<Item>();

		float xOffset; // how many pixels the screen has shifted (so we could
						// draw the wallpaper parallax effect correctly)

		// POINTS
		boolean mShowStreakCounter = false;
		int mStreakCounter = 0;
		int streakPeriod = 0;
		boolean isStreak = false;

		// PAINTS
		Paint actionTextPaint;
		Paint counterPaint;
		Paint streakPaint;

		// Action Items
		ArrayList<Item> actionItems = new ArrayList<Item>();
		ActionStyle actionStyle = ActionStyle.None;
		String[] actionText;

		boolean disableInteraction;
		boolean prefCheckDisableInteraction;
		boolean prefCheckDisableItems;

		RandomValueList directionValues;
		RandomValueList scaleValues;
		RandomValueList speedValues;
		RandomValueList angleValues;
		RandomValueList opacityValues;
		RandomValueList rotationPeriodValues;

		// Dying State Animation Variables
		RandomValueList durationOfDyingState;
		RandomValueList speedValuesOfDying;
		RandomValueList directionValuesOfDying;
		boolean fadeOutDying;
		RandomValueList rotationPeriodValuesDying;
		ScaleType scaleTypeDying = ScaleType.Grow;

		// Action Item Animation Variables
		boolean useGifDurationForActionState;
		RandomValueList durationValuesOfAction;
		RandomValueList speedValuesOfAction;
		RandomValueList directionValuesOfAction;
		boolean fadeOutAction;
		RandomValueList rotationPeriodValuesOfAction;
		ScaleType scaleTypeAction = ScaleType.None;

		// No-Scroll Mode
		boolean noScroll;

		// Spawn mode
		SpawnMode spawnMode = SpawnMode.Border;

		boolean associateActionItems;
		boolean spawnItem;

		// splash screen
		int durationOfSplashLogo = 1000;
		int currentSplashTimer = 0;
		int durationOfSplashLogoFade = 200;
		Item splashLogo;
		Item splashLogoText;
		boolean drawLwcLogo = false;
		boolean drawPersonalLogo = true;
		String lwcLogoText = "Live Wallpaper Creator";
		Item personalLogo;

		// LWC 2.6
		String selectedBackground;

		// LWC 2.6

		LwpEngine() {
		}

		void init() {
			mPrefs = SBLiveWallpaper.this.getSharedPreferences(
					SHARED_PREFS_NAME, 0);
			mPrefs.registerOnSharedPreferenceChangeListener(this);

			Util.init(getApplicationContext(), mCanvasWidth, mCanvasHeight);

			Resources res = getResources();

			// create splash logo
			splashLogo = new Item();
			Bitmap splashLogoBitmap = BitmapFactory.decodeResource(res,
					R.drawable.lwc_logo);
			splashLogo.setDrawable(splashLogoBitmap);
			RectF logoBounds = splashLogo.getScaledBounds();
			splashLogo.setPosition(
					(mCanvasWidth - splashLogoBitmap.getWidth()) / 2,
					(mCanvasHeight - splashLogoBitmap.getHeight() * 1.5f) / 2);
			splashLogo.setDurationOfDyingState(durationOfSplashLogoFade);
			splashLogo.fadeOutDying = true;
			splashLogo.scaleTypeDying = ScaleType.None;
			splashLogo.setAlive();

			splashLogoText = new Item();
			Paint logoPaint = new Paint();
			logoPaint.setColor(Color.WHITE);
			logoPaint.setTypeface(Typeface.createFromAsset(getAssets(),
					"fonts/Merienda One.ttf"));
			logoPaint.setTextSize(30);
			splashLogoText.setPaint(logoPaint);
			splashLogoText.setDrawable(lwcLogoText);
			RectF logoTextBounds = splashLogoText.getScaledBounds();
			splashLogoText.setPosition(
					(mCanvasWidth - logoTextBounds.width()) / 2,
					logoBounds.bottom + logoTextBounds.height());
			splashLogoText.setDurationOfDyingState(durationOfSplashLogoFade);
			splashLogoText.fadeOutDying = true;
			splashLogoText.scaleTypeDying = ScaleType.None;
			splashLogoText.setAlive();

			// create personal logo
			personalLogo = new Item();

			int logoResID = res.getIdentifier("personal_logo", "drawable",
					getApplicationContext().getPackageName());
			if (logoResID != 0) {
				Bitmap personalLogoBitmap = BitmapFactory.decodeResource(res,
						logoResID);
				personalLogo.setDrawable(personalLogoBitmap);
				// RectF personalLogoBounds = personalLogo.getScaledBounds();
				personalLogo.setPosition(
						(mCanvasWidth - personalLogoBitmap.getWidth()) / 2,
						(mCanvasHeight - personalLogoBitmap.getHeight()) / 2);
				personalLogo.setDurationOfDyingState(durationOfSplashLogoFade);
				personalLogo.fadeOutDying = true;
				personalLogo.scaleTypeDying = ScaleType.None;
				personalLogo.setAlive();
			}
			drawPersonalLogo = logoResID != 0;

			// LWC 2.6

			// Load background bitmap
			// if (background == null) background = getWallpaperResource();

			// LWC 2.6

			isDemo = res.getBoolean(R.bool.isDemo);
			itemCount = res.getInteger(R.integer.itemCount);

			// Load item bitmaps
			String[] itemNames = res.getStringArray(R.array.items);
			itemResources.clear();
			for (String item : itemNames) {
				itemResources.add(BitmapFactory.decodeResource(res, res
						.getIdentifier(item, "drawable", getApplication()
								.getPackageName())));
			}

			// Load item gifs
			String[] gifNames = res.getStringArray(R.array.gifs);
			for (String item : gifNames) {
				itemResources.add(Movie.decodeStream(getResources()
						.openRawResource(
								getResources().getIdentifier(item, "drawable",
										getPackageName()))));
			}

			// Load action item bitmaps
			String[] actionItems = res.getStringArray(R.array.actionItems);
			actionItemResources.clear();
			for (String item : actionItems) {
				actionItemResources.add(BitmapFactory.decodeResource(res, res
						.getIdentifier(item, "drawable", getApplication()
								.getPackageName())));
			}

			// Load action item gifs
			String[] actionGifNames = res.getStringArray(R.array.actionGifs);
			for (String item : actionGifNames) {
				actionItemResources.add(Movie.decodeStream(getResources()
						.openRawResource(
								getResources().getIdentifier(item, "drawable",
										getPackageName()))));
			}

			actionText = res.getStringArray(R.array.actionText);
			actionStyle = ActionStyle.valueOf((res
					.getString(R.string.actionStyle)));

			streakPeriod = res.getInteger(R.integer.streakPeriod);
			disableInteraction = res.getBoolean(R.bool.disableInteraction);

			// Load speed values
			speedValues = new RandomValueList(
					res.getStringArray(R.array.speedValues)[0]); // default is
																	// speed
																	// preset 1

			scaleValues = new RandomValueList(
					res.getString(R.string.scaleValues));
			angleValues = new RandomValueList(
					res.getString(R.string.angleValues));
			opacityValues = new RandomValueList(
					res.getString(R.string.opacityValues));
			rotationPeriodValues = new RandomValueList(
					res.getString(R.string.rotationPeriodValues));

			// Load Direction ('directionsList' array contains all 4 directions
			// where as 'direction' array contains only the directions which are
			// selected in settings)
			directionValues = new RandomValueList(
					res.getStringArray(R.array.directionValues)[0]); // default
																		// is
																		// direction
																		// preset
																		// 1

			// Load Dying State Animation Variables
			durationOfDyingState = new RandomValueList(
					res.getString(R.string.durationValuesOfDying));
			speedValuesOfDying = new RandomValueList(
					res.getString(R.string.speedValuesOfDying));
			directionValuesOfDying = new RandomValueList(
					res.getString(R.string.directionValuesOfDying));
			fadeOutDying = res.getBoolean(R.bool.fadeOutDying);
			scaleTypeDying = ScaleType.valueOf(res
					.getString(R.string.scaleTypeDying));
			rotationPeriodValuesDying = new RandomValueList(
					res.getString(R.string.rotationValuesOfDying));

			// Load Action Item Animation Variables
			useGifDurationForActionState = res
					.getBoolean(R.bool.useGifDurationForActionState);
			durationValuesOfAction = new RandomValueList(
					res.getString(R.string.durationValuesOfAction));
			speedValuesOfAction = new RandomValueList(
					res.getString(R.string.speedValuesOfAction));
			directionValuesOfAction = new RandomValueList(
					res.getString(R.string.directionValuesOfAction));
			fadeOutAction = res.getBoolean(R.bool.fadeOutAction);
			scaleTypeAction = ScaleType.valueOf(res
					.getString(R.string.scaleTypeAction));
			rotationPeriodValuesOfAction = new RandomValueList(
					res.getString(R.string.rotationValuesOfAction));

			// spawn mode
			spawnMode = SpawnMode.valueOf(res.getString(R.string.spawnMode));

			// associate action items
			associateActionItems = res.getBoolean(R.bool.associateActionItems);

			initPaintObjects();

			onSharedPreferenceChanged(mPrefs, null);

			// createShortcut();

			initComplete = true;
		}

		public void respawn(Item item, boolean spawnInsideScreen) {

			// Step 1: Randomise item properties: Image, Direction, Scale,
			// Angle, Opacity, Speed, rotationPeriod

			// Select random item image
			int randomIndex = randomInt(0, itemResources.size() - 1);
			item.resourceIndex = randomIndex;
			item.setDrawable(itemResources.get(randomIndex));

			// Direction
			int dirValue = directionValues.getRandomValue();
			item.setDirection(dirValue);

			// Speed
			float value = speedValues.getRandomValue();
			float speed = value / 100f * mCanvasWidth / FPS;
			item.setSpeed(speed);

			// Scale
			item.setScale(scaleValues.getRandomValue() / 100f);

			// Angle
			item.setAngle(angleValues.getRandomValue());

			// Opacity
			item.setOpacity(opacityValues.getRandomValue() / 100f);

			// Rotation Period
			item.setRotationPeriod(rotationPeriodValues.getRandomValue());

			// Step 2: Set item position based on spawn mode value
			RectF bounds = item.getScaledBounds();
			if (spawnInsideScreen) {
				int randomX = SBLiveWallpaper.randomInt(0, mCanvasWidth
						- bounds.width());
				int randomY = SBLiveWallpaper.randomInt(0, mCanvasHeight
						- bounds.height());
				item.setPosition(randomX, randomY);
			} else {
				randomizeItemPosition(item);
			}
			item.hasEnteredScreen = spawnInsideScreen;

			// Step 3: Set Dying State Animation Values
			item.durationOfDyingState = durationOfDyingState.getRandomValue();
			item.scaleTypeDying = scaleTypeDying;
			item.fadeOutDying = fadeOutDying;
			item.setRotationPeriodDying(rotationPeriodValuesDying
					.getRandomValue());

			// Step 4: set alive state to prompt item to start updating and
			// drawing
			item.setAlive();
		}

		void applyDyingStateProperties(Item item) {
			// Direction
			if (!directionValuesOfDying.isEmpty()) {
				int dirValue = directionValuesOfDying.getRandomValue();
				item.setDirection(dirValue);
			}

			// Speed
			if (!speedValuesOfDying.isEmpty()) {
				float value = speedValuesOfDying.getRandomValue();
				float speed = value / 100f * mCanvasWidth / FPS;
				item.setSpeed(speed);
			}

			// Rotation Period
			if (!rotationPeriodValuesDying.isEmpty()) {
				item.setRotationPeriod(rotationPeriodValuesDying
						.getRandomValue());
			}
		}

		void update() {

			if (!isPreview()) {
				if (drawPersonalLogo
						&& currentSplashTimer > (durationOfSplashLogo + durationOfSplashLogoFade * 4)) {
					drawPersonalLogo = false;
					drawLwcLogo = true;
					currentSplashTimer = 0;
				} else if (drawPersonalLogo
						&& currentSplashTimer >= durationOfSplashLogo
						&& personalLogo.getState() == State.Alive) {
					personalLogo.setDying(0);
				}
				if (drawPersonalLogo) {
					personalLogo.update(UPDATE_PERIOD);
					currentSplashTimer += UPDATE_PERIOD;
				}

				if (drawLwcLogo
						&& currentSplashTimer > (durationOfSplashLogo + durationOfSplashLogoFade * 4)) {
					drawLwcLogo = false;
				} else if (drawLwcLogo
						&& currentSplashTimer >= durationOfSplashLogo
						&& splashLogo.getState() == State.Alive) {
					splashLogo.setDying(0);
					splashLogoText.setDying(0);
				}
				if (drawLwcLogo) {
					splashLogo.update(UPDATE_PERIOD);
					splashLogoText.update(UPDATE_PERIOD);
					currentSplashTimer += UPDATE_PERIOD;
				}

				if (drawLwcLogo || drawPersonalLogo)
					return;
			}

			RectF bounds;
			State state;

			// remove inactive items when spawn mode = touch
			if (spawnMode == SpawnMode.Touch) {
				Item item;
				for (int i = items.size() - 1; i >= 0; i--) {
					item = items.get(i);
					if (item.getState() == State.Inactive)
						items.remove(i);
				}
			}

			if (spawnItem) {
				Item item = new Item();
				item.setPaint(actionTextPaint);
				items.add(item);
				respawn(item, false);
				spawnItem = false;
			}

			// update items
			for (Item item : items) {

				state = item.getState();

				if (state == State.Dead) {
					// do not spawn an action item if a streak item was already
					// spawned when the streak was hit to avoid confusion
					// created by two items spawning from one parent item almost
					// at the same time
					if (!item.streakItem) {
						switch (actionStyle) {
						case Image: { // spawn an image action item
							spawnImageActionItem(item);
							break;
						}
						case Text: { // spawn a text action item
							spawnTextActionItem(item);
							break;
						}
						case Both: { // spawn either a text or an image action
										// item, choose randomly

							if (Math.random() > 0.5)
								spawnTextActionItem(item);
							else
								spawnImageActionItem(item);
							break;
						}
						default: {
						} // :P
						}
					}

					if (spawnMode != SpawnMode.Touch)
						respawn(item, false);
					else {
						item.setInactive();
					}

				} else if (state == State.Alive) {
					// check if item is out of bounds of the screen area, if so,
					// respawn it
					bounds = item.getScaledBounds();
					if (item.hasEnteredScreen) {
						if (!RectF.intersects(bounds, screenBounds)) {
							if (spawnMode != SpawnMode.Touch)
								respawn(item, false);
						}
					} else {
						if (RectF.intersects(bounds, screenBounds)) {
							item.hasEnteredScreen = true;
						}
					}
				}

				// update the item
				item.update(UPDATE_PERIOD);
			}

			// update action items
			for (Item item : actionItems) {
				item.update(UPDATE_PERIOD);
			}

			// remove dead action items from array
			Item actionItem;
			for (int i = actionItems.size() - 1; i >= 0; i--) {
				actionItem = actionItems.get(i);
				if (actionItem.getState() == State.Dead)
					actionItems.remove(i);
			}
		}

		public void onSharedPreferenceChanged(SharedPreferences prefs,
				String key) {
			boolean showCounter = false;
			Resources res = getResources();
			if (isDemo) {
				speedValues = new RandomValueList(
						res.getStringArray(R.array.speedValues)[0]); // set
																		// speed
																		// to
																		// Preset
																		// 1
																		// always
																		// if
																		// this
																		// is a
																		// demo
																		// version
				showCounter = false;

				// direction
				directionValues = new RandomValueList(
						res.getStringArray(R.array.directionValues)[0]); // preset
																			// 1
																			// if
																			// demo
			} else {
				// speed
				speedValues = new RandomValueList(prefs.getString(
						"speed_settings",
						res.getStringArray(R.array.speedValues)[0]));

				// counter
				showCounter = prefs.getBoolean("counter_settings", false);

				// direction
				directionValues = new RandomValueList(prefs.getString(
						"direction_settings",
						res.getStringArray(R.array.directionValues)[0]));

			}
			// hide items
			prefCheckDisableItems = prefs.getBoolean("hide_items", false);

			// interaction
			prefCheckDisableInteraction = prefs.getBoolean(
					"disable_interaction", false)
					|| res.getBoolean(R.bool.disableInteraction);

			// number of items multiplier
			numOfItemsMultiplier = Integer.parseInt(prefs.getString(
					"numOfItemsMultiplier", "100")) / 100f;

			// streak counter
			mShowStreakCounter = showCounter && itemCount > 0
					&& !prefCheckDisableItems;

			// no-scroll
			noScroll = prefs.getBoolean("pref_noScroll", false);

			// LWC 2.6
			// change background
			selectedBackground = prefs.getString("background_settings",
					"Background 1");
			// LWC 2.6

			// init items
			int totalItems = (int) (itemCount * numOfItemsMultiplier);
			items.clear();
			if (spawnMode != SpawnMode.Touch) {
				for (int i = 0; i < totalItems; i++) {
					Item item = new Item();
					item.setPaint(actionTextPaint);
					items.add(item);
					respawn(item, true);
				}
			}

			// fps
			FPS = Integer.parseInt(prefs.getString("pref_fps", "50"));
			UPDATE_PERIOD = 1000 / FPS;

			background = getWallpaperResource();
		}

		public void randomizeItemPosition(Item item) {

			// init some temp variables
			RectF itemBounds = item.getScaledBounds();
			float x = screenBounds.left;
			float y = screenBounds.top;

			if (spawnMode == SpawnMode.Touch && spawnItem) {
				x = mTouchX - itemBounds.width() / 2;
				y = mTouchY - itemBounds.height() / 2;
			} else if (spawnMode == SpawnMode.Center) {
				x = screenBounds.left
						+ (screenBounds.width() - itemBounds.width()) / 2;
				y = screenBounds.top
						+ (screenBounds.height() - itemBounds.height()) / 2;
			} else if (spawnMode == SpawnMode.Random) {
				x = Util.randomInt(screenBounds.left, screenBounds.right
						- itemBounds.width());
				y = Util.randomInt(screenBounds.top, screenBounds.bottom
						- itemBounds.height());
			} else if (spawnMode == SpawnMode.Border) {
				// based on the signs of the velocity vector components we can
				// determine which borders we have to consider
				Vector velocity = item.getVelocity();
				float dx = velocity.getX();
				float dy = velocity.getY();

				// randomise item location
				x = Util.randomInt(screenBounds.left, screenBounds.right
						- itemBounds.width());
				y = Util.randomInt(screenBounds.top, screenBounds.bottom
						- itemBounds.height());

				float moveDistX = 0;
				float moveDistY = 0;

				// x
				if (dx > 0) {
					moveDistX = x - screenBounds.left + itemBounds.width();
				} else if (dx < 0) {
					moveDistX = screenBounds.right - x;
				}
				// y
				if (dy > 0) {
					moveDistY = y - screenBounds.top + itemBounds.height();
				} else if (dy < 0) {
					moveDistY = screenBounds.bottom - y;
				}

				// determine which direction requires the smaller shift
				if (dx != 0 && dy != 0) {
					if (Math.abs(moveDistX / dx) < Math.abs(moveDistY / dy)) {
						moveDistY = (dx == 0) ? moveDistY : Math.abs(moveDistX
								* dy / dx);
					} else { // y is smaller
						moveDistX = (dy == 0) ? moveDistX : Math.abs(moveDistY
								* dx / dy);
					}
				}

				x += moveDistX * (dx > 0 ? -1 : 1);
				y += moveDistY * (dy > 0 ? -1 : 1);
			}
			item.setPosition(x, y);
		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
			setTouchEventsEnabled(true);
			PreferenceManager.setDefaultValues(getBaseContext(),
					R.xml.lwp_settings, false);
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			mHandler.removeCallbacks(mDrawLWP);
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			mVisible = visible;
			if (visible) {
				drawFrame();
			} else {
				mHandler.removeCallbacks(mDrawLWP);
			}
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			mCanvasWidth = width;
			mCanvasHeight = height;
			screenBounds.set(0, 0, mCanvasWidth, mCanvasHeight);
			init();
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			super.onSurfaceCreated(holder);
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
			mVisible = false;
			mHandler.removeCallbacks(mDrawLWP);
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset, float xStep,
				float yStep, int xPixels, int yPixels) {
			this.xOffset = xOffset;
			drawFrame();
		}

		@Override
		public void onTouchEvent(MotionEvent event) {
			if (prefCheckDisableItems)
				return;
			// if (!disableInteraction && !mDisableInteractionChecked) {

			mTouchX = event.getX();
			mTouchY = event.getY();

			if (event.getAction() == MotionEvent.ACTION_DOWN) {

				boolean isHit = false;
				RectF bounds;

				if (!prefCheckDisableInteraction) {
					for (Item item : items) {
						bounds = item.getScaledBounds(); // get item position
															// and size

						if (item.getState() == State.Alive
								&& mTouchX >= bounds.left
								&& mTouchX <= bounds.right
								&& mTouchY >= bounds.top
								&& mTouchY <= bounds.bottom) {
							mStreakCounter++;
							// check if a streak was reached and spawn an action
							// item which displays the streak, instead of the
							// usual text or image action item
							if (streakPeriod != 0
									&& mStreakCounter % streakPeriod == 0) {
								item.streakItem = true;
								spawnTextActionItem(mStreakCounter + "", item);
							}
							int dyingDuration = item.getDurationOfDyingState();
							if (dyingDuration == 0) {
								item.setDead();
								isHit = true;
								break;
							} else if (dyingDuration < UPDATE_PERIOD) {
								item.setDurationOfDyingState(UPDATE_PERIOD);
							}

							item.setDying(mStreakCounter);
							applyDyingStateProperties(item);

							isHit = true;
							break;
						}
					}
				}
				if (!isHit) {
					mStreakCounter = 0;
					if (spawnMode == SpawnMode.Touch) { // raise flag to spawn
														// item in the update
														// method
						spawnItem = true;
					}
				}

			}
			// }
			super.onTouchEvent(event);
		}

		void drawFrame() {
			final SurfaceHolder holder = getSurfaceHolder();

			Canvas c = null;
			try {
				c = holder.lockCanvas();
				if (c != null) {
					if (initComplete) {
						update();
						draw(c);
					}
				}
			} finally {
				if (c != null)
					holder.unlockCanvasAndPost(c);
			}

			mHandler.removeCallbacks(mDrawLWP);
			if (mVisible) {
				mHandler.postDelayed(mDrawLWP, UPDATE_PERIOD);
			}
		}

		void spawnImageActionItem(Item parentItem) {
			Item actionItem = new Item();
			actionItem.setPaint(actionTextPaint);
			// randomise image of action item
			actionItem.setDrawable(getActionImage(parentItem));
			initActionItemProperties(actionItem, parentItem);
		}

		void spawnTextActionItem(String text, Item parentItem) {
			Item actionItem = new Item();
			actionItem.setPaint((parentItem.streakItem ? streakPaint
					: actionTextPaint));
			actionItem.setDrawable(text);
			initActionItemProperties(actionItem, parentItem);
		}

		void spawnTextActionItem(Item parentItem) {
			// randomise text of action item
			spawnTextActionItem(getActionText(parentItem), parentItem);
		}

		String getActionText(Item parentItem) {
			if (associateActionItems) {
				return actionText[parentItem.resourceIndex % actionText.length];
			} else {
				return actionText[randomInt(0, actionText.length - 1)];
			}
		}

		Object getActionImage(Item parentItem) {
			if (associateActionItems) {
				return actionItemResources.get(parentItem.resourceIndex
						% actionItemResources.size());
			} else {
				return actionItemResources.get(randomInt(0,
						actionItemResources.size() - 1));
			}
		}

		void initActionItemProperties(Item actionItem, Item parentItem) {

			if (parentItem.streakItem) {
				// Set Action Item Animation Values
				actionItem.durationOfDyingState = 1000;
				actionItem.scaleTypeDying = ScaleType.None;
				actionItem.fadeOutDying = fadeOutAction;
				actionItem.setRotationPeriodDying(0);

				// Direction
				actionItem.setDirection(90);

				// Speed
				float value = 20;
				float speed = value / 100f * mCanvasWidth / FPS;
				actionItem.setSpeed(speed);

				// Rotation Period = 0
			} else {
				// Set Action Item Animation Values
				if (useGifDurationForActionState
						&& actionItem.getItemType() == ItemType.Gif) {
					actionItem.durationOfDyingState = actionItem
							.getGifDuration();
				} else
					actionItem.durationOfDyingState = durationValuesOfAction
							.getRandomValue();

				if (actionItem.getDurationOfDyingState() == 0) {
					return;
				}

				actionItem.scaleTypeDying = scaleTypeAction;
				actionItem.fadeOutDying = fadeOutAction;

				/*
				 * if (!rotationPeriodValuesOfAction.isEmpty()) {
				 * actionItem.setRotationPeriodDying
				 * (rotationPeriodValuesOfAction.getRandomValue()); }
				 */

				// Direction
				if (!directionValuesOfAction.isEmpty()) {
					actionItem.setDirection(directionValuesOfAction
							.getRandomValue());
				} else {
					actionItem.setDirection(parentItem.getDirection());
				}

				// Speed
				if (!speedValuesOfAction.isEmpty()) {
					float value = speedValuesOfAction.getRandomValue();
					float speed = value / 100f * mCanvasWidth / FPS;
					actionItem.setSpeed(speed);
				} else {
					actionItem.setSpeed(parentItem.getSpeed());
				}

				// Rotation Period
				if (!rotationPeriodValuesOfAction.isEmpty()) {
					actionItem.setRotationPeriod(rotationPeriodValuesOfAction
							.getRandomValue());
				} else {
					actionItem
							.setRotationPeriod(parentItem.getRotationPeriod());
				}
			}

			// [randomisation of the following properties is not implemented for
			// action items, their default values are given]
			// Scale = 1
			// Angle = 0 degrees
			// Opacity = 100%

			// align action item to centre of parent item
			RectF bounds = actionItem.getScaledBounds();
			RectF parentBounds = parentItem.getScaledBounds();
			float x = parentBounds.left
					+ (parentBounds.width() - bounds.width()) / 2;
			float y = parentBounds.top
					+ (parentBounds.height() - bounds.height()) / 2;
			actionItem.setPosition(x, y);
			actionItem.updateMatrix();

			// set state to dying
			actionItem.setDying(0);

			// add action item to action items array
			actionItems.add(actionItem);
		}

		void draw(Canvas c) {
			c.drawColor(Color.BLACK);

			if (!isPreview() && drawLwcLogo) {
				splashLogo.draw(c);
				splashLogoText.draw(c);
			} else if (!isPreview() && drawPersonalLogo) {
				personalLogo.draw(c);
			} else {
				// draw background image
				if (noScroll) {
					c.drawBitmap(background,
							-(background.getWidth() - mCanvasWidth) / 2,
							mBackgroundOffset.y, null);
				} else {
					c.drawBitmap(background, -xOffset * mCanvasWidth
							- mBackgroundOffset.x, -mBackgroundOffset.y, null);
				}

				if (!prefCheckDisableItems) {
					// draw streak counter
					if (mShowStreakCounter) {
						float t = counterPaint.measureText(mStreakCounter + "");
						c.drawText(mStreakCounter + "", mCanvasWidth / 2 - t
								/ 2, mCanvasHeight / 3, counterPaint);
					}

					// draw items
					for (Item item : items) {
						item.updateGifTiming();
						item.draw(c);
					}

					// draw action items
					for (Item actionItem : actionItems) {
						actionItem.updateGifTiming();
						actionItem.draw(c);
					}
				}
			}
			/*
			 * // draw centre lines c.drawLine(0, mCanvasHeight / 2,
			 * mCanvasWidth, mCanvasHeight / 2, streakPaint); // horizontal
			 * c.drawLine(mCanvasWidth / 2, 0, mCanvasWidth / 2, mCanvasHeight,
			 * streakPaint); // vertical
			 */

			// draw debug output
			Util.drawDebugOutput(c);

		}

		public void initPaintObjects() {
			int offset = 0;
			int fontSize = 0;
			Resources res = getResources();

			actionTextPaint = new Paint();
			actionTextPaint.setAntiAlias(true);
			actionTextPaint.setColor(res.getColor(R.color.actionTextFontColor));

			try {
				actionTextPaint.setTypeface(Typeface.createFromAsset(
						getAssets(),
						"fonts/" + res.getString(R.string.actionTextFont)
								+ ".ttf"));
			} catch (Exception ex) {

			}

			fontSize = res.getInteger(R.integer.actionTextFontSize);
			if (fontSize > 20)
				offset = 20;
			actionTextPaint.setTextSize(fontSize - offset);
			offset = 0;

			counterPaint = new Paint();
			counterPaint.setAntiAlias(true);
			counterPaint.setColor(res.getColor(R.color.counterFontColor));

			try {
				counterPaint.setTypeface(Typeface
						.createFromAsset(getAssets(),
								"fonts/" + res.getString(R.string.counterFont)
										+ ".ttf"));
			} catch (Exception ex) {

			}

			counterPaint.setTextSize((float) res
					.getInteger(R.integer.counterFontSize));

			streakPaint = new Paint();
			streakPaint.setAntiAlias(true);
			streakPaint.setColor(res.getColor(R.color.streakFontColor));

			try {
				streakPaint
						.setTypeface(Typeface.createFromAsset(getAssets(),
								"fonts/" + res.getString(R.string.streakFont)
										+ ".ttf"));
			} catch (Exception ex) {

			}

			fontSize = res.getInteger(R.integer.streakFontSize);
			if (fontSize > 20)
				offset = 20;
			streakPaint.setTextSize(fontSize - offset);
		}

		public Bitmap getWallpaperResource() {
			Bitmap scaledBitmap = null;
			DisplayMetrics dm = getResources().getDisplayMetrics();
			PointF tSize = new PointF(dm.widthPixels * 2, dm.heightPixels);
			PointF hSize = new PointF(1080, 960);
			PointF mSize = new PointF(640, 480);

			// determine which ratio best suits the target (which image do we
			// need to resize least to fill our screen)
			float sfHigh = tSize.x / hSize.x; // assume scale by width
			if (sfHigh < tSize.y / hSize.y) { // scale by height
				sfHigh = tSize.y / hSize.y;
			}

			float sfMid = tSize.x / mSize.x; // assume scale by width
			if (sfMid < tSize.y / mSize.y) { // scale by height
				sfMid = tSize.y / mSize.y;
			}

			// assume to use high resolution image
			float scaleFactor = sfHigh;
			int res = R.drawable.wallpaper_hdpi;

			if (sfHigh > sfMid) { // use mid resolution image
				scaleFactor = sfMid;
				res = R.drawable.wallpaper_mdpi;
			}

			// LWC 2.6
			if (selectedBackground.equals("Blue")) {

				res = R.drawable.wallpaper_hdpi;

				if (sfHigh > sfMid) { // use mid resolution image
					scaleFactor = sfMid;
					res = R.drawable.wallpaper_mdpi;
				}

			}

			else if (selectedBackground.equals("Grey")) {

				res = R.drawable.wallpaper_hdpi1;

				if (sfHigh > sfMid) { // use mid resolution image
					scaleFactor = sfMid;
					res = R.drawable.wallpaper_mdpi1;
				}

			}

			else if (selectedBackground.equals("Orange")) {

				res = R.drawable.wallpaper_hdpi2;

				if (sfHigh > sfMid) { // use mid resolution image
					scaleFactor = sfMid;
					res = R.drawable.wallpaper_mdpi2;
				}

			}

			else if (selectedBackground.equals("Pink")) {

				res = R.drawable.wallpaper_hdpi3;

				if (sfHigh > sfMid) { // use mid resolution image
					scaleFactor = sfMid;
					res = R.drawable.wallpaper_mdpi3;
				}

			}

			else if (selectedBackground.equals("Light Blue")) {

				res = R.drawable.wallpaper_hdpi4;

				if (sfHigh > sfMid) { // use mid resolution image
					scaleFactor = sfMid;
					res = R.drawable.wallpaper_mdpi4;
				}

			}

			Rect cropRect;
			BitmapRegionDecoder decoder;
			Bitmap croppedBitmap;
			try {
				decoder = BitmapRegionDecoder.newInstance(getResources()
						.openRawResource(res), false);

				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPreferredConfig = Config.ARGB_8888; // explicit
																// setting!

				BitmapFactory.Options optionsSize = new BitmapFactory.Options();
				optionsSize.inJustDecodeBounds = true;
				BitmapFactory.decodeResource(getResources(), res, optionsSize);

				float wallpaperRatio = (mCanvasWidth * 2)
						/ (float) mCanvasHeight;
				if (noScroll) {
					wallpaperRatio = mCanvasWidth / (float) mCanvasHeight;
				}
				cropRect = Util.getFittingRectangle(optionsSize.outWidth,
						optionsSize.outHeight, wallpaperRatio);

				croppedBitmap = decoder.decodeRegion(cropRect, options);

				scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap,
						noScroll ? mCanvasWidth : (mCanvasWidth * 2),
						mCanvasHeight, true);

				croppedBitmap.recycle();

			} catch (NotFoundException e) {

			} catch (IOException e) {

			}

			return scaledBitmap;
		}

		/*
		 * void createShortcut() {
		 * 
		 * Intent shortcutIntent; shortcutIntent = new Intent();
		 * shortcutIntent.setAction
		 * (WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
		 * shortcutIntent.setComponent(null);
		 * 
		 * shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		 * shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		 * 
		 * final Intent putShortCutIntent = new Intent();
		 * putShortCutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
		 * shortcutIntent);
		 * 
		 * // Sets the custom shortcut's title
		 * putShortCutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
		 * getResources().getString(R.string.appName));
		 * putShortCutIntent.putExtra
		 * (Intent.EXTRA_SHORTCUT_ICON_RESOURCE,Intent.
		 * ShortcutIconResource.fromContext(getApplicationContext(),
		 * R.drawable.icon)); putShortCutIntent.setAction(
		 * "com.android.launcher.action.INSTALL_SHORTCUT");
		 * putShortCutIntent.putExtra("duplicate", false);
		 * sendBroadcast(putShortCutIntent); }
		 */
	}

	// returns a random integer between min and max inclusive
	public static int randomInt(int min, int max) {
		return min + (int) (Math.random() * (max - min + 1));
	}

	public static int randomInt(float min, float max) {
		return (int) min + (int) (Math.random() * ((int) max - (int) min + 1));
	}
}