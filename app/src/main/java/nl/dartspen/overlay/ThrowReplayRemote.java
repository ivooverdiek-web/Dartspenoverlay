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
        void onClose();
        void onDrag(int dx, int dy);
    }

     private final Listener listener;
    private final Map<String, TextView> targetButtons = new LinkedHashMap<>();
    private TextView modeButton;
    private TextView speedButton;
    private SeekBar speedSlider;
    private TextView message;
    private boolean saveMode;

    ThrowReplayRemote(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setOrientation(VERTICAL);
        setPadding(dp(7), dp(7), dp(7), dp(7));
        GradientDrawable backdrop = new GradientDrawable();
        backdrop.setColor(0xF51A2535);
        backdrop.setCornerRadius(dp(12));
        backdrop.setStroke(dp(1), 0xFF64748B);
        setBackground(backdrop);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(HORIZONTAL);
        addView(header, new LayoutParams(LayoutParams.MATCH_PARENT, dp(42)));

        TextView drag = button("↕", 0xFF334155);
        drag.setTextSize(19f);
        header.addView(drag, new LayoutParams(dp(35), dp(38)));
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
        header.addView(title, new LayoutParams(0, dp(38), 1f));

        TextView close = button("×", 0xFF7F1D1D);
        close.setTextSize(19f);
        header.addView(close, new LayoutParams(dp(34), dp(38)));
        close.setOnClickListener(v -> listener.onClose());

        LinearLayout speedRow = new LinearLayout(context);
        speedRow.setGravity(Gravity.CENTER_VERTICAL);
        speedRow.setOrientation(HORIZONTAL);
        addView(speedRow, new LayoutParams(LayoutParams.MATCH_PARENT, dp(42)));

        TextView slower = button("−", 0xFF334155);
        slower.setTextSize(19f);
        speedRow.addView(slower, new LayoutParams(dp(42), dp(39)));
        slower.setOnClickListener(v -> listener.onSpeedStep(-1));
        speedButton = button("Speed 100% (original)", 0xFF0F766E);
        speedRow.addView(speedButton, new LayoutParams(0, dp(39), 1f));
        speedButton.setContentDescription("Throw replay speed; tap to reset to original");
        speedButton.setOnClickListener(v -> listener.onSpeedPercentChanged(100));
        TextView faster = button("+", 0xFF334155);
        faster.setTextSize(19f);
        speedRow.addView(faster, new LayoutParams(dp(42), dp(39)));
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
        addView(speedSlider, new LayoutParams(LayoutParams.MATCH_PARENT, dp(36)));

        modeButton = button("💾 Save last: OFF", 0xFF334155);
        addView(modeButton, new LayoutParams(LayoutParams.MATCH_PARENT, dp(38)));
        modeButton.setOnClickListener(v -> {
            saveMode = !saveMode;
            setSaveMode(saveMode);
        });

        message = new TextView(context);
        message.setTextSize(11f);
        message.setTextColor(0xFFCBD5E1);
        message.setPadding(dp(4), dp(6), dp(4), dp(6));
        addView(message, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(true);
        addView(scroll, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout sections = new LinearLayout(context);
        sections.setOrientation(VERTICAL);
        scroll.addView(sections);
        addTargetSection(sections, "TREBLES", "T");
        addTargetSection(sections, "DOUBLES", "D");

        LinearLayout bullRow = new LinearLayout(context);
        bullRow.setOrientation(HORIZONTAL);
        sections.addView(bullRow, new LayoutParams(LayoutParams.MATCH_PARENT, dp(44)));
        addTargetButton(bullRow, "Bull", new LayoutParams(LayoutParams.MATCH_PARENT, dp(42)));

        setSaveMode(false);
    }

    private void addTargetSection(LinearLayout parent, String title, String prefix) {
        TextView section = new TextView(getContext());
        section.setText(title);
        section.setTextSize(12f);
        section.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        section.setTextColor(0xFFE2E8F0);
        section.setPadding(dp(5), dp(8), dp(4), dp(3));
        parent.addView(section);

        GridLayout grid = new GridLayout(getContext());
        grid.setColumnCount(5);
        parent.addView(grid, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        for (int number = 20; number >= 1; number--) {
            String key = prefix + number;
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dp(51);
            params.height = dp(41);
            params.setMargins(dp(2), dp(2), dp(2), dp(2));
            addTargetButton(grid, key, params);
        }
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
        view.setOnClickListener(v -> listener.onTargetPressed(key, saveMode));
    }

    void setSaveMode(boolean enabled) {
        saveMode = enabled;
        modeButton.setText(enabled ? "💾 SAVE MODE: Choose a target" : "💾 Save last: OFF");
        modeButton.setBackgroundColor(enabled ? 0xFF166534 : 0xFF334155);
        message.setText(enabled
                ? "Tap a target to save the LAST captured swipe in that slot."
                : "Green = saved. Tap a green target to replay its throw.");
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
                    ? "Replay saved throw " + target
                    : "No recorded throw for " + target);
        }
    }

    void setSpeedPercent(int percent) {
        int clamped = Math.max(1, Math.min(100, percent));
        speedButton.setText(clamped == 100
                ? "Speed 100% (original)" : "Speed " + clamped + "%");
        speedSlider.setProgress(clamped - 1);
    }

    int preferredWidthPx() { return dp(290); }

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
