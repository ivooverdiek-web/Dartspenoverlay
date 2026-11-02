package nl.dartspen.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class OverlayService extends Service {
    static final String ACTION_STOP = "nl.dartspen.overlay.STOP";
    private static final String CHANNEL = "darts_pen_overlay";
    private static final int NOTIFICATION = 412;
    // Below Android 12's single-overlay obscuring threshold for touch pass-through.
    private static final float PLAY_ALPHA = 0.70f;
    private static final int[] COLORS = {
            0xFF22D3EE, 0xFFFACC15, 0xFFFB7185, 0xFF4ADE80, 0xFFFFFFFF
    };
    private static final float[] WIDTHS = {2f, 4f, 7f};
    private WindowManager windows;
    private WindowManager.LayoutParams canvasParams, toolbarParams;
    private LineCanvasView canvas;
    private LinearLayout toolbar, options;
    private TextView mode, undo, clear, tool, color, width;
    private StrokeStore store;
    private SharedPreferences ui;
    private int colorIndex, widthIndex;
    private boolean straight, editing = true, canvasAdded, toolbarAdded;
    private String lastProfile = "";

    @Override public void onCreate() {
        super.onCreate();
        windows = (WindowManager)getSystemService(WINDOW_SERVICE);
        store = new StrokeStore(this);
        ui = getSharedPreferences("ui_options", MODE_PRIVATE);
        colorIndex = Math.floorMod(store.colorIndex(), COLORS.length);
        widthIndex = Math.floorMod(store.widthIndex(), WIDTHS.length);
        straight = store.straight();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startNotification();
        if (!canvasAdded) {
            try {
                showOverlay();
            } catch (RuntimeException e) {
                removeOverlay();
                Toast.makeText(this, "Kan overlay niet openen. Controleer de toestemming.",
                        Toast.LENGTH_LONG).show();
                stopSelf();
            }
        }
        return START_NOT_STICKY;
    }

    private void startNotification() {
        NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Darts Pen Overlay",
                NotificationManager.IMPORTANCE_LOW);
        manager.createNotificationChannel(channel);
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openIntent = PendingIntent.getActivity(this, 1, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent stop = new Intent(this, OverlayService.class).setAction(ACTION_STOP);
        PendingIntent stopIntent = PendingIntent.getService(this, 2, stop,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification note = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setContentTitle("Darts Pen Overlay actief")
                .setContentText("Richtlijnen blijven zichtbaar boven het spel")
                .setContentIntent(openIntent).setOngoing(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent).build())
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION, note,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else startForeground(NOTIFICATION, note);
    }

    private void showOverlay() {
        canvas = new LineCanvasView(this, store, new LineCanvasView.Tools() {
            @Override public int color() { return COLORS[colorIndex]; }
            @Override public float widthDp() { return WIDTHS[widthIndex]; }
            @Override public boolean straight() { return straight; }
        });
        canvasParams = new WindowManager.LayoutParams(-1, -1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        canvasParams.gravity = Gravity.TOP | Gravity.START;
        canvasParams.alpha = 1f;
        windows.addView(canvas, canvasParams);
        canvasAdded = true;

        toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.VERTICAL);
        toolbar.setPadding(dp(5), dp(5), dp(5), dp(5));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xEF172033);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), 0xFF475569);
        toolbar.setBackground(bg);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.addView(top);
        TextView drag = chip("↕", 0xFF334155);
        top.addView(drag);
        mode = chip("▶ Spelen", 0xFF0891B2);
        top.addView(mode);
        undo = chip("↶", 0xFF334155);
        top.addView(undo);
        clear = chip("Wis", 0xFF7F1D1D);
        top.addView(clear);
        options = new LinearLayout(this);
        options.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.addView(options);
        tool = chip("Rechte lijn", 0xFF334155);
        options.addView(tool);
        color = chip("● Kleur", 0xFF334155);
        options.addView(color);
        width = chip("4 dp", 0xFF334155);
        options.addView(width);

        toolbarParams = new WindowManager.LayoutParams(-2, -2,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        toolbarParams.gravity = Gravity.TOP | Gravity.RIGHT;
        toolbarParams.x = dp(8);
        toolbarParams.y = dp(90);
        windows.addView(toolbar, toolbarParams);
        toolbarAdded = true;

        mode.setOnClickListener(v -> setEditing(!editing));
        undo.setOnClickListener(v -> { if (editing) canvas.undo(); });
        clear.setOnClickListener(v -> toast("Houd Wis ingedrukt om lijnen te wissen"));
        clear.setOnLongClickListener(v -> {
            if (editing) {
                canvas.clear();
                toast("Lijnen van deze schermstand gewist");
            }
            return true;
        });
        tool.setOnClickListener(v -> {
            straight = !straight;
            store.straight(straight);
            updateToolbar();
        });
        color.setOnClickListener(v -> {
            colorIndex = (colorIndex + 1) % COLORS.length;
            store.colorIndex(colorIndex);
            updateToolbar();
        });
        width.setOnClickListener(v -> {
            widthIndex = (widthIndex + 1) % WIDTHS.length;
            store.widthIndex(widthIndex);
            updateToolbar();
        });
        attachDrag(drag);
        canvas.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, orr, ob) -> {
            if (toolbarAdded) toolbar.post(this::restorePosition);
        });
        toolbar.post(this::restorePosition);
        updateToolbar();
    }

    private TextView chip(String label, int background) {
        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(12f);
        text.setGravity(Gravity.CENTER);
        text.setMinHeight(dp(36));
        text.setPadding(dp(8), dp(6), dp(8), dp(6));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(background);
        shape.setCornerRadius(dp(8));
        text.setBackground(shape);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        text.setLayoutParams(params);
        return text;
    }

    private void setEditing(boolean edit) {
        editing = edit;
        canvas.setEditable(edit);
        canvasParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | (edit ? 0 : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        canvasParams.alpha = edit ? 1f : PLAY_ALPHA;
        windows.updateViewLayout(canvas, canvasParams);
        updateToolbar();
    }

    private void updateToolbar() {
        mode.setText(editing ? "▶ Spelen" : "✎ Tekenen");
        undo.setVisibility(editing ? View.VISIBLE : View.GONE);
        clear.setVisibility(editing ? View.VISIBLE : View.GONE);
        options.setVisibility(editing ? View.VISIBLE : View.GONE);
        tool.setText(straight ? "Rechte lijn" : "Vrij tekenen");
        color.setTextColor(COLORS[colorIndex]);
        width.setText((int)WIDTHS[widthIndex] + " dp");
    }

    private void attachDrag(TextView handle) {
        handle.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            int initialX, initialY;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    downX = e.getRawX(); downY = e.getRawY();
                    initialX = toolbarParams.x; initialY = toolbarParams.y;
                    return true;
                }
                if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    toolbarParams.x = Math.max(0, Math.min(
                            Math.max(0, canvas.getWidth() - dp(40)),
                            initialX - Math.round(e.getRawX() - downX)));
                    toolbarParams.y = Math.max(0, Math.min(
                            Math.max(0, canvas.getHeight() - dp(40)),
                            initialY + Math.round(e.getRawY() - downY)));
                    windows.updateViewLayout(toolbar, toolbarParams);
                    return true;
                }
                if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                    String profile = canvas.profile();
                    ui.edit().putInt("x_" + profile, toolbarParams.x)
                            .putInt("y_" + profile, toolbarParams.y).apply();
                    return true;
                }
                return true;
            }
        });
    }

    private void restorePosition() {
        if (!toolbarAdded || canvas == null) return;
        String profile = canvas.profile();
        if (!profile.equals(lastProfile)) {
            toolbarParams.x = ui.getInt("x_" + profile, dp(8));
            toolbarParams.y = ui.getInt("y_" + profile, dp(90));
            lastProfile = profile;
        }
        toolbarParams.x = Math.max(0, Math.min(toolbarParams.x,
                Math.max(0, canvas.getWidth() - dp(40))));
        toolbarParams.y = Math.max(0, Math.min(toolbarParams.y,
                Math.max(0, canvas.getHeight() - dp(40))));
        try { windows.updateViewLayout(toolbar, toolbarParams); }
        catch (RuntimeException ignored) { }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public void onDestroy() {
        removeOverlay();
        super.onDestroy();
    }
    private void removeOverlay() {
        if (toolbarAdded) {
            try { windows.removeView(toolbar); } catch (RuntimeException ignored) { }
            toolbarAdded = false;
        }
        if (canvasAdded) {
            try { windows.removeView(canvas); } catch (RuntimeException ignored) { }
            canvasAdded = false;
        }
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
