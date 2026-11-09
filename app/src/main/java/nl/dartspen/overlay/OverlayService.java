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
import android.graphics.PointF;
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
    private static final float[] NUDGE_STEPS = {0.5f, 1f, 2f, 5f};
    private static final long[] EDIT_DURATION_STEPS_MS = {10L, 100L, 1000L};
    // Physical captured swipe is short; saved guide can extend it safely.
    private static final float[] PRACTICE_LENGTHS = {1f, 2f, 3f, 4f, 6f, 8f};
    private static final float[] PRACTICE_WIDTHS = {1f, 1.5f, 2f, 3f};
    // Replay speed 100% = recorded duration; lower percentages slow the swipe.
    // Keep the original 41 selection indices stable for existing users.
    // The 20 singles are appended so old T/D/Bull saved slots are untouched.
    private static final String[] REPLAY_TARGETS = createReplayTargets();
    private static final String[] QUICK_TARGETS = {
        "T20", "T19", "T18",
        "T17", "D20", "D17",
        "D16", "D18", "Bull"
    };

    private static String[] createReplayTargets() {
        String[] targets = new String[61];
        targets[0] = "T20";
        targets[1] = "D20";
        targets[2] = "Bull";
        int index = 3;
        for (int n = 19; n >= 1; n--) {
            targets[index++] = "T" + n;
            targets[index++] = "D" + n;
        }
        for (int n = 20; n >= 1; n--) {
            targets[index++] = "S" + n;
        }
        return targets;
    }
    // First 3 = familiar widths, last 3 = deliberately oversized test lines.
    private static final String[] PRO_WIDTH_LABELS = {
            "Lijn: dik 3,25", "Lijn: extra 4,75", "Lijn: max 6,5",
            "Lijn: groot 12", "Lijn: vinger 24", "Lijn: VINGER 48"
    };

    private WindowManager windows;
    private WindowManager.LayoutParams canvasParams, toolbarParams, targetParams, remoteParams;
    private ThrowReplayRemote remotePanel;
    private boolean remoteOpen;
    private ThrowPathEditorView throwEditor;
    private WindowManager.LayoutParams throwEditorParams;
    private String editedThrowTarget = "", editedThrowProfile = "";
    private long editedThrowDurationMs, editedThrowOriginalDurationMs;
    private int editedThrowStepIndex, editedDurationStepIndex;
    private LinearLayout targetBar, targetRows;
    private ScrollView targetScroll;
    private TextView targetToggle;
    private final ArrayList<TextView> targetButtons = new ArrayList<>();
    private final ArrayList<TextView> quickReplayButtons = new ArrayList<>();
    private boolean targetBarAdded, targetCollapsed;
    private LineCanvasView canvas;
    private LinearLayout toolbar, toolbarTop, toolbarExpandedContent, options, practiceRow, practiceSettingsRow, replayRow, quickPlayGrid, remoteLauncherRow, videoFilterRow, proActions, proStyleActions, proFinePanel, moveControls, versionPanel, versionRows;
    private GridLayout palette;
    private ScrollView versionScroll;
    private TextView dragHandle, minimizeMenuButton, compactBubble, mode, undo, clear, tool, color, width, eraser, move, moveStep, versions, close, saveVersion, calibrate, proFineToggle, proFineStep, glowButton, lineThicknessButton, practiceToggle, saveThrow, skipThrow, practiceLengthButton, practiceWidthButton, replayTargetButton, saveReplayButton, playReplayButton, remoteLauncherButton, videoRingButton, videoNumberButton;
    private StrokeStore store;
    private SharedPreferences ui;
    private int colorIndex, widthIndex, eraserIndex, nudgeIndex, glowLevel, proLineThickness, practiceLengthIndex, practiceWidthIndex, replayTargetIndex, replaySequence, replaySpeedPercent, replayRingIndex;
    private boolean straight, erasing, editing = true, canvasAdded, toolbarAdded, proFineOpen;
    private boolean practiceMode, replayPending, menuMinimized;
    private ArrayList<PointF> lastPracticeSwipe;
    private ArrayList<Long> lastPracticePointTimesMs;
    private ArrayList<Long> editedThrowTimesMs, editedThrowOriginalTimesMs;
    private long editedThrowTimingSourceDurationMs, editedThrowOriginalTimingDurationMs;
    private long lastPracticeDurationMs;
    private RecordedThrowStore recordedThrows;
    private String lastPracticeScreenProfile = "";
    private String lastPracticeTarget = "";
    private String lastProfile = "";
    private String lastThicknessTarget = "";
    private String lastVideoFilterTarget = "";
    private int videoRingIndex = 0;
    private int videoNumberIndex = 0;
    private static final String[] VIDEO_RINGS = {"T", "D", "All"};
    private static final int[] VIDEO_NUMBERS = {
            0,20,1,18,4,13,6,10,15,2,17,3,19,7,16,8,11,14,9,12,5,25
    };

    @Override public void onCreate() {
        super.onCreate();
        windows = (WindowManager)getSystemService(WINDOW_SERVICE);
        store = new StrokeStore(this);
        recordedThrows = new RecordedThrowStore(this);
        ui = getSharedPreferences("ui_options", MODE_PRIVATE);
        colorIndex = Math.floorMod(store.colorIndex(), COLORS.length);
        widthIndex = Math.floorMod(store.widthIndex(), WIDTHS.length);
        eraserIndex = Math.floorMod(ui.getInt("eraser_size", 0), ERASERS.length);
        nudgeIndex = Math.floorMod(ui.getInt("nudge_step", 1), NUDGE_STEPS.length);
        glowLevel = Math.max(0, Math.min(3, ui.getInt("pro_glow_level", 2)));
        // Start this explicit finger-size experiment at the full 48 dp.
        // Tapping the line button once cycles back to the original 3.25 dp.
        if (!ui.getBoolean("finger_test_v28_applied", false)) {
            proLineThickness = 5;
            ui.edit().putInt("pro_line_thickness", 5)
                    .putBoolean("finger_test_v28_applied", true).apply();
        } else {
            proLineThickness = Math.max(0, Math.min(
                    PRO_WIDTH_LABELS.length - 1, ui.getInt("pro_line_thickness", 5)));
        }
        practiceLengthIndex = Math.floorMod(ui.getInt("practice_length_index", 3), PRACTICE_LENGTHS.length);
        practiceWidthIndex = Math.floorMod(ui.getInt("practice_width_index", 1), PRACTICE_WIDTHS.length);
        replayTargetIndex = Math.floorMod(ui.getInt("replay_target_index", 0), REPLAY_TARGETS.length);
        replaySpeedPercent = Math.max(1, Math.min(100, ui.getInt("replay_speed_percent", 100)));
        replayRingIndex = Math.max(0, Math.min(2, ui.getInt("replay_ring_index", 2)));
        straight = store.straight();
        erasing = false;
        targetCollapsed = ui.getBoolean("target_bar_collapsed", false);
        menuMinimized = ui.getBoolean("toolbar_minimized", false);
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
        canvas.setProGlowLevel(glowLevel);
        canvas.setProLineThickness(proLineThickness);
        canvas.setPracticeSwipeListener(this::onPracticeSwipe);
        canvas.setCalibrationListener((stage, finished) -> {
            if (finished) {
                toast("PRO-lijnen uitgelijnd en opgeslagen. Oude versie staat onder Versies.");
            } else if (stage == 1) {
                toast("PRO ijken: tik precies op het MIDDEN van de bull");
            } else if (stage == 2) {
                toast("Tik nu midden bovenaan op de BUITENSTE gouden bordrand");
            }
            updateToolbar();
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
        toolbar.setPadding(dp(3), dp(3), dp(3), dp(3));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xEF172033);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), 0xFF475569);
        toolbar.setBackground(bg);

        toolbarTop = new LinearLayout(this);
        toolbarTop.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.addView(toolbarTop);
        dragHandle = chip("↕", 0xFF334155);
        dragHandle.setMinHeight(dp(30));
        dragHandle.setPadding(dp(5), dp(1), dp(5), dp(1));
        toolbarTop.addView(dragHandle);
        mode = chip("▶ Spelen", 0xFF0891B2);
        toolbarTop.addView(mode);
        mode.setVisibility(View.GONE);
        undo = chip("↶", 0xFF334155);
        toolbarTop.addView(undo);
        eraser = chip("Gum", 0xFF334155);
        toolbarTop.addView(eraser);
        move = chip("↔ Verplaats", 0xFF334155);
        toolbarTop.addView(move);
        close = chip("×", 0xFF991B1B);
        close.setTextSize(17f);
        close.setMinHeight(dp(30));
        close.setPadding(dp(5), dp(1), dp(5), dp(1));
        toolbarTop.addView(close);
        minimizeMenuButton = chip("−", 0xFF334155);
        minimizeMenuButton.setTextSize(17f);
        minimizeMenuButton.setMinHeight(dp(30));
        minimizeMenuButton.setMinWidth(dp(29));
        minimizeMenuButton.setPadding(dp(5), dp(1), dp(5), dp(1));
        toolbarTop.addView(minimizeMenuButton);
        compactBubble = chip("🎯", 0xFF0F766E);
        compactBubble.setTextSize(18f);
        compactBubble.setWidth(dp(43));
        compactBubble.setHeight(dp(43));
        compactBubble.setContentDescription("Menu openen");
        compactBubble.setVisibility(View.GONE);
        toolbarTop.addView(compactBubble);

        videoFilterRow = new LinearLayout(this);
        videoFilterRow.setOrientation(LinearLayout.HORIZONTAL);
        videoFilterRow.setVisibility(View.GONE);
        toolbar.addView(videoFilterRow);
        videoRingButton = chip("Treble (20)", 0xFF854D0E);
        videoNumberButton = chip("Alle nummers", 0xFF334155);
        videoFilterRow.addView(videoRingButton);
        videoFilterRow.addView(videoNumberButton);

        practiceRow = new LinearLayout(this);
        practiceRow.setOrientation(LinearLayout.HORIZONTAL);
        practiceRow.setVisibility(View.GONE);
        toolbar.addView(practiceRow);
        practiceToggle = chip("◎ Vang worp", 0xFF0F766E);
        saveThrow = chip("★ Bewaar", 0xFF166534);
        skipThrow = chip("× Overslaan", 0xFF334155);
        practiceRow.addView(practiceToggle);
        practiceToggle.setTextSize(11f);
        practiceToggle.setMinHeight(dp(31));
        practiceToggle.setPadding(dp(4), dp(2), dp(4), dp(2));
        saveThrow.setVisibility(View.GONE);
        skipThrow.setVisibility(View.GONE);

        practiceSettingsRow = new LinearLayout(this);
        practiceSettingsRow.setOrientation(LinearLayout.HORIZONTAL);
        practiceSettingsRow.setVisibility(View.GONE);
        toolbar.addView(practiceSettingsRow);
        practiceLengthButton = chip("↗ Lengte ×4", 0xFF075985);
        practiceWidthButton = chip("Lijndikte 1,5", 0xFF334155);
        practiceSettingsRow.addView(practiceLengthButton);
        practiceSettingsRow.addView(practiceWidthButton);

        // A saved throw is separate from the existing ★ Bewaar visual guide.
        // ▶ T20 executes only on an explicit user tap, never on target selection.
        replayRow = new LinearLayout(this);
        replayRow.setOrientation(LinearLayout.HORIZONTAL);
        replayRow.setVisibility(View.GONE);
        toolbar.addView(replayRow);
        replayTargetButton = chip("Doel T20", 0xFF334155);
        saveReplayButton = chip("💾 Bewaar T20", 0xFF166534);
        playReplayButton = chip("▶ T20", 0xFF0F766E);
        replayRow.addView(replayTargetButton);
        replayRow.addView(saveReplayButton);
        replayRow.addView(playReplayButton);
        playReplayButton.setVisibility(View.GONE); // The nine buttons already replay.
        replayTargetButton.setTextSize(11f);
        saveReplayButton.setTextSize(11f);
        replayTargetButton.setMinHeight(dp(31));
        saveReplayButton.setMinHeight(dp(31));
        replayTargetButton.setPadding(dp(4), dp(2), dp(4), dp(2));
        saveReplayButton.setPadding(dp(4), dp(2), dp(4), dp(2));

        // QUICK PLAY: exactly nine buttons, in the requested order.
        // Each replays only its OWN saved swipe; long-press opens its editor.
        quickPlayGrid = new LinearLayout(this);
        quickPlayGrid.setOrientation(LinearLayout.VERTICAL);
        quickPlayGrid.setVisibility(View.GONE);
        toolbar.addView(quickPlayGrid);
        for (int row = 0; row < 3; row++) {
            LinearLayout quickRow = new LinearLayout(this);
            quickRow.setOrientation(LinearLayout.HORIZONTAL);
            quickPlayGrid.addView(quickRow);
            for (int col = 0; col < 3; col++) {
                final String targetName = QUICK_TARGETS[row * 3 + col];
                TextView quick = chip(targetName, 0xFF334155);
                quick.setTextSize(12.5f);
                quick.setTypeface(android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD);
                quick.setMinWidth(dp(58));
                quick.setMinHeight(dp(34));
                quick.setPadding(dp(2), dp(1), dp(2), dp(1));
                quick.setContentDescription("Replay recorded " + targetName);
                LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(
                        0, dp(37), 1f);
                cell.setMargins(dp(1), dp(1), dp(1), dp(1));
                quickRow.addView(quick, cell);
                quick.setOnClickListener(v -> playReplayTarget(targetName));
                quick.setOnLongClickListener(v -> {
                    if (recordedThrows.load(canvas.profile(), targetName) == null) {
                        toast("Neem eerst een worp op voor " + targetName);
                    } else {
                        if (!remoteOpen) openReplayRemote();
                        if (remoteOpen) startThrowEdit(targetName);
                    }
                    return true;
                });
                quickReplayButtons.add(quick);
            }
        }

        remoteLauncherRow = new LinearLayout(this);
        remoteLauncherRow.setOrientation(LinearLayout.HORIZONTAL);
        remoteLauncherRow.setVisibility(View.GONE);
        toolbar.addView(remoteLauncherRow);
        remoteLauncherButton = chip("🎛 Remote / bewerken", 0xFF0369A1);
        remoteLauncherButton.setTextSize(11f);
        remoteLauncherButton.setMinHeight(dp(30));
        remoteLauncherButton.setPadding(dp(4), dp(2), dp(4), dp(2));
        remoteLauncherRow.addView(remoteLauncherButton);

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
        proActions = new LinearLayout(this);
        proActions.setOrientation(LinearLayout.HORIZONTAL);
        proActions.setVisibility(View.GONE);
        toolbar.addView(proActions);
        calibrate = chip("◎ PRO ijken", 0xFF0F766E);
        proActions.addView(calibrate);
        proFineToggle = chip("⊞ PRO fijn", 0xFF0369A1);
        proActions.addView(proFineToggle);

        // Keep the PRO controls narrow enough for the Magic V5 cover screen.
        proStyleActions = new LinearLayout(this);
        proStyleActions.setOrientation(LinearLayout.HORIZONTAL);
        proStyleActions.setVisibility(View.GONE);
        toolbar.addView(proStyleActions);
        glowButton = chip("✦ Glow 2", 0xFF7C3AED);
        proStyleActions.addView(glowButton);
        lineThicknessButton = chip("Lijn: VINGER 48", 0xFF0369A1);
        proStyleActions.addView(lineThicknessButton);

        proFinePanel = new LinearLayout(this);
        proFinePanel.setOrientation(LinearLayout.VERTICAL);
        proFinePanel.setVisibility(View.GONE);
        toolbar.addView(proFinePanel);
        TextView fineHint = chip("Corrigeer alle PRO-lijnen tegelijk", 0xFF283547);
        fineHint.setTextSize(11f);
        proFinePanel.addView(fineHint);
        LinearLayout fineArrows = new LinearLayout(this);
        fineArrows.setOrientation(LinearLayout.HORIZONTAL);
        proFinePanel.addView(fineArrows);
        TextView fineLeft = chip("←", 0xFF334155);
        TextView fineUp = chip("↑", 0xFF334155);
        TextView fineDown = chip("↓", 0xFF334155);
        TextView fineRight = chip("→", 0xFF334155);
        proFineStep = chip("1 dp", 0xFF0F766E);
        for (TextView arrow : new TextView[]{fineLeft, fineUp, fineDown, fineRight}) {
            arrow.setMinWidth(dp(41));
            arrow.setTextSize(19f);
            fineArrows.addView(arrow);
        }
        fineArrows.addView(proFineStep);
        fineLeft.setOnClickListener(v -> adjustAllPro(-NUDGE_STEPS[nudgeIndex],0f,1f,0f));
        fineUp.setOnClickListener(v -> adjustAllPro(0f,-NUDGE_STEPS[nudgeIndex],1f,0f));
        fineDown.setOnClickListener(v -> adjustAllPro(0f,NUDGE_STEPS[nudgeIndex],1f,0f));
        fineRight.setOnClickListener(v -> adjustAllPro(NUDGE_STEPS[nudgeIndex],0f,1f,0f));
        proFineStep.setOnClickListener(v -> {
            nudgeIndex = (nudgeIndex+1) % NUDGE_STEPS.length;
            ui.edit().putInt("nudge_step", nudgeIndex).apply();
            updateToolbar();
        });
        LinearLayout fineScaleRotate = new LinearLayout(this);
        fineScaleRotate.setOrientation(LinearLayout.HORIZONTAL);
        proFinePanel.addView(fineScaleRotate);
        TextView smaller = chip("−0,5% zoom",0xFF334155);
        TextView bigger = chip("+0,5% zoom",0xFF334155);
        TextView rotateLeft = chip("↶ 0,25°",0xFF334155);
        TextView rotateRight = chip("↷ 0,25°",0xFF334155);
        fineScaleRotate.addView(smaller);
        fineScaleRotate.addView(bigger);
        fineScaleRotate.addView(rotateLeft);
        fineScaleRotate.addView(rotateRight);
        smaller.setOnClickListener(v -> adjustAllPro(0f,0f,0.995f,0f));
        bigger.setOnClickListener(v -> adjustAllPro(0f,0f,1.005f,0f));
        rotateLeft.setOnClickListener(v -> adjustAllPro(0f,0f,1f,-.25f));
        rotateRight.setOnClickListener(v -> adjustAllPro(0f,0f,1f,.25f));

        // Only show the nudging pad when the user chooses to move a line.
        // This avoids cluttering the game with permanent arrow controls.
        moveControls = new LinearLayout(this);
        moveControls.setOrientation(LinearLayout.VERTICAL);
        moveControls.setVisibility(View.GONE);
        toolbar.addView(moveControls);
        TextView hint = chip("Tik op lijn, sleep of gebruik pijltjes", 0xFF283547);
        hint.setTextSize(11f);
        moveControls.addView(hint);
        LinearLayout arrows = new LinearLayout(this);
        arrows.setOrientation(LinearLayout.HORIZONTAL);
        moveControls.addView(arrows);
        TextView left = chip("←", 0xFF334155);
        TextView up = chip("↑", 0xFF334155);
        TextView down = chip("↓", 0xFF334155);
        TextView right = chip("→", 0xFF334155);
        moveStep = chip("1 dp", 0xFF0F766E);
        for (TextView arrow : new TextView[]{left, up, down, right}) {
            arrow.setTextSize(18f);
            arrow.setMinWidth(dp(43));
            arrows.addView(arrow);
        }
        arrows.addView(moveStep);
        left.setOnClickListener(v -> nudgeLine(-NUDGE_STEPS[nudgeIndex], 0f));
        up.setOnClickListener(v -> nudgeLine(0f, -NUDGE_STEPS[nudgeIndex]));
        down.setOnClickListener(v -> nudgeLine(0f, NUDGE_STEPS[nudgeIndex]));
        right.setOnClickListener(v -> nudgeLine(NUDGE_STEPS[nudgeIndex], 0f));
        moveStep.setOnClickListener(v -> {
            nudgeIndex = (nudgeIndex + 1) % NUDGE_STEPS.length;
            ui.edit().putInt("nudge_step", nudgeIndex).apply();
            updateToolbar();
        });

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

        // Collect every optional/advanced panel into one collapsible section.
        // Its children keep their own visibility state when the menu is shrunk.
        toolbarExpandedContent = new LinearLayout(this);
        toolbarExpandedContent.setOrientation(LinearLayout.VERTICAL);
        while (toolbar.getChildCount() > 1) {
            View child = toolbar.getChildAt(1);
            toolbar.removeViewAt(1);
            toolbarExpandedContent.addView(child);
        }
        toolbar.addView(toolbarExpandedContent);

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

        minimizeMenuButton.setOnClickListener(v -> setMenuMinimized(true));
        compactBubble.setOnClickListener(v -> setMenuMinimized(false));
        mode.setOnClickListener(v -> setEditing(!editing));
        videoRingButton.setOnClickListener(v -> {
            videoRingIndex = (videoRingIndex + 1) % VIDEO_RINGS.length;
            saveVideoFilter();
        });
        videoNumberButton.setOnClickListener(v -> {
            videoNumberIndex = (videoNumberIndex + 1) % VIDEO_NUMBERS.length;
            saveVideoFilter();
        });
        videoNumberButton.setOnLongClickListener(v -> {
            videoNumberIndex = 0;
            saveVideoFilter();
            toast("Alle nummers tonen");
            return true;
        });
        practiceToggle.setOnClickListener(v -> {
            if (practiceMode) {
                stopPracticeCapture();
                toast("Opnemen gestopt; gewoon spelen staat weer aan");
                return;
            }
            if (!SwipeReplayAccessibilityService.isAvailable()) {
                toast("Schakel eerst Darts Pen Overlay - Practice Replay in bij Toegankelijkheid. Android vraagt jouw toestemming.");
                Intent settings = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(settings);
                return;
            }
            startPracticeCapture();
        });
        saveThrow.setOnClickListener(v -> saveLastPracticeThrow());
        replayTargetButton.setOnClickListener(v -> {
            replayTargetIndex = (replayTargetIndex + 1) % REPLAY_TARGETS.length;
            ui.edit().putInt("replay_target_index", replayTargetIndex).apply();
            updateToolbar();
        });
        replayTargetButton.setOnLongClickListener(v -> {
            replayTargetIndex = 0;
            ui.edit().putInt("replay_target_index", 0).apply();
            updateToolbar();
            toast("Doel teruggezet naar T20");
            return true;
        });
        saveReplayButton.setOnClickListener(v -> saveReplayPreset());
        playReplayButton.setOnClickListener(v -> playReplayPreset());
        remoteLauncherButton.setOnClickListener(v -> {
            if (remoteOpen) closeReplayRemote();
            else openReplayRemote();
        });
        practiceLengthButton.setOnClickListener(v -> {
            practiceLengthIndex = (practiceLengthIndex + 1) % PRACTICE_LENGTHS.length;
            ui.edit().putInt("practice_length_index", practiceLengthIndex).apply();
            refreshRecordedPreview();
            updateToolbar();
        });
        practiceWidthButton.setOnClickListener(v -> {
            practiceWidthIndex = (practiceWidthIndex + 1) % PRACTICE_WIDTHS.length;
            ui.edit().putInt("practice_width_index", practiceWidthIndex).apply();
            refreshRecordedPreview();
            updateToolbar();
        });
        skipThrow.setOnClickListener(v -> {
            lastPracticeSwipe = null;
            canvas.clearRecordedGuidePreview();
            updateToolbar();
            toast("Laatste worp overgeslagen");
        });
        calibrate.setOnClickListener(v -> {
            if (!editing || !StrokeStore.isProTarget(canvas.target())) return;
            proFineOpen = false;
            erasing = false;
            canvas.toggleProCalibration();
            palette.setVisibility(View.GONE);
            versionPanel.setVisibility(View.GONE);
            updateToolbar();
        });
        glowButton.setOnClickListener(v -> {
            glowLevel = (glowLevel + 1) % 4;
            ui.edit().putInt("pro_glow_level", glowLevel).apply();
            canvas.setProGlowLevel(glowLevel);
            updateToolbar();
        });
        lineThicknessButton.setOnClickListener(v -> {
            proLineThickness = (proLineThickness + 1) % PRO_WIDTH_LABELS.length;
            // Video comparisons have independent width preferences. Switching
            // between them never changes the original user's PRO thickness.
            ui.edit().putInt(proThicknessKey(canvas.target()), proLineThickness).apply();
            canvas.setProLineThickness(proLineThickness);
            updateToolbar();
        });
        proFineToggle.setOnClickListener(v -> {
            if (!editing || !StrokeStore.isProTarget(canvas.target())) return;
            boolean opening = !proFineOpen;
            if (opening) {
                canvas.cancelCalibration();
                canvas.setMoveMode(false);
                erasing = false;
                palette.setVisibility(View.GONE);
                versionPanel.setVisibility(View.GONE);
                if (!store.saveVersion(canvas.layerProfile(),"Back-up vóór PRO fijn")) {
                    toast("Kon back-up niet bewaren; probeer opnieuw");
                    return;
                }
            }
            proFineOpen = opening;
            updateToolbar();
        });
        move.setOnClickListener(v -> {
            proFineOpen = false;
            canvas.cancelCalibration();
            toggleMoveMode();
        });
        close.setOnClickListener(v -> stopSelf());
        undo.setOnClickListener(v -> { if (editing) canvas.undo(); });
        eraser.setOnClickListener(v -> {
            proFineOpen = false;
            canvas.cancelCalibration();
            if (canvas.isMoveMode()) canvas.setMoveMode(false);
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
            proFineOpen = false;
            canvas.cancelCalibration();
            if (canvas.isMoveMode()) canvas.setMoveMode(false);
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
        attachDrag(dragHandle);
        canvas.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, orr, ob) -> {
            if (toolbarAdded) toolbar.post(this::restorePosition);
            if (targetBarAdded) targetBar.post(() -> { resizeTargetList(); refreshTargetBar(); });
            if (remoteOpen) remotePanel.post(() -> { resizeReplayRemote(); refreshReplayRemote(); });
            if (throwEditor != null && !editedThrowProfile.equals(canvas.profile())) {
                toolbar.post(() -> {
                    if (throwEditor != null && !editedThrowProfile.equals(canvas.profile())) {
                        finishThrowEdit(false);
                        toast("Schermstand veranderd; edit geannuleerd");
                    }
                });
            }
        });
        toolbar.post(this::restorePosition);
        showTargetBar();
        // Keep live game touches unobstructed unless capture is explicitly on.
        setEditing(false);
        if (targetBar != null) targetBar.setVisibility(View.GONE);
        updateToolbar();
        setMenuMinimized(menuMinimized);
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
            // The target strip is intentionally narrow on the Magic V5.
            String shortName = "PRO Swipe".equals(target) ? "Swipe" :
                    "PRO Dart".equals(target) ? "Dart" : target;
            TextView button = chip(shortName, 0xFF334155);
            button.setContentDescription(target);
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
                refreshThicknessForTarget();
                lastPracticeSwipe = null;
                canvas.clearRecordedGuidePreview();
                proFineOpen = false;
                if (versionPanel != null) versionPanel.setVisibility(View.GONE);
                if (palette != null) palette.setVisibility(View.GONE);
                refreshTargetBar();
                updateToolbar();
                if ("PRO Swipe".equals(target)) {
                    toast("Swipe: alle 20 triples, 20 doubles en Bull. Wissel T / D en kies nummer.");
                } else if ("PRO Dart".equals(target)) {
                    toast("Dart: alle 20 triples, 20 doubles en Bull. Wissel T / D en kies nummer.");
                }
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

    private String proThicknessKey(String target) {
        return StrokeStore.isVideoVariantTarget(target) ?
                "pro_line_thickness_" + target.replace(" ", "_") : "pro_line_thickness";
    }

    private void refreshThicknessForTarget() {
        if (canvas == null) return;
        String target = canvas.target();
        if (target.equals(lastThicknessTarget)) return;
        lastThicknessTarget = target;
        int defaultWidth = StrokeStore.isVideoVariantTarget(target) ? 2 :
                ui.getInt("pro_line_thickness", 5);
        proLineThickness = Math.max(0, Math.min(PRO_WIDTH_LABELS.length - 1,
                ui.getInt(proThicknessKey(target), defaultWidth)));
        canvas.setProLineThickness(proLineThickness);
    }

    private String videoFilterKey(String target, String suffix) {
        return "video_" + suffix + "_" + target.replace(" ", "_");
    }

    private void refreshVideoFilterForTarget() {
        if (canvas == null) return;
        String target = canvas.target();
        if (lastVideoFilterTarget.equals(target)) return;
        lastVideoFilterTarget = target;
        if (StrokeStore.isVideoVariantTarget(target)) {
            videoRingIndex = Math.max(0, Math.min(2,
                    ui.getInt(videoFilterKey(target,"ring"),0)));
            videoNumberIndex = Math.max(0, Math.min(VIDEO_NUMBERS.length-1,
                    ui.getInt(videoFilterKey(target,"number"),0)));
        }
        canvas.setVideoComparisonFilter(VIDEO_RINGS[videoRingIndex],
                VIDEO_NUMBERS[videoNumberIndex]);
    }

    private void saveVideoFilter() {
        if (canvas == null || !StrokeStore.isVideoVariantTarget(canvas.target())) return;
        ui.edit().putInt(videoFilterKey(canvas.target(),"ring"),videoRingIndex)
                .putInt(videoFilterKey(canvas.target(),"number"),videoNumberIndex).apply();
        canvas.setVideoComparisonFilter(VIDEO_RINGS[videoRingIndex],
                VIDEO_NUMBERS[videoNumberIndex]);
        updateToolbar();
    }

    private void refreshTargetBar() {
        if (targetBar == null || canvas == null) return;
        refreshThicknessForTarget();
        refreshVideoFilterForTarget();
        String current = canvas.target();
        targetToggle.setText(targetCollapsed ?
                ("PRO Swipe".equals(current) ? "Swipe ◀" :
                 "PRO Dart".equals(current) ? "Dart ◀" : current + " ◀") : "≡");
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


    private void adjustAllPro(float dxDp, float dyDp, float scale, float rotateDegrees) {
        if (!editing || !proFineOpen || !StrokeStore.isProTarget(canvas.target())) return;
        if (!canvas.fineAdjustPro(dxDp,dyDp,scale,rotateDegrees)) {
            toast("Aanpassing niet mogelijk aan de schermrand");
        }
    }

    private void toggleMoveMode() {
        if (!editing) return;
        boolean enabling = !canvas.isMoveMode();
        canvas.setMoveMode(enabling);
        if (enabling) {
            erasing = false;
            palette.setVisibility(View.GONE);
            versionPanel.setVisibility(View.GONE);
        }
        updateToolbar();
    }

    private void nudgeLine(float dxDp, float dyDp) {
        if (!editing || !canvas.isMoveMode()) return;
        if (!canvas.nudgeSelected(dxDp, dyDp)) {
            toast("Tik eerst op een getekende lijn (of hij zit al aan de schermrand)");
        }
    }


    /**
     * This experiment is opt-in: a touchable overlay captures a single real
     * gesture, becomes non-touchable, and the accessibility service plays the
     * captured path into the game. The user judges whether the game accepted it
     * and explicitly chooses to turn that path into a saved line.
     */
    private void updatePracticeTouchWindow(boolean captureTouches) {
        if (!canvasAdded || canvasParams == null) return;
        canvas.setPracticeCaptureEnabled(captureTouches);
        canvasParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                (captureTouches ? 0 : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        canvasParams.alpha = captureTouches ? 1f : PLAY_ALPHA;
        windows.updateViewLayout(canvas, canvasParams);
    }

    /**
     * Magnify only the dartboard using Android's native window magnifier.
     * The board centre is taken from the saved PRO calibration, even when the
     * currently selected target layer is not PRO.
     */
    /** Collapse both floating menus into a single, 43dp reopen bubble. */
    private void setMenuMinimized(boolean minimized) {
        menuMinimized = minimized;
        ui.edit().putBoolean("toolbar_minimized", minimized).apply();
        if (toolbarExpandedContent == null || toolbarTop == null) return;
        for (View control : new View[]{
                dragHandle, close, minimizeMenuButton}) {
            if (control != null) control.setVisibility(minimized ? View.GONE : View.VISIBLE);
        }
        if (compactBubble != null) compactBubble.setVisibility(
                minimized ? View.VISIBLE : View.GONE);
        toolbarExpandedContent.setVisibility(minimized ? View.GONE : View.VISIBLE);
        toolbar.setPadding(dp(minimized ? 1 : 5), dp(minimized ? 1 : 5),
                dp(minimized ? 1 : 5), dp(minimized ? 1 : 5));
        if (targetBarAdded && targetBar != null)
            targetBar.setVisibility(View.GONE);
        // Toolbar window uses WRAP_CONTENT: it shrinks with the content.
        if (toolbarAdded) {
            try { windows.updateViewLayout(toolbar, toolbarParams); }
            catch (RuntimeException ignored) { }
        }
        if (!minimized) updateToolbar();
    }

    private void startPracticeCapture() {
        if (practiceMode || throwEditor != null || !SwipeReplayAccessibilityService.isAvailable()) return;
        if (editing) setEditing(false);
        canvas.setMoveMode(false);
        practiceMode = true;
        replayPending = false;
        lastPracticeSwipe = null;
        canvas.clearRecordedGuidePreview();
        updatePracticeTouchWindow(true);
        updateToolbar();
        toast("Oefenworp aan: swipe op het bord. De app speelt hem daarna na; tik ★ Bewaar bij een goede worp.");
    }

    private void stopPracticeCapture() {
        practiceMode = false;
        replayPending = false;
        ++replaySequence;
        updatePracticeTouchWindow(false);
        updateToolbar();
    }

    private void onPracticeSwipe(ArrayList<PointF> points, ArrayList<Long> times, long elapsedMs) {
        if (!practiceMode || replayPending || points == null || points.size() < 2
                || times == null || times.size() != points.size()) return;
        lastPracticeSwipe = points;
        lastPracticePointTimesMs = times;
        lastPracticeDurationMs = elapsedMs;
        lastPracticeScreenProfile = canvas.profile();
        lastPracticeTarget = canvas.target();
        refreshRecordedPreview();
        replayPending = true;
        final int sequence = ++replaySequence;
        updatePracticeTouchWindow(false);
        setReplayRemoteHidden(true);
        updateToolbar();
        final float w = canvas.getWidth(), h = canvas.getHeight();
        final int[] origin = new int[2];
        canvas.getLocationOnScreen(origin);
        // Wait for the non-touchable overlay flag to reach the window manager
        // before Android injects the path into the underlying game.
        canvas.postDelayed(() -> {
            if (!practiceMode || !replayPending || sequence != replaySequence) return;
            boolean accepted = SwipeReplayAccessibilityService.replay(
                    points, times, w, h, origin[0], origin[1], elapsedMs,
                    0f,
                    success -> onReplayFinished(success, sequence));
            if (!accepted && replayPending) onReplayFinished(false, sequence);
        }, 95L);
    }

    private void onReplayFinished(boolean success, int sequence) {
        if (!replayPending || sequence != replaySequence) return;
        replayPending = false;
        if (!success) {
            toast("Android kon de worp niet naspelen. Schakel oefenmodus uit voor gewone bediening.");
        }
        updateToolbar();
        // Allow the game's gesture release/result to settle before re-arming.
        canvas.postDelayed(() -> {
            if (practiceMode && !replayPending &&
                    SwipeReplayAccessibilityService.isAvailable()) {
                updatePracticeTouchWindow(true);
            } else if (practiceMode) {
                stopPracticeCapture();
            }
            if (!replayPending) setReplayRemoteHidden(false);
        }, 300L);
    }

    /** Saves the last captured finger path to a named target slot. */
    private void saveReplayPreset() {
        saveReplayPresetFor(REPLAY_TARGETS[replayTargetIndex]);
    }

    private boolean saveReplayPresetFor(String target) {
        if (throwEditor != null) {
            toast("Sluit eerst de swipe-editor");
            return false;
        }
        if (replayPending) {
            toast("Wacht tot de worp is afgerond");
            return false;
        }
        if (lastPracticeSwipe == null || lastPracticeSwipe.size() < 2
                || !canvas.profile().equals(lastPracticeScreenProfile)) {
            toast("Vang eerst een worp op dit scherm met ◎ Vang worp");
            return false;
        }
        boolean saved = recordedThrows.save(canvas.profile(), target, lastPracticeSwipe,
                lastPracticeDurationMs, 0f, lastPracticePointTimesMs);
        if (saved) {
            toast("Worp bewaard voor " + target + ". Tik ▶ " + target + " om te herhalen.");
        } else {
            toast("Bewaren mislukt; probeer opnieuw");
        }
        updateToolbar();
        refreshReplayRemote();
        return saved;
    }

    /** Tap-to-replay from the compact toolbar. */
    private void playReplayPreset() {
        playReplayTarget(REPLAY_TARGETS[replayTargetIndex]);
    }

    /** Tap-to-replay from either the compact toolbar or the 41-target remote. */
    private void playReplayTarget(String target) {
        if (throwEditor != null) {
            toast("Sluit eerst de swipe-editor");
            return;
        }
        if (replayPending) {
            toast("Er wordt al een worp afgespeeld");
            return;
        }
        RecordedThrowStore.SavedThrow saved = recordedThrows.load(canvas.profile(), target);
        if (saved == null) {
            toast("Nog geen worp voor " + target + ". Eerst opnemen en bewaren.");
            return;
        }
        if (!SwipeReplayAccessibilityService.isAvailable()) {
            toast("Schakel Darts Pen Snap (Test) - Practice Replay in bij Toegankelijkheid");
            Intent settings = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(settings);
            return;
        }
        if (editing) setEditing(false);
        replayPending = true;
        final int sequence = ++replaySequence;
        updatePracticeTouchWindow(false);
        setReplayRemoteHidden(true);
        updateToolbar();
        final float w = canvas.getWidth(), h = canvas.getHeight();
        final String screen = canvas.profile();
        final int[] origin = new int[2];
        canvas.getLocationOnScreen(origin);
        // A recorded 500 ms throw is 500 ms at 100%, 1000 ms at 50%.
        // Android limits each gesture to GestureDescription.getMaxGestureDuration().
        final long adjustedDuration = Math.round(saved.durationMs * 100.0 / replaySpeedPercent);
        // Allow the finger-up event and overlay window changes to settle.
        canvas.postDelayed(() -> {
            if (!replayPending || sequence != replaySequence) return;
            if (!screen.equals(canvas.profile()) || canvas.getWidth() != (int)w ||
                    canvas.getHeight() != (int)h) {
                onReplayFinished(false, sequence);
                toast("Scherm gewijzigd; kies dezelfde schermstand als bij opnemen");
                return;
            }
            boolean accepted = SwipeReplayAccessibilityService.replay(
                    saved.points, saved.pointTimesMs, w, h, origin[0], origin[1],
                    adjustedDuration, 0f,
                    success -> onReplayFinished(success, sequence));
            if (!accepted && replayPending) onReplayFinished(false, sequence);
        }, 140L);
    }

    /** Editing shows raw recorded points; no angle snapping is applied. */
    private ArrayList<PointF> visibleReplayPath(RecordedThrowStore.SavedThrow saved) {
        ArrayList<PointF> result = new ArrayList<>();
        if (saved == null) return result;
        for (PointF point : saved.points) result.add(new PointF(point.x, point.y));
        return result;
    }

    /** The path-editor overlay intentionally intercepts touches, preventing accidental throws. */
    private void startThrowEdit(String target) {
        if (throwEditor != null || replayPending || !remoteOpen || canvas == null) return;
        RecordedThrowStore.SavedThrow saved = recordedThrows.load(canvas.profile(), target);
        if (saved == null) {
            toast("Nog geen opgeslagen worp voor " + target);
            return;
        }
        if (practiceMode) stopPracticeCapture();
        if (editing) setEditing(false);
        editedThrowProfile = canvas.profile();
        editedThrowTarget = target;
        editedThrowDurationMs = saved.durationMs;
        editedThrowOriginalDurationMs = saved.durationMs;
        editedThrowTimingSourceDurationMs = saved.durationMs;
        editedThrowOriginalTimingDurationMs = saved.durationMs;
        editedThrowTimesMs = saved.pointTimesMs == null ? null :
                new ArrayList<>(saved.pointTimesMs);
        editedThrowOriginalTimesMs = saved.pointTimesMs == null ? null :
                new ArrayList<>(saved.pointTimesMs);
        editedThrowStepIndex = 1; // 1dp like the PRO fine editor
        editedDurationStepIndex = 1; // 100ms by default; tap to select 10/100/1000
        throwEditor = new ThrowPathEditorView(this, visibleReplayPath(saved), () -> { });
        throwEditorParams = new WindowManager.LayoutParams(-1, -1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        throwEditorParams.gravity = Gravity.TOP | Gravity.LEFT;
        try {
            windows.addView(throwEditor, throwEditorParams);
            // Order editor below the floating remote: the path consumes game
            // touches, while the remote's Save / Cancel controls remain usable.
            windows.removeViewImmediate(remotePanel);
            windows.addView(remotePanel, remoteParams);
        } catch (RuntimeException e) {
            if (throwEditor != null) {
                try { windows.removeViewImmediate(throwEditor); }
                catch (RuntimeException ignored) { }
            }
            throwEditor = null;
            throwEditorParams = null;
            editedThrowProfile = "";
            editedThrowTarget = "";
            toast("Swipe-editor kan niet openen");
            return;
        }
        remotePanel.showEditor(target, throwEditor.getMode(),
                NUDGE_STEPS[editedThrowStepIndex], editedThrowDurationMs);
        remotePanel.setDurationStep(EDIT_DURATION_STEPS_MS[editedDurationStepIndex]);
        toast("✎ " + target + ": versleep de gele lijn of gebruik PRO fijn. Save of Cancel.");
    }

    /** All geometry edits remain local until Save. */
    private void handleThrowEditAction(String action) {
        if (throwEditor == null || action == null) return;
        float step = NUDGE_STEPS[editedThrowStepIndex];
        boolean transformed = true;
        switch (action) {
            case "left":
                transformed = throwEditor.fineAdjust(-step,0f,1f,0f);
                break;
            case "right":
                transformed = throwEditor.fineAdjust(step,0f,1f,0f);
                break;
            case "up":
                transformed = throwEditor.fineAdjust(0f,-step,1f,0f);
                break;
            case "down":
                transformed = throwEditor.fineAdjust(0f,step,1f,0f);
                break;
            case "rotateLeft":
                transformed = throwEditor.fineAdjust(0f,0f,1f,-0.25f);
                break;
            case "rotateRight":
                transformed = throwEditor.fineAdjust(0f,0f,1f,0.25f);
                break;
            case "shorter":
                transformed = throwEditor.fineAdjust(0f,0f,0.995f,0f);
                break;
            case "longer":
                transformed = throwEditor.fineAdjust(0f,0f,1.005f,0f);
                break;
            case "mode":
                throwEditor.setMode((throwEditor.getMode()+1)%3);
                break;
            case "step":
                editedThrowStepIndex = (editedThrowStepIndex+1)%NUDGE_STEPS.length;
                break;
            case "undo":
                transformed = throwEditor.undo();
                break;
            case "reset":
                transformed = throwEditor.reset();
                editedThrowDurationMs = editedThrowOriginalDurationMs;
                editedThrowTimingSourceDurationMs = editedThrowOriginalTimingDurationMs;
                editedThrowTimesMs = editedThrowOriginalTimesMs == null ? null :
                        new ArrayList<>(editedThrowOriginalTimesMs);
                break;
            case "durationMinus":
                editedThrowDurationMs = Math.max(25L,
                        editedThrowDurationMs-EDIT_DURATION_STEPS_MS[editedDurationStepIndex]);
                break;
            case "durationPlus":
                editedThrowDurationMs = Math.min(30000L,
                        editedThrowDurationMs+EDIT_DURATION_STEPS_MS[editedDurationStepIndex]);
                break;
            case "durationStep":
                editedDurationStepIndex =
                        (editedDurationStepIndex + 1) % EDIT_DURATION_STEPS_MS.length;
                remotePanel.setDurationStep(EDIT_DURATION_STEPS_MS[editedDurationStepIndex]);
                break;
            case "test":
                testEditedThrow();
                return;
            case "previous":
                RecordedThrowStore.SavedThrow previous = recordedThrows.loadPrevious(
                        editedThrowProfile, editedThrowTarget);
                if (previous != null) {
                    throwEditor.replaceWith(visibleReplayPath(previous));
                    editedThrowDurationMs = previous.durationMs;
                    editedThrowTimingSourceDurationMs = previous.durationMs;
                    editedThrowTimesMs = previous.pointTimesMs == null ? null :
                            new ArrayList<>(previous.pointTimesMs);
                    toast("Vorige versie geladen. Tik Save om te bewaren.");
                } else {
                    toast("Nog geen vorige opgeslagen versie voor dit doel");
                }
                break;
            case "save":
                finishThrowEdit(true);
                return;
            case "cancel":
                finishThrowEdit(false);
                return;
            default:
                return;
        }
        if (!transformed && ("left".equals(action) || "right".equals(action)
                || "up".equals(action) || "down".equals(action)
                || "rotateLeft".equals(action) || "rotateRight".equals(action)
                || "shorter".equals(action) || "longer".equals(action))) {
            toast("Aanpassing raakt de schermrand");
        }
        if (remotePanel != null) remotePanel.updateEditorStatus(
                throwEditor.getMode(), NUDGE_STEPS[editedThrowStepIndex],
                editedThrowDurationMs);
    }

    /**
     * Preserve the recorded slow/fast movement pattern while changing the
     * overall throw duration. Used identically by TEST and SAVE.
     */
    private ArrayList<Long> currentEditedPointTimes(int pointCount) {
        if (editedThrowTimesMs == null || editedThrowTimesMs.size() != pointCount)
            return null; // legacy recordings remain playable without timestamps
        ArrayList<Long> adjusted = new ArrayList<>();
        long last = 0L;
        for (long t : editedThrowTimesMs) {
            long scaled = Math.round(t * (double)editedThrowDurationMs /
                    Math.max(1L, editedThrowTimingSourceDurationMs));
            last = Math.max(last, Math.min(editedThrowDurationMs, scaled));
            adjusted.add(last);
        }
        return adjusted;
    }

    /**
     * Send this one unsaved edited throw as a manual test. It is NEVER stored.
     * Temporarily release both overlay windows so Darts Club receives input.
     * Restore the editor afterwards, whether dispatch succeeded or failed.
     */
    private void testEditedThrow() {
        if (throwEditor == null || replayPending || canvas == null) return;
        if (!editedThrowProfile.equals(canvas.profile())) {
            toast("Schermstand gewijzigd; test geannuleerd");
            return;
        }
        if (!SwipeReplayAccessibilityService.isAvailable()) {
            toast("Schakel de Practice Replay toegankelijkheidsservice in voor Test");
            return;
        }
        final ArrayList<PointF> draftPoints = throwEditor.getPoints();
        if (draftPoints.size() < 2) return;
        final ArrayList<Long> draftTimes = currentEditedPointTimes(draftPoints.size());
        // Test exactly as a future ▶ target replay would: apply the currently
        // chosen remote speed percentage to this unsaved base duration.
        final long draftDuration = Math.round(
                editedThrowDurationMs * 100.0 / replaySpeedPercent);
        final float width = canvas.getWidth(), height = canvas.getHeight();
        if (width <= 0 || height <= 0) {
            toast("Nog geen bruikbaar speelveld");
            return;
        }
        final int[] origin = new int[2];
        canvas.getLocationOnScreen(origin);
        replayPending = true;
        final int sequence = ++replaySequence;
        updatePracticeTouchWindow(false);
        setEditorTouchEnabled(false);
        setReplayRemoteHidden(true);
        // Always restore editor controls if Android never reports completion.
        canvas.postDelayed(() -> {
            if (replayPending && sequence == replaySequence && throwEditor != null)
                onEditedTestFinished(false, sequence);
        }, Math.min(65000L, draftDuration + 6000L));
        // Wait for the windows to become fully non-touchable before injection.
        canvas.postDelayed(() -> {
            if (!replayPending || sequence != replaySequence || throwEditor == null)
                return;
            if (!editedThrowProfile.equals(canvas.profile())) {
                onEditedTestFinished(false, sequence);
                return;
            }
            boolean dispatched = SwipeReplayAccessibilityService.replay(
                    draftPoints, draftTimes, width, height, origin[0], origin[1],
                    draftDuration, 0f, result -> onEditedTestFinished(result, sequence));
            if (!dispatched) onEditedTestFinished(false, sequence);
        }, 175L);
    }

    private void setEditorTouchEnabled(boolean enabled) {
        if (throwEditor == null || throwEditorParams == null) return;
        throwEditorParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                (enabled ? 0 : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        throwEditorParams.alpha = enabled ? 1f : 0f;
        try { windows.updateViewLayout(throwEditor, throwEditorParams); }
        catch (RuntimeException ignored) { }
    }

    private void onEditedTestFinished(boolean success, int sequence) {
        if (!replayPending || sequence != replaySequence) return;
        replayPending = false;
        // Let the game finish processing the synthetic finger-up first.
        canvas.postDelayed(() -> {
            if (throwEditor != null && remoteOpen) {
                setEditorTouchEnabled(true);
                setReplayRemoteHidden(false);
            }
        }, 300L);
        toast(success
                ? "Test afgespeeld. Edit is NIET opgeslagen; klik ✓ SAVE om te bewaren."
                : "Android kon de testworp niet afspelen. Edit blijft onopgeslagen.");
    }

    private void finishThrowEdit(boolean save) {
        if (throwEditor == null) return;
        boolean saved = false;
        String target = editedThrowTarget;
        if (save && canvas != null && editedThrowProfile.equals(canvas.profile())) {
            // TEST and SAVE share exactly the same path/timing calculation.
            ArrayList<PointF> editedPoints = throwEditor.getPoints();
            ArrayList<Long> adjustedTimes = currentEditedPointTimes(editedPoints.size());
            // Store the displayed effective path without applying snapping again.
            saved = recordedThrows.saveEdited(editedThrowProfile, editedThrowTarget,
                    editedPoints, editedThrowDurationMs, 0f, adjustedTimes);
        }
        if (save && !saved) {
            toast("Bewaren niet gelukt. De edit blijft open; probeer opnieuw of annuleer.");
            return;
        }
        try { windows.removeViewImmediate(throwEditor); }
        catch (RuntimeException ignored) { }
        throwEditor = null;
        throwEditorParams = null;
        editedThrowTarget = "";
        editedThrowProfile = "";
        editedThrowTimesMs = null;
        editedThrowOriginalTimesMs = null;
        if (remotePanel != null) {
            remotePanel.hideEditor();
            refreshReplayRemote();
        }
        refreshQuickPlayButtons();
        if (save) toast(saved ? "✓ " + target + " apart opgeslagen (vorige versie bewaard)" :
                "Worp niet opgeslagen. Schermstand of opslag veranderd.");
    }

    /** Opens a distinct floating remote; it can be moved or closed. */
    private void openReplayRemote() {
        if (remoteOpen || canvas == null) return;
        remotePanel = new ThrowReplayRemote(this, new ThrowReplayRemote.Listener() {
            @Override public void onTargetPressed(String target, boolean saveMode) {
                if (saveMode) {
                    if (saveReplayPresetFor(target)) remotePanel.setSaveMode(false);
                } else {
                    playReplayTarget(target);
                }
            }
            @Override public void onSpeedStep(int delta) {
                changeReplaySpeedPercent(replaySpeedPercent + delta);
            }
            @Override public void onSpeedPercentChanged(int percent) {
                changeReplaySpeedPercent(percent);
            }
            @Override public void onRingSelected(int ringIndex) {
                replayRingIndex = ringIndex;
                ui.edit().putInt("replay_ring_index", ringIndex).apply();
            }
            @Override public void onEditTarget(String target) {
                startThrowEdit(target);
            }
            @Override public void onEditAction(String action) {
                handleThrowEditAction(action);
            }
            @Override public void onClose() {
                if (throwEditor != null) {
                    toast("Eerst ✓ SAVE of × CANCEL. Sluiten bewaart geen wijzigingen.");
                } else closeReplayRemote();
            }
            @Override public void onDrag(int dx, int dy) { moveReplayRemote(dx, dy); }
        });
        remotePanel.setSelectedRing(replayRingIndex);

        remoteParams = new WindowManager.LayoutParams(
                dp(274), dp(445),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        remoteParams.gravity = Gravity.TOP | Gravity.LEFT;
        remoteParams.x = Math.max(0, canvas.getWidth() - dp(284));
        remoteParams.y = dp(100);
        remoteOpen = true;
        resizeReplayRemote();
        try {
            windows.addView(remotePanel, remoteParams);
        } catch (RuntimeException e) {
            remoteOpen = false;
            remotePanel = null;
            remoteParams = null;
            toast("Kan de replay remote niet openen");
            return;
        }
        refreshReplayRemote();
        updateToolbar();
    }

    private void closeReplayRemote() {
        finishThrowEdit(false);
        if (remotePanel != null) {
            try { windows.removeView(remotePanel); } catch (RuntimeException ignored) { }
        }
        remoteOpen = false;
        remotePanel = null;
        remoteParams = null;
        if (toolbarAdded) updateToolbar();
    }

    private void resizeReplayRemote() {
        if (!remoteOpen || remotePanel == null || remoteParams == null || canvas == null) return;
        int w = canvas.getWidth() > 0 ? canvas.getWidth() : getResources().getDisplayMetrics().widthPixels;
        int h = canvas.getHeight() > 0 ? canvas.getHeight() : getResources().getDisplayMetrics().heightPixels;
        remoteParams.width = Math.min(dp(274), Math.max(dp(190), w - dp(8)));
        remoteParams.height = Math.min(dp(445), Math.max(dp(235), h - dp(145)));
        remoteParams.x = Math.max(0, Math.min(remoteParams.x, w - remoteParams.width));
        remoteParams.y = Math.max(0, Math.min(remoteParams.y, h - remoteParams.height));
        try { windows.updateViewLayout(remotePanel, remoteParams); }
        catch (IllegalArgumentException ignored) { /* not attached until addView */ }
    }

    private void moveReplayRemote(int dx, int dy) {
        if (!remoteOpen || remoteParams == null || canvas == null) return;
        remoteParams.x += dx;
        remoteParams.y += dy;
        resizeReplayRemote();
    }

    /** Prevent the remote from intercepting the synthetic swipe it launched. */
    private void setReplayRemoteHidden(boolean hidden) {
        if (!remoteOpen || remotePanel == null || remoteParams == null) return;
        remoteParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                (hidden ? WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE : 0);
        remoteParams.alpha = hidden ? 0f : 1f;
        try { windows.updateViewLayout(remotePanel, remoteParams); }
        catch (RuntimeException ignored) { }
    }

    private void changeReplaySpeedPercent(int percent) {
        int next = Math.max(1, Math.min(100, percent));
        if (replaySpeedPercent != next) {
            replaySpeedPercent = next;
            ui.edit().putInt("replay_speed_percent", next).apply();
        }
        if (remoteOpen && remotePanel != null) remotePanel.setSpeedPercent(next);
    }

    private void refreshReplayRemote() {
        if (!remoteOpen || remotePanel == null || canvas == null) return;
        remotePanel.refresh(recordedThrows, canvas.profile(), replaySpeedPercent);
    }

    private void refreshRecordedPreview() {
        if (canvas == null) return;
        if (lastPracticeSwipe == null ||
                !canvas.profile().equals(lastPracticeScreenProfile) ||
                !canvas.target().equals(lastPracticeTarget)) {
            canvas.clearRecordedGuidePreview();
            return;
        }
        canvas.setRecordedGuidePreview(lastPracticeSwipe,
                PRACTICE_LENGTHS[practiceLengthIndex],
                PRACTICE_WIDTHS[practiceWidthIndex]);
    }

    private void saveLastPracticeThrow() {
        if (replayPending) {
            toast("Wacht tot de oefenworp klaar is");
            return;
        }
        if (lastPracticeSwipe == null || lastPracticeSwipe.size() < 2) {
            toast("Nog geen worp opgenomen");
            return;
        }
        if (!canvas.profile().equals(lastPracticeScreenProfile) ||
                !canvas.target().equals(lastPracticeTarget)) {
            toast("Deze worp hoort bij een andere doel-laag of schermstand");
            return;
        }
        if (!store.saveVersion(canvas.layerProfile(), "Voor opslaan beste oefenworp")) {
            toast("Kon herstelkopie niet bewaren");
            return;
        }
        if (!canvas.saveRecordedThrow(lastPracticeSwipe,
                PRACTICE_LENGTHS[practiceLengthIndex],
                PRACTICE_WIDTHS[practiceWidthIndex])) {
            toast("Worp bewaren mislukt");
            return;
        }
        lastPracticeSwipe = null;
        canvas.clearRecordedGuidePreview();
        updateToolbar();
        toast("★ Goede worp bewaard als langere richtlijn (" +
                (int)PRACTICE_LENGTHS[practiceLengthIndex] + "×) onder " + canvas.target());
    }

    private void setEditing(boolean edit) {
        if (practiceMode) stopPracticeCapture();
        if (edit && remoteOpen) closeReplayRemote();
        editing = edit;
        canvas.setEditable(edit);
        canvasParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | (edit ? 0 : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        canvasParams.alpha = edit ? 1f : PLAY_ALPHA;
        windows.updateViewLayout(canvas, canvasParams);
        if (!edit) {
            proFineOpen = false;
            palette.setVisibility(View.GONE);
            versionPanel.setVisibility(View.GONE);
        }
        updateToolbar();
    }

    /** Green = saved swipe; gray = empty slot. The quick buttons never record. */
    private void refreshQuickPlayButtons() {
        if (canvas == null || recordedThrows == null) return;
        for (int i = 0; i < quickReplayButtons.size(); i++) {
            String target = QUICK_TARGETS[i];
            TextView button = quickReplayButtons.get(i);
            boolean recorded = recordedThrows.load(canvas.profile(), target) != null;
            button.setText(recorded ? "▶ " + target : target);
            button.setTextColor(recorded ? 0xFFBBF7D0 : 0xFFE2E8F0);
            button.setContentDescription(recorded
                    ? "Play saved " + target + " swipe, long press to edit"
                    : "No saved swipe for " + target);
            if (button.getBackground() instanceof GradientDrawable) {
                ((GradientDrawable)button.getBackground()).setColor(
                        recorded ? 0xFF14532D : 0xFF334155);
            }
            button.setEnabled(!replayPending);
            button.setAlpha(replayPending ? 0.55f : 1f);
        }
    }

    private void updateToolbar() {
        if (mode == null) return;
        mode.setText(editing ? "▶ Spelen" : "✎ Tekenen");
        if (videoFilterRow != null) {
            boolean video = StrokeStore.isVideoVariantTarget(canvas.target());
            videoFilterRow.setVisibility(video && !practiceMode ? View.VISIBLE : View.GONE);
            videoRingButton.setText(videoRingIndex == 0 ? "T ×20" :
                    videoRingIndex == 1 ? "D ×20" : "T+D ×40");
            int n = VIDEO_NUMBERS[videoNumberIndex];
            videoNumberButton.setText(n == 0 ? "Alle nummers" :
                    n == 25 ? "Bull" : "Nr. " + n);
        }
        if (practiceRow != null) {
            practiceRow.setVisibility(!editing || practiceMode ? View.VISIBLE : View.GONE);
            practiceSettingsRow.setVisibility(!editing || practiceMode ? View.VISIBLE : View.GONE);
            replayRow.setVisibility(!editing || practiceMode ? View.VISIBLE : View.GONE);
            remoteLauncherRow.setVisibility(!editing || practiceMode ? View.VISIBLE : View.GONE);
            remoteLauncherButton.setText(remoteOpen ? "🎛 Close 61-target remote" : "🎛 Open 61-target remote");
            String replayTarget = REPLAY_TARGETS[replayTargetIndex];
            replayTargetButton.setText("Doel " + replayTarget);
            saveReplayButton.setText("💾 Bewaar " + replayTarget);
            saveReplayButton.setVisibility(lastPracticeSwipe != null && !replayPending
                    && canvas.profile().equals(lastPracticeScreenProfile)
                    ? View.VISIBLE : View.GONE);
            playReplayButton.setText("▶ " + replayTarget);
            playReplayButton.setTextColor(recordedThrows.load(canvas.profile(), replayTarget) != null
                    ? 0xFFA7F3D0 : 0xFF94A3B8);
            practiceLengthButton.setText("↗ Lengte ×" +
                    (int)PRACTICE_LENGTHS[practiceLengthIndex]);
            practiceWidthButton.setText("Lijndikte " +
                    (PRACTICE_WIDTHS[practiceWidthIndex] == 1.5f ? "1,5" :
                            PRACTICE_WIDTHS[practiceWidthIndex] == 1f ? "1" :
                                    PRACTICE_WIDTHS[practiceWidthIndex] == 2f ? "2" : "3"));
            practiceToggle.setText(practiceMode ?
                    (replayPending ? "◎ Herhaal…" : "● Vang AAN") : "◎ Vang worp");
            practiceToggle.setTextColor(practiceMode ? 0xFFA7F3D0 : Color.WHITE);
            saveThrow.setVisibility(lastPracticeSwipe == null ? View.GONE : View.VISIBLE);
            skipThrow.setVisibility(lastPracticeSwipe == null ? View.GONE : View.VISIBLE);
        }
        undo.setVisibility(editing ? View.VISIBLE : View.GONE);
        eraser.setVisibility(editing && !canvas.isMoveMode() ? View.VISIBLE : View.GONE);
        move.setVisibility(editing ? View.VISIBLE : View.GONE);
        move.setText(canvas.isMoveMode() ? "✓ Verplaats" : "↔ Verplaats");
        move.setTextColor(canvas.isMoveMode() ? 0xFF67E8F9 : Color.WHITE);
        options.setVisibility(editing && !canvas.isMoveMode() && !proFineOpen ? View.VISIBLE : View.GONE);
        if (proActions != null) {
            boolean showPro = editing && StrokeStore.isProTarget(canvas.target()) && !canvas.isMoveMode();
            proActions.setVisibility(showPro ? View.VISIBLE : View.GONE);
            proStyleActions.setVisibility(showPro ? View.VISIBLE : View.GONE);
            proFinePanel.setVisibility(showPro && proFineOpen ? View.VISIBLE : View.GONE);
            proFineToggle.setText(proFineOpen ? "✓ PRO fijn" : "⊞ PRO fijn");
            glowButton.setText(glowLevel == 0 ? "Glow uit" : "✦ Glow " + glowLevel);
            lineThicknessButton.setText(PRO_WIDTH_LABELS[proLineThickness]);
            proFineStep.setText(NUDGE_STEPS[nudgeIndex] == .5f ? "0,5 dp" :
                    ((int)NUDGE_STEPS[nudgeIndex]) + " dp");
            calibrate.setText(canvas.calibrationStage() == 1 ? "1: Tik bull" :
                    canvas.calibrationStage() == 2 ? "2: Tik bovenrand" : "◎ PRO ijken");
        }
        moveControls.setVisibility(editing && canvas.isMoveMode() ? View.VISIBLE : View.GONE);
        moveStep.setText(NUDGE_STEPS[nudgeIndex] == .5f ? "0,5 dp" :
                ((int)NUDGE_STEPS[nudgeIndex]) + " dp");
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
        // QUICK PLAY build: hide old drawing and guide-building controls,
        // regardless of their previous state. Recording/editing is in remote.
        mode.setVisibility(View.GONE);
        undo.setVisibility(View.GONE);
        eraser.setVisibility(View.GONE);
        move.setVisibility(View.GONE);
        videoFilterRow.setVisibility(View.GONE);
        options.setVisibility(View.GONE);
        proActions.setVisibility(View.GONE);
        proStyleActions.setVisibility(View.GONE);
        proFinePanel.setVisibility(View.GONE);
        moveControls.setVisibility(View.GONE);
        palette.setVisibility(View.GONE);
        versionPanel.setVisibility(View.GONE);
        practiceSettingsRow.setVisibility(View.GONE);
        replayRow.setVisibility(View.VISIBLE);
        playReplayButton.setVisibility(View.GONE);
        saveReplayButton.setVisibility(View.VISIBLE);
        saveReplayButton.setEnabled(lastPracticeSwipe != null
                && canvas.profile().equals(lastPracticeScreenProfile)
                && !replayPending);
        saveReplayButton.setTextColor(saveReplayButton.isEnabled()
                ? 0xFFBBF7D0 : 0xFF94A3B8);
        saveThrow.setVisibility(View.GONE);
        skipThrow.setVisibility(View.GONE);
        quickPlayGrid.setVisibility(View.VISIBLE);
        remoteLauncherRow.setVisibility(View.VISIBLE);
        remoteLauncherButton.setText(remoteOpen
                ? "🎛 Sluit remote" : "🎛 Remote / bewerken");
        if (targetBar != null) targetBar.setVisibility(View.GONE);
        refreshQuickPlayButtons();

        // A toolbar refresh must never expand the menu on its own.
        if (menuMinimized && toolbarExpandedContent != null) {
            for (View control : new View[]{
                    dragHandle, close, minimizeMenuButton}) {
                if (control != null) control.setVisibility(View.GONE);
            }
            toolbarExpandedContent.setVisibility(View.GONE);
            compactBubble.setVisibility(View.VISIBLE);
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
        closeReplayRemote();
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
