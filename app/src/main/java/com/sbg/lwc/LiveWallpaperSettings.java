package com.sbg.lwc;

import crownapps.dandelionlivewallpaper.R;

import com.ironsource.mobilcore.AdUnitEventListener;
import com.ironsource.mobilcore.MobileCore;
import com.sbg.lwc.SBLiveWallpaper.SpawnMode;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.widget.Toast;

public class LiveWallpaperSettings extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Preference mCounter_settings = null;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        initMobileCore();

        getPreferenceManager().setSharedPreferencesName(SBLiveWallpaper.SHARED_PREFS_NAME);
        Resources res = getResources();

        addPreferencesFromResource(R.xml.lwp_settings);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        boolean isDemo = res.getBoolean(R.bool.isDemo);
        boolean mDisableInteraction = res.getBoolean(R.bool.disableInteraction);
        int mItemCount = res.getInteger(R.integer.itemCount);

        SpawnMode spawnMode = SpawnMode.valueOf(res.getString(R.string.spawnMode));

        PreferenceCategory main_preference_category = (PreferenceCategory) findPreference("main_preference_category");

        // disable nuber of items preference if spawnMode = touch
        if (spawnMode == SpawnMode.Touch) {
            main_preference_category.removePreference(getPreferenceScreen().findPreference("numOfItemsMultiplier"));
            //main_preference_category.removePreference(getPreferenceScreen().findPreference("hide_items"));
        }

        // is interaction is disabled during build then we need to remove that options from preferences 
        if (mDisableInteraction || isDemo || mItemCount == 0) {
            Preference disable_interaction = getPreferenceScreen().findPreference("disable_interaction");
            main_preference_category.removePreference(disable_interaction);
        }

        // remove hide items checkbox if there are no items by default
        if (mItemCount == 0) {
            main_preference_category.removePreference(getPreferenceScreen().findPreference("numOfItemsMultiplier"));
            main_preference_category.removePreference(getPreferenceScreen().findPreference("hide_items"));
        }

        if (isDemo) {
            // remove preference items
            main_preference_category.removePreference(getPreferenceScreen().findPreference("speed_settings"));
            main_preference_category.removePreference(getPreferenceScreen().findPreference("direction_settings"));
            main_preference_category.removePreference(getPreferenceScreen().findPreference("counter_settings"));
        } else {
            main_preference_category.removePreference(getPreferenceScreen().findPreference("demo_tag"));
        }

        if (mDisableInteraction) {
            if (getPreferenceScreen().findPreference("counter_settings") != null)
                main_preference_category.removePreference(getPreferenceScreen().findPreference("counter_settings"));
        }

        // Add custom user promotion preferences      
        PreferenceCategory pcPromo = (PreferenceCategory) findPreference("pcPromo");

        // 1. Create Google Play Link (Rate Me)
        Preference rateMePreference = new Preference(this);
        rateMePreference.setKey("rateMe");
        rateMePreference.setTitle("Rate Me (Google Play)");

        String gplayUrl = "https://play.google.com/store/apps/details?id=" + getPackageName();
        rateMePreference.setOnPreferenceClickListener(new UrlOnPreferenceClickListener(gplayUrl));
        pcPromo.addPreference(rateMePreference);

        // 2. Create "Share Me" preference
        Preference shareMePreference = new Preference(this);
        shareMePreference.setKey("shareMe");
        shareMePreference.setTitle("Share Me (send to a friend)");
        shareMePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                share();
                return true;
            }
        });
        pcPromo.addPreference(shareMePreference);

        // 3. Create individual preference buttons (up to 3)  
        // load labels and urls from strings.xml
        String[] labels = getResources().getStringArray(R.array.promoLabels);
        String[] urls = getResources().getStringArray(R.array.promoUrls);

        if (labels.length > 0) {
            PreferenceCategory pcCustomPromo = new PreferenceCategory(this);
            pcCustomPromo.setTitle("More From the Developer");
            getPreferenceScreen().addPreference(pcCustomPromo);


            for (int i = 0; i < labels.length; i++) {
                Preference prefLink = new Preference(this);
                prefLink.setKey("promoLink" + i);
                prefLink.setTitle(labels[i]);

                prefLink.setOnPreferenceClickListener(new UrlOnPreferenceClickListener(urls[i]));
                pcCustomPromo.addPreference(prefLink);
            }
        }
    }

    private void initMobileCore() {
        MobileCore.setAdUnitEventListener(new AdUnitEventListener() {
            @Override
            public void onAdUnitEvent(MobileCore.AD_UNITS adUnit, EVENT_TYPE eventType, MobileCore.AD_UNIT_TRIGGER... trigger) {
                if (adUnit == MobileCore.AD_UNITS.INTERSTITIAL && eventType == EVENT_TYPE.AD_UNIT_INIT_SUCCEEDED) {
                    MobileCore.loadAdUnit(MobileCore.AD_UNITS.INTERSTITIAL, MobileCore.AD_UNIT_TRIGGER.MAIN_MENU);
                } else if (adUnit == MobileCore.AD_UNITS.INTERSTITIAL && eventType == EVENT_TYPE.AD_UNIT_READY) {
                    for (MobileCore.AD_UNIT_TRIGGER myTrigger : trigger) {
                        if (myTrigger.equals(MobileCore.AD_UNIT_TRIGGER.MAIN_MENU)) {
                            MobileCore.showInterstitial(LiveWallpaperSettings.this, MobileCore.AD_UNIT_TRIGGER.MAIN_MENU, null);
                        }
                    }
                }
            }
        });

        //Uncomment and add your ids and stuff
//        MobileCore.init(activity, *DEVELOPER_HASH*, *LOG_TYPE*, *AD_UNITS*);
    }

    void openUrl(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        try {
            startActivity(i);
        } catch (Exception ex) {
            Toast.makeText(this, url + "\n\nThe URL format is not valid. Make sure it is prefixed with: http://", Toast.LENGTH_LONG).show();
        }
    }

    public void share() {
        //create the send intent
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        //set the type
        shareIntent.setType("text/plain");

        //add a subject
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.appName));

        String packageName = getPackageName();
        //build the body of the message to be shared
        String shareMessage = "Hey, check out this live wallpaper!\n\nhttps://play.google.com/store/apps/details?id=" + packageName;

        //add the message
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareMessage);

        //start the chooser for sharing
        startActivity(Intent.createChooser(shareIntent, "How do you want to share?"));
    }

    // easy way to retrieve application name without explicitly providing a resource id or package name
    public static String getApplicationName(Context context) {
        int stringId = context.getApplicationInfo().labelRes;
        return context.getString(stringId);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    class UrlOnPreferenceClickListener implements Preference.OnPreferenceClickListener {

        String url = "";

        public UrlOnPreferenceClickListener(String url) {
            this.url = url;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            openUrl(url);
            return false;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        // TODO Auto-generated method stub

    }
}
