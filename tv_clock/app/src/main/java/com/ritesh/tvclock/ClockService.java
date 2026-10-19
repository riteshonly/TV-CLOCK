package com.ritesh.tvclock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** One TextView overlay; redraws only on the system minute tick. */
public class ClockService extends Service {
    private WindowManager wm;
    private TextView view;
    private boolean h24;

    private final BroadcastReceiver tick = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { update(); }
    };

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, notification());
        if (view == null && (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this))) addOverlay();
        return START_STICKY;
    }

    private Notification notification() {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("clock", "Clock", NotificationManager.IMPORTANCE_MIN);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
            b = new Notification.Builder(this, "clock");
        } else {
            b = new Notification.Builder(this);
        }
        return b.setContentTitle("TV Clock").setSmallIcon(android.R.drawable.ic_dialog_info).build();
    }

    private void addOverlay() {
        SharedPreferences p = getSharedPreferences("clock", MODE_PRIVATE);
        h24 = p.getBoolean("h24", false);
        String corner = p.getString("corner", "top_right");
        int size = p.getInt("size", 28);

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        view = new TextView(this);
        view.setTextColor(Color.WHITE);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        view.setShadowLayer(4, 0, 0, Color.BLACK);
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        view.setPadding(pad * 2, pad, pad * 2, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x80000000);
        bg.setCornerRadius(pad * 2);
        view.setBackground(bg);

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = (corner.startsWith("bottom") ? Gravity.BOTTOM : Gravity.TOP)
                | (corner.endsWith("left") ? Gravity.START : Gravity.END);
        int margin = (int) (24 * getResources().getDisplayMetrics().density); // TV overscan safety
        lp.x = margin;
        lp.y = margin;
        wm.addView(view, lp);

        IntentFilter f = new IntentFilter(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        registerReceiver(tick, f);
        update();
    }

    private void update() {
        if (view != null)
            view.setText(new SimpleDateFormat(h24 ? "HH:mm" : "h:mm a", Locale.getDefault()).format(new Date()));
    }

    @Override
    public void onDestroy() {
        if (view != null) {
            unregisterReceiver(tick);
            wm.removeView(view);
            view = null;
        }
        super.onDestroy();
    }
}
