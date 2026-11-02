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
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.ArrayList;

public final class OverlayService extends Service {
    static final String ACTION_STOP = "nl.dartspen.overlay.STOP";
    private static final String CHANNEL = "darts_pen_overlay";
    private static final int NOTIFICATION = 412;
    // The non-touchable canvas alpha is below the Android 12 obscuring threshold.
    private static final float PLAY_ALPHA = 0.70f;
    private static final int[] COLORS = {
            0xFF22D3EE, 0xFFFACC15, 0xFFFB7185, 0xFF4ADE80,
            0xFFFFFFFF, 0xFFFF3333, 0xFFFF7A00, 0xFF00B5FF,
            0xFF2563EB, 0xFF1E40AF, 0xFF2DD4BF, 0xFF059669,
            0xFFA3E635, 0xFF84CC16, 0xFF9333EA, 0xFF7C3AED,
            0xFFEC4899, 0xFFD946EF, 0xFF9CA3AF, 0xFF111827,
            0xFF000000, 0xFF8B5E3C, 0xFFE0E7FF, 0xFFFFF3B0
    };
    private static final float[] WIDTHS = {1f, 2f, 3f, 4f, 6f, 8f, 12f};
    private static final float[] ERASERS = {4f, 8f, 16f, 32f};

    private WindowManager windows;
    private WindowManager.LayoutParams canvasParams, toolbarParams, targetParams;
    private LinearLayout targetBar, targetRows;
    private ScrollView targetScroll;
    private TextView targetToggle;
    private final ArrayList<TextView> targetButtons = new ArrayList<>();
    private boolean targetBarAdded, targetCollapsed;
    private LineCanvasView canvas;
    private LinearLayout toolbar, options, versionPanel, versionRows;
    private GridLayout palette;
    private ScrollView versionScroll;
    private TextView mode, undo, clear, tool, color, width, eraser, versions, close, saveVersion;
    private StrokeStore store;
    private SharedPreferences ui;
    private int colorIndex, widthIndex, eraserIndex;
    private boolean straight, erasing, editing = true, canvasAdded, toolbarAdded;
    private String lastProfile = "";

    @Override public void onCreate() {
        super.onCreate();
        windows = (WindowManager)getSystemService(WINDOW_SERVICE);
        store = new StrokeStore(this);
        ui = getSharedPreferences("ui_options", MODE_PRIVATE);
        colorIndex = Math.floorMod(store.colorIndex(), COLORS.length);
        widthIndex = Math.floorMod(store.widthIndex(), WIDTHS.length);
        eraserIndex = Math.floorMod(ui.getInt("eraser_size", 0), ERASERS.length);
        straight = store.straight();
        erasing = false;
        targetCollapsed = ui.getBoolean("target_bar_collapsed", false);
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
            try { showOverlay(); }
            catch (RuntimeException e) {
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
                .setContentText("Richtlijnen boven het spel · Stop via melding of ×")
                .setContentIntent(openIntent).setOngoing(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent).build())
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else startForeground(NOTIFICATION, note);
    }

