package com.ritesh.tvclock;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

/** Saves optional settings passed as extras, then (re)starts the overlay service. */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Intent i = getIntent();
        SharedPreferences.Editor e = getSharedPreferences("clock", MODE_PRIVATE).edit();
        if (i.hasExtra("corner")) e.putString("corner", i.getStringExtra("corner")); // top_right|top_left|bottom_right|bottom_left
        if (i.hasExtra("size")) e.putInt("size", i.getIntExtra("size", 28));          // sp
        if (i.hasExtra("h24")) e.putBoolean("h24", i.getBooleanExtra("h24", false));
        e.apply();
        stopService(new Intent(this, ClockService.class));
        BootReceiver.start(this);
        finish();
    }
}
