package com.sbg.lwc;

import crownapps.dandelionlivewallpaper.R;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
 
public class Splash extends Activity{
 
    //Make a button that will take the user to the live wallpaper picker
    private Button lwpInstallBtn;
    //Make a simple textview for the title of your wallpaper that we can display in the splash activity
    private TextView title;
 
    public void sendMessage(View view)
    {
        Intent intent = new Intent(Splash.this, LiveWallpaperSettings.class);
        startActivity(intent);
    }
    
    public void sendMessage2(View view)
    {
    	Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=crownapps.neonlivewallpaper"));
        startActivity(intent);
    }
    
    public void sendMessage3(View view)
    {
    	Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=crownapps.bubble"));
        startActivity(intent);
    }
    
    public void sendMessage4(View view)
    {
    	Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=crownapps.delux"));
        startActivity(intent);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //activities need to refer to an xml layout file, where you have laid out all of the participating components. xml layout files are stored in res/layout. ours is called splash.xml
        setContentView(R.layout.splash);
 
        //this is optional, but you can add your own fonts for your text components for more variety :) if you do, make sure you put it in your assets folder. in this example i have a fonts subfolder inside my assets.
        
 
        //link the textview with the xml element using the ID that is given to it in the layout file
        title = (TextView) findViewById(R.id.title);
        //now you can apply the custom font, if you want to
        
 
        //now we link the button to the xml in similar fashion
        lwpInstallBtn = (Button) findViewById(R.id.installWallpaper);
        //apply the custom font again
        
        lwpInstallBtn.setOnClickListener(new ImageButton.OnClickListener() {
 
            public void onClick(View v) {
                //this is where we customize the listener. basically we will outlay a set of instructions that will execute once the button is pressed.
     
                //first, we deal with accessing the wallpaper picker.
                //we need to use an intent to tell the system that we want to access the live wallpaper picker. create a new intent with this next line
                Intent intent = new Intent();
                //tell the intent what to do.
                intent.setAction(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                //send the intent through
                startActivity(intent);
 
     
                //second, let's guide the user to select the wallpaper once they are in the wallpaper picker.
                //we accomplish this by showing a toast message. it will briefly appear with our instructions. this is the line to get it done:
                Toast.makeText(getApplicationContext(), "Select " + getString(R.string.appName), Toast.LENGTH_SHORT).show();
     
                //now that we are done, close the splash activity since we don't need it anymore.
                finish();
            }
        });
    }
 
    @Override
    protected void onResume() {
        super.onResume();
    }
 
    @Override
    protected void onStop() {
        super.onStop();
    }
 
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