    private void showOverlay() {
        canvas = new LineCanvasView(this, store, new LineCanvasView.Tools() {
            @Override public int color() { return COLORS[colorIndex]; }
            @Override public float widthDp() { return WIDTHS[widthIndex]; }
            @Override public boolean straight() { return straight; }
            @Override public boolean eraser() { return erasing; }
            @Override public float eraserDp() { return ERASERS[eraserIndex]; }
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
        eraser = chip("Gum", 0xFF334155);
        top.addView(eraser);
        close = chip("×", 0xFF991B1B);
        close.setTextSize(20f);
        top.addView(close);

        options = new LinearLayout(this);
        options.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.addView(options);
        tool = chip("Rechte lijn", 0xFF334155);
        options.addView(tool);
        color = chip("Kleuren", 0xFF334155);
        options.addView(color);
        width = chip("4 dp", 0xFF334155);
        options.addView(width);
        versions = chip("Versies", 0xFF334155);
        options.addView(versions);
        clear = chip("Wis", 0xFF7F1D1D);
        options.addView(clear);

        palette = new GridLayout(this);
        palette.setColumnCount(8);
        palette.setVisibility(View.GONE);
        toolbar.addView(palette);
        for (int i = 0; i < COLORS.length; i++) {
            final int selected = i;
            TextView swatch = chip(" ", COLORS[i]);
            swatch.setWidth(dp(35));
            swatch.setHeight(dp(35));
            swatch.setMinHeight(dp(35));
            swatch.setPadding(dp(2), dp(2), dp(2), dp(2));
            swatch.setOnClickListener(v -> {
                colorIndex = selected;
                store.colorIndex(colorIndex);
                palette.setVisibility(View.GONE);
                updateToolbar();
            });
            palette.addView(swatch);
        }

        versionPanel = new LinearLayout(this);
        versionPanel.setOrientation(LinearLayout.VERTICAL);
        versionPanel.setMinimumWidth(dp(306));
        versionPanel.setVisibility(View.GONE);
        toolbar.addView(versionPanel);
        saveVersion = chip("+ Huidige lijnen bewaren", 0xFF0F766E);
        versionPanel.addView(saveVersion);
        versionScroll = new ScrollView(this);
        versionScroll.setFillViewport(false);
        versionPanel.addView(versionScroll);
        versionRows = new LinearLayout(this);
        versionRows.setOrientation(LinearLayout.VERTICAL);
        versionScroll.addView(versionRows);
        saveVersion.setOnClickListener(v -> {
            String profile = canvas.layerProfile();
            if (store.saveVersion(profile, null)) {
                toast("Versie bewaard");
                refreshVersions();
            } else toast("Opslaan niet gelukt");
        });

        toolbarParams = new WindowManager.LayoutParams(-2, -2,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        toolbarParams.gravity = Gravity.TOP | Gravity.LEFT;
        toolbarParams.x = dp(8);
        toolbarParams.y = dp(85);
        windows.addView(toolbar, toolbarParams);
        toolbarAdded = true;

        mode.setOnClickListener(v -> setEditing(!editing));
        close.setOnClickListener(v -> stopSelf());
        undo.setOnClickListener(v -> { if (editing) canvas.undo(); });
        eraser.setOnClickListener(v -> {
            erasing = !erasing;
            palette.setVisibility(View.GONE);
            updateToolbar();
        });
        clear.setOnClickListener(v -> toast("Houd Wis ingedrukt om alle lijnen te wissen"));
        clear.setOnLongClickListener(v -> {
            if (editing) {
                canvas.clear();
                toast("Lijnen gewist; opgeslagen versies blijven bestaan");
            }
            return true;
        });
        tool.setOnClickListener(v -> {
            erasing = false;
            straight = !straight;
            store.straight(straight);
            updateToolbar();
        });
        color.setOnClickListener(v -> {
            boolean opening = palette.getVisibility() != View.VISIBLE;
            versionPanel.setVisibility(View.GONE);
            palette.setVisibility(opening ? View.VISIBLE : View.GONE);
            updateToolbar();
        });
        width.setOnClickListener(v -> {
            if (erasing) {
                eraserIndex = (eraserIndex + 1) % ERASERS.length;
                ui.edit().putInt("eraser_size", eraserIndex).apply();
            } else {
                widthIndex = (widthIndex + 1) % WIDTHS.length;
                store.widthIndex(widthIndex);
            }
            updateToolbar();
        });
        versions.setOnClickListener(v -> {
            boolean opening = versionPanel.getVisibility() != View.VISIBLE;
            palette.setVisibility(View.GONE);
            versionPanel.setVisibility(opening ? View.VISIBLE : View.GONE);
            if (opening) refreshVersions();
            updateToolbar();
        });
        attachDrag(drag);
        canvas.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, orr, ob) -> {
            if (toolbarAdded) toolbar.post(this::restorePosition);
            if (targetBarAdded) targetBar.post(() -> { resizeTargetList(); refreshTargetBar(); });
        });
        toolbar.post(this::restorePosition);
        showTargetBar();
        updateToolbar();
    }


    // A separate right-side overlay is clickable even while the drawing
    // canvas is set to pass touches through to Darts Club.
    private void showTargetBar() {
        targetBar = new LinearLayout(this);
        targetBar.setOrientation(LinearLayout.VERTICAL);
        targetBar.setPadding(dp(3), dp(4), dp(3), dp(4));
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xE5172033);
        background.setCornerRadius(dp(10));
        background.setStroke(dp(1), 0xFF475569);
        targetBar.setBackground(background);

        targetToggle = chip("≡", 0xFF334155);
        targetToggle.setTextSize(14f);
        targetToggle.setGravity(Gravity.CENTER);
        targetToggle.setMinWidth(dp(48));
        targetBar.addView(targetToggle);
        targetToggle.setOnClickListener(v -> {
            targetCollapsed = !targetCollapsed;
            ui.edit().putBoolean("target_bar_collapsed", targetCollapsed).apply();
            refreshTargetBar();
        });

        targetScroll = new ScrollView(this);
        targetScroll.setVerticalScrollBarEnabled(false);
        targetScroll.setFillViewport(false);
        targetBar.addView(targetScroll);
        targetRows = new LinearLayout(this);
        targetRows.setOrientation(LinearLayout.VERTICAL);
        targetScroll.addView(targetRows);
        for (String target : StrokeStore.TARGETS) {
            TextView button = chip(target, 0xFF334155);
            button.setTextSize(12f);
            button.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(dp(48), dp(38));
            itemParams.setMargins(dp(1), dp(2), dp(1), dp(2));
            button.setLayoutParams(itemParams);
            button.setGravity(Gravity.CENTER);
            targetRows.addView(button);
            targetButtons.add(button);
            button.setOnClickListener(v -> {
                canvas.selectTarget(target);
                if (versionPanel != null) versionPanel.setVisibility(View.GONE);
                if (palette != null) palette.setVisibility(View.GONE);
                refreshTargetBar();
                updateToolbar();
            });
        }
        targetParams = new WindowManager.LayoutParams(-2, -2,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        targetParams.gravity = Gravity.TOP | Gravity.RIGHT;
        targetParams.x = dp(4);
        targetParams.y = dp(80);
        resizeTargetList();
        windows.addView(targetBar, targetParams);
        targetBarAdded = true;
        refreshTargetBar();
    }

    private void resizeTargetList() {
        if (targetScroll == null || canvas == null) return;
        int h = canvas.getHeight();
        if (h <= 0) h = getResources().getDisplayMetrics().heightPixels;
        int maxListHeight = Math.max(dp(110), Math.min(dp(580), h - dp(155)));
        targetScroll.setLayoutParams(new LinearLayout.LayoutParams(dp(55), maxListHeight));
    }

    private void refreshTargetBar() {
        if (targetBar == null || canvas == null) return;
        String current = canvas.target();
        targetToggle.setText(targetCollapsed ? current + " ◀" : "≡");
        targetScroll.setVisibility(targetCollapsed ? View.GONE : View.VISIBLE);
        for (int i = 0; i < targetButtons.size(); i++) {
            TextView button = targetButtons.get(i);
            boolean selected = StrokeStore.TARGETS[i].equals(current);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(7));
            bg.setColor(selected ? 0xFF0891B2 : 0xFF334155);
            if (selected) bg.setStroke(dp(1), 0xFFFFFFFF);
            button.setBackground(bg);
            button.setTextColor(selected ? Color.WHITE : 0xFFDCE6F1);
        }
    }

