package nl.dartspen.overlay;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User-operated floating remote. The button only dispatches a stored gesture
 * when pressed; merely displaying or selecting a target never injects input.
 */
final class ThrowReplayRemote extends LinearLayout {
    interface Listener {
        void onTargetPressed(String target, boolean saveMode);
        void onSpeedStep(int delta);
        void onSpeedPercentChanged(int percent);
        void onRingSelected(int ringIndex);
        void onEditTarget(String target);
        void onEditAction(String action);
        void onClose();
        void onDrag(int dx, int dy);
    }

    private final Listener listener;
    private final Map<String, TextView> targetButtons = new LinkedHashMap<>();
    private TextView modeButton, editModeButton;
    private LinearLayout actionModeRow, ringTabs;
    private GridLayout[] ringGrids = new GridLayout[3];
    private TextView[] ringButtons = new TextView[3];
    private int selectedRing = 2; // Default to treble, preserving quick access to T20.
    private TextView speedButton;
    private SeekBar speedSlider;
    private TextView message;
    private TextView editHeading, editModeChoice, editStepChoice, editDurationLabel;
    private TextView durationMinusButton, durationPlusButton, durationStepButton;
    private LinearLayout editorFixedActions;
    private ScrollView targetScroll, editorScroll;
    private LinearLayout speedRow;
    private boolean saveMode, editSelectMode;