    private void refreshVersions() {
        if (versionRows == null || canvas == null) return;
        versionRows.removeAllViews();
        List<StrokeStore.Version> stored = store.listVersions(canvas.layerProfile());
        if (stored.isEmpty()) {
            TextView empty = chip("Nog geen versies opgeslagen", 0xFF334155);
            versionRows.addView(empty);
        }
        for (StrokeStore.Version saved : stored) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            versionRows.addView(row);
            TextView label = chip(saved.label, 0xFF283547);
            label.setTextSize(11f);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, dp(37), 1f);
            labelParams.setMargins(dp(2), dp(2), dp(2), dp(2));
            label.setLayoutParams(labelParams);
            row.addView(label);
            TextView restore = chip("Herstel", 0xFF0F766E);
            row.addView(restore);
            restore.setOnClickListener(v -> {
                if (store.restoreVersion(canvas.layerProfile(), saved.id)) {
                    canvas.reloadFromStorage();
                    refreshVersions();
                    toast("Versie hersteld; vorige lijnen bewaard als back-up");
                } else toast("Herstellen niet gelukt");
            });
            TextView delete = chip("×", 0xFF7F1D1D);
            row.addView(delete);
            delete.setOnClickListener(v -> {
                store.deleteVersion(canvas.layerProfile(), saved.id);
                refreshVersions();
            });
        }
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1,
                dp(Math.min(stored.size() * 42 + (stored.isEmpty() ? 40 : 0), 210)));
        versionScroll.setLayoutParams(scrollParams);
    }

    private TextView chip(String label, int background) {
        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(12f);
        text.setGravity(Gravity.CENTER);
        text.setMinHeight(dp(36));
        text.setPadding(dp(7), dp(6), dp(7), dp(6));
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
        if (!edit) {
            palette.setVisibility(View.GONE);
            versionPanel.setVisibility(View.GONE);
        }
        updateToolbar();
    }

    private void updateToolbar() {
        if (mode == null) return;
        mode.setText(editing ? "▶ Spelen" : "✎ Tekenen");
        undo.setVisibility(editing ? View.VISIBLE : View.GONE);
        eraser.setVisibility(editing ? View.VISIBLE : View.GONE);
        options.setVisibility(editing ? View.VISIBLE : View.GONE);
        eraser.setText(erasing ? "✓ Gum" : "Gum");
        eraser.setTextColor(erasing ? 0xFFFCA5A5 : Color.WHITE);
        tool.setText(straight ? "Recht" : "Vrij");
        color.setText("● Kleuren");
        color.setTextColor(COLORS[colorIndex]);
        width.setText((int)(erasing ? ERASERS[eraserIndex] : WIDTHS[widthIndex]) +
                (erasing ? " gum" : " dp"));
        versions.setText(versionPanel.getVisibility() == View.VISIBLE ? "Versies ▲" : "Versies");
        for (int i = 0; i < palette.getChildCount(); i++) {
            TextView swatch = (TextView)palette.getChildAt(i);
            swatch.setText(i == colorIndex ? "✓" : " ");
            swatch.setTextColor(Color.luminance(COLORS[i]) > .48
                    ? Color.BLACK : Color.WHITE);
        }
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
                            initialX + Math.round(e.getRawX() - downX)));
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
            if (versionPanel.getVisibility() == View.VISIBLE) refreshVersions();
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
        if (targetBarAdded) {
            try { windows.removeView(targetBar); } catch (RuntimeException ignored) { }
            targetBarAdded = false;
        }
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