    ThrowReplayRemote(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setOrientation(VERTICAL);
        setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable backdrop = new GradientDrawable();
        backdrop.setColor(0xF51A2535);
        backdrop.setCornerRadius(dp(12));
        backdrop.setStroke(dp(1), 0xFF64748B);
        setBackground(backdrop);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(HORIZONTAL);
        addView(header, new LayoutParams(LayoutParams.MATCH_PARENT, dp(36)));

        TextView drag = button("↕", 0xFF334155);
        drag.setTextSize(19f);
        header.addView(drag, new LayoutParams(dp(32), dp(34)));
        drag.setOnTouchListener(new OnTouchListener() {
            float lastX, lastY;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    lastX = e.getRawX(); lastY = e.getRawY();
                    return true;
                }
                if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    int dx = Math.round(e.getRawX() - lastX);
                    int dy = Math.round(e.getRawY() - lastY);
                    lastX = e.getRawX(); lastY = e.getRawY();
                    listener.onDrag(dx, dy);
                    return true;
                }
                return true;
            }
        });

        TextView title = button("🎯 THROW REMOTE", 0xFF1A2535);
        title.setTextSize(12f);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        header.addView(title, new LayoutParams(0, dp(34), 1f));

        TextView close = button("×", 0xFF7F1D1D);
        close.setTextSize(19f);
        header.addView(close, new LayoutParams(dp(32), dp(34)));
        close.setOnClickListener(v -> listener.onClose());

        // Fixed editor actions stay visible when precision controls scroll.
        // Test replays the current unsaved path and returns to this editor.
        editorFixedActions = new LinearLayout(context);
        editorFixedActions.setOrientation(HORIZONTAL);
        editorFixedActions.setVisibility(View.GONE);
        addView(editorFixedActions, new LayoutParams(LayoutParams.MATCH_PARENT, dp(43)));
        actionButton(editorFixedActions, "▶ TEST", "test");
        actionButton(editorFixedActions, "✓ SAVE", "save");
        actionButton(editorFixedActions, "× CANCEL", "cancel");

        speedRow = new LinearLayout(context);
        speedRow.setGravity(Gravity.CENTER_VERTICAL);
        speedRow.setOrientation(HORIZONTAL);
        addView(speedRow, new LayoutParams(LayoutParams.MATCH_PARENT, dp(36)));

        TextView slower = button("−", 0xFF334155);
        slower.setTextSize(19f);
        speedRow.addView(slower, new LayoutParams(dp(38), dp(34)));
        slower.setOnClickListener(v -> listener.onSpeedStep(-1));
        speedButton = button("Speed 100% (original)", 0xFF0F766E);
        speedRow.addView(speedButton, new LayoutParams(0, dp(34), 1f));
        speedButton.setContentDescription("Throw replay speed; tap to reset to original");
        speedButton.setOnClickListener(v -> listener.onSpeedPercentChanged(100));
        TextView faster = button("+", 0xFF334155);
        faster.setTextSize(19f);
        speedRow.addView(faster, new LayoutParams(dp(38), dp(34)));
        faster.setOnClickListener(v -> listener.onSpeedStep(1));
        slower.setOnLongClickListener(v -> {
            listener.onSpeedStep(-10);
            return true;
        });
        faster.setOnLongClickListener(v -> {
            listener.onSpeedStep(10);
            return true;
        });

        speedSlider = new SeekBar(context);
        speedSlider.setMax(99);
        speedSlider.setProgress(99);
        speedSlider.setContentDescription("Replay speed 1 to 100 percent. 100 percent is original speed.");
        speedSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) listener.onSpeedPercentChanged(progress + 1);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        addView(speedSlider, new LayoutParams(LayoutParams.MATCH_PARENT, dp(30)));

        // One compact row instead of two stacked full-width mode buttons.
        actionModeRow = new LinearLayout(context);
        actionModeRow.setOrientation(HORIZONTAL);
        addView(actionModeRow, new LayoutParams(LayoutParams.MATCH_PARENT, dp(36)));
        modeButton = button("💾 Save: OFF", 0xFF334155);
        actionModeRow.addView(modeButton, new LayoutParams(0, dp(34), 1f));
        modeButton.setOnClickListener(v -> setSaveMode(!saveMode));

        editModeButton = button("✎ Edit: OFF", 0xFF334155);
        LinearLayout.LayoutParams editModeParams = new LayoutParams(0, dp(34), 1f);
        editModeParams.setMargins(dp(3), 0, 0, 0);
        actionModeRow.addView(editModeButton, editModeParams);
        editModeButton.setOnClickListener(v -> setEditSelectMode(!editSelectMode));

        message = new TextView(context);
        message.setTextSize(10f);
        message.setTextColor(0xFFCBD5E1);
        message.setPadding(dp(3), dp(3), dp(3), dp(3));
        addView(message, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        targetScroll = new ScrollView(context);
        targetScroll.setFillViewport(false);
        targetScroll.setVerticalScrollBarEnabled(true);
        addView(targetScroll, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout sections = new LinearLayout(context);
        sections.setOrientation(VERTICAL);
        targetScroll.addView(sections);

        ringTabs = new LinearLayout(context);
        ringTabs.setOrientation(HORIZONTAL);
        sections.addView(ringTabs, new LayoutParams(LayoutParams.MATCH_PARENT, dp(36)));
        String[] labels = {"Single", "Double", "Treble"};
        String[] prefixes = {"S", "D", "T"};
        for (int i = 0; i < 3; i++) {
            final int ring = i;
            TextView tab = button(labels[i], 0xFF334155);
            tab.setTextSize(12f);
            LinearLayout.LayoutParams tabParams = new LayoutParams(0, dp(34), 1f);
            if (i != 0) tabParams.setMargins(dp(3), 0, 0, 0);
            ringTabs.addView(tab, tabParams);
            tab.setContentDescription("Choose " + labels[i] + " targets");
            tab.setOnClickListener(v -> {
                setSelectedRing(ring);
                listener.onRingSelected(ring);
            });
            ringButtons[i] = tab;
            ringGrids[i] = createNumberGrid(sections, prefixes[i]);
        }

        // Bull is always visible, regardless of the selected ring.
        LinearLayout bullRow = new LinearLayout(context);
        bullRow.setOrientation(HORIZONTAL);
        LinearLayout.LayoutParams bullRowParams = new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(40));
        bullRowParams.topMargin = dp(2);
        sections.addView(bullRow, bullRowParams);
        addTargetButton(bullRow, "Bull", new LayoutParams(LayoutParams.MATCH_PARENT, dp(38)));

        buildEditorControls(context);
        setSelectedRing(2);
        setSaveMode(false);
    }

    /** Only a single 20-number ring is visible at once. */
    private GridLayout createNumberGrid(LinearLayout parent, String prefix) {
        GridLayout grid = new GridLayout(getContext());
        grid.setColumnCount(5);
        parent.addView(grid, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        for (int number = 20; number >= 1; number--) {
            String key = prefix + number;
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dp(46);
            params.height = dp(38);
            params.setMargins(dp(2), dp(2), dp(2), dp(2));
            addTargetButton(grid, key, params);
        }
        return grid;
    }

    void setSelectedRing(int ring) {
        selectedRing = Math.max(0, Math.min(2, ring));
        for (int i = 0; i < 3; i++) {
            boolean active = i == selectedRing;
            ringGrids[i].setVisibility(active ? View.VISIBLE : View.GONE);
            ringButtons[i].setBackgroundColor(active ? 0xFF0F766E : 0xFF334155);
            ringButtons[i].setTextColor(active ? Color.WHITE : 0xFFCBD5E1);
        }
        targetScroll.scrollTo(0, 0);
    }

    private void addTargetButton(LinearLayout parent, String key, LayoutParams params) {
        TextView view = button(key, 0xFF334155);
        view.setTextSize(12f);
        view.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        parent.addView(view, params);
        setTargetAction(view, key);
    }

    private void addTargetButton(GridLayout parent, String key, GridLayout.LayoutParams params) {
        TextView view = button(key, 0xFF334155);
        view.setTextSize(12f);
        view.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        parent.addView(view, params);
        setTargetAction(view, key);
    }

    private void setTargetAction(TextView view, String key) {
        targetButtons.put(key, view);
        view.setContentDescription("Replay saved throw " + key);
        view.setOnClickListener(v -> {
            if (editSelectMode) listener.onEditTarget(key);
            else listener.onTargetPressed(key, saveMode);
        });
        view.setOnLongClickListener(v -> {
            listener.onEditTarget(key);
            return true;
        });
    }

    void setSaveMode(boolean enabled) {
        saveMode = enabled;
        if (enabled) setEditSelectMode(false);
        modeButton.setText(enabled ? "💾 SAVE: ON" : "💾 Save: OFF");
        modeButton.setBackgroundColor(enabled ? 0xFF166534 : 0xFF334155);
        message.setText(enabled
                ? "Tap a target to save the LAST captured swipe in that slot."
                : "Green = saved. Tap to replay, long-press to edit.");
    }

    void setEditSelectMode(boolean enabled) {
        editSelectMode = enabled;
        if (enabled) {
            saveMode = false;
            modeButton.setText("💾 Save: OFF");
            modeButton.setBackgroundColor(0xFF334155);
        }
        editModeButton.setText(enabled ? "✎ EDIT: ON" : "✎ Edit: OFF");
        editModeButton.setBackgroundColor(enabled ? 0xFF854D0E : 0xFF334155);
        if (enabled) message.setText("Tap a green target to edit only its own swipe.");
        else if (!saveMode) message.setText("Green = saved. Tap to replay, long-press to edit.");
    }

    void refresh(RecordedThrowStore store, String profile, int speedPercent) {
        setSpeedPercent(speedPercent);
        for (Map.Entry<String, TextView> entry : targetButtons.entrySet()) {
            String target = entry.getKey();
            TextView view = entry.getValue();
            boolean saved = store.load(profile, target) != null;
            view.setText(saved ? "▶ " + target : target);
            view.setTextColor(saved ? 0xFFBBF7D0 : Color.WHITE);
            view.setBackgroundColor(saved ? 0xFF14532D : 0xFF334155);
            view.setContentDescription(saved
                    ? "Replay or edit saved throw " + target
                    : "No recorded throw for " + target);
        }
    }

    void setSpeedPercent(int percent) {
        int clamped = Math.max(1, Math.min(100, percent));
        speedButton.setText(clamped == 100
                ? "Speed 100% (original)" : "Speed " + clamped + "%");
        speedSlider.setProgress(clamped - 1);
    }

    /** Replaces target grid with controls for exactly one saved swipe. */
    private void buildEditorControls(Context context) {
        editorScroll = new ScrollView(context);
        editorScroll.setFillViewport(false);
        editorScroll.setVerticalScrollBarEnabled(true);
        editorScroll.setVisibility(View.GONE);
        addView(editorScroll, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(VERTICAL);
        editorScroll.addView(controls);

        editHeading = new TextView(context);
        editHeading.setTextSize(13f);
        editHeading.setTypeface(android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD);
        editHeading.setTextColor(0xFFFACC15);
        editHeading.setPadding(dp(5), dp(4), dp(5), dp(3));
        controls.addView(editHeading);
        TextView hint = new TextView(context);
        hint.setText("Drag the yellow path or endpoints. TEST tries unsaved edits, SAVE keeps them, CANCEL discards.");
        hint.setTextColor(0xFFE2E8F0);
        hint.setTextSize(11f);
        hint.setPadding(dp(5), dp(4), dp(5), dp(5));
        controls.addView(hint);

        LinearLayout modeRow = actionRow(controls);
        editModeChoice = actionButton(modeRow, "Move: Whole", "mode");
        editStepChoice = actionButton(modeRow, "Step: 1 dp", "step");

        LinearLayout moveRow = actionRow(controls);
        actionButton(moveRow, "←", "left");
        actionButton(moveRow, "↑", "up");
        actionButton(moveRow, "↓", "down");
        actionButton(moveRow, "→", "right");

        LinearLayout rotateRow = actionRow(controls);
        actionButton(rotateRow, "↶ 0.25°", "rotateLeft");
        actionButton(rotateRow, "↷ 0.25°", "rotateRight");

        LinearLayout scaleRow = actionRow(controls);
        actionButton(scaleRow, "− 0.5% length", "shorter");
        actionButton(scaleRow, "+ 0.5% length", "longer");

        editDurationLabel = new TextView(context);
        editDurationLabel.setTextColor(0xFFE2E8F0);
        editDurationLabel.setTextSize(12f);
        editDurationLabel.setGravity(Gravity.CENTER);
        controls.addView(editDurationLabel, new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(27)));
        LinearLayout durationRow = actionRow(controls);
        durationMinusButton = actionButton(durationRow, "− 100 ms", "durationMinus");
        durationStepButton = actionButton(durationRow, "Step 100 ms", "durationStep");
        durationPlusButton = actionButton(durationRow, "+ 100 ms", "durationPlus");

        LinearLayout undoRow = actionRow(controls);
        actionButton(undoRow, "↶ Undo", "undo");
        actionButton(undoRow, "Reset", "reset");

        LinearLayout backupRow = actionRow(controls);
        actionButton(backupRow, "Previous saved version", "previous");

        // TEST / SAVE / CANCEL are fixed above the scrolling editor, so that
        // important actions can never disappear at the bottom of the panel.
    }

    private LinearLayout actionRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        parent.addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, dp(43)));
        return row;
    }

    private TextView actionButton(LinearLayout row, String label, String action) {
        TextView view = button(label,
                "save".equals(action) ? 0xFF166534 :
                "cancel".equals(action) ? 0xFF7F1D1D : 0xFF334155);
        view.setTextSize(11f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, dp(39), 1f);
        params.setMargins(dp(2), dp(1), dp(2), dp(1));
        row.addView(view, params);
        view.setOnClickListener(v -> listener.onEditAction(action));
        return view;
    }

    void showEditor(String target, int moveMode, float stepDp, long durationMs) {
        setSaveMode(false);
        setEditSelectMode(false);
        editHeading.setText("✎ Edit saved throw: " + target);
        editorFixedActions.setVisibility(View.VISIBLE);
        updateEditorStatus(moveMode, stepDp, durationMs);
        speedRow.setVisibility(View.GONE);
        speedSlider.setVisibility(View.GONE);
        actionModeRow.setVisibility(View.GONE);
        message.setVisibility(View.GONE);
        targetScroll.setVisibility(View.GONE);
        editorScroll.setVisibility(View.VISIBLE);
    }

    void updateEditorStatus(int moveMode, float stepDp, long durationMs) {
        editModeChoice.setText(moveMode == 1 ? "Move: Start" :
                moveMode == 2 ? "Move: End" : "Move: Whole");
        editStepChoice.setText("Step: " + (stepDp == 0.5f ? "0.5" :
                stepDp == (int)stepDp ? Integer.toString((int)stepDp) :
                Float.toString(stepDp)) + " dp");
        editDurationLabel.setText("Duration: " + durationMs + " ms (max 30,000 ms)");
    }

    void setDurationStep(long stepMs) {
        durationStepButton.setText("Step " + stepMs + "ms");
        durationMinusButton.setText("−" + stepMs);
        durationPlusButton.setText("+" + stepMs);
    }

    void hideEditor() {
        editorFixedActions.setVisibility(View.GONE);
        editorScroll.setVisibility(View.GONE);
        speedRow.setVisibility(View.VISIBLE);
        speedSlider.setVisibility(View.VISIBLE);
        actionModeRow.setVisibility(View.VISIBLE);
        message.setVisibility(View.VISIBLE);
        targetScroll.setVisibility(View.VISIBLE);
        setEditSelectMode(false);
    }

    int preferredWidthPx() { return dp(274); }

    private TextView button(String label, int color) {
        TextView view = new TextView(getContext());
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(13f);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(color);
        view.setPadding(dp(2), dp(3), dp(2), dp(3));
        return view;
    }

    private int dp(float amount) {
        return Math.round(amount * getResources().getDisplayMetrics().density);
    }
}
