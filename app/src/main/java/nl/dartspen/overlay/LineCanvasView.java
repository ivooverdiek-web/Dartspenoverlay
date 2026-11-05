package nl.dartspen.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

final class LineCanvasView extends View {
    interface Tools {
        int color();
        float widthDp();
        boolean straight();
        boolean eraser();
        float eraserDp();
    }
    interface CalibrationListener {
        void onCalibrationStage(int stage, boolean finished);
    }
    interface PracticeSwipeListener {
        void onPracticeSwipe(ArrayList<PointF> points, long durationMs);
    }

    private final StrokeStore store;
    private final Tools tools;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<StrokeStore.Stroke> strokes = new ArrayList<>();
    private final ArrayDeque<ArrayList<StrokeStore.Stroke>> history = new ArrayDeque<>();
    // Keep the board calibration in sync with undo for global PRO adjustments.
    private final ArrayDeque<float[]> anchorHistory = new ArrayDeque<>();
    private StrokeStore.Stroke preview;
    private StrokeStore.Stroke recordedGuidePreview;
    private PracticeSwipeListener practiceListener;
    private boolean practiceCaptureEnabled;
    private final ArrayList<PointF> practicePoints = new ArrayList<>();
    private int practicePointerId = -1;
    private long practiceDownTimeMs;
    private String profile = "";
    private String target = "Vrij";
    // 0 = off, 1 = subtle, 2 = normal, 3 = intense. Appearance only;
    // existing saved points and per-stroke colors remain untouched.
    private int proGlowLevel = 2;
    // Visibility presets: 0=bold (default), 1=extra bold, 2=maximum.
    // These change rendering only; saved PRO calibration remains unchanged.
    private int proLineThickness = 0;
    private final Paint neonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private CalibrationListener calibrationListener;
    private int calibrationStage = 0;
    private PointF calibrationBull;
    private boolean moveMode = false;
    private int selectedLine = -1;
    private int movePointerId = -1;
    private boolean draggingLine = false;
    private boolean movedLine = false;
    private float downX, downY;
    private final ArrayList<PointF> originalMovePoints = new ArrayList<>();
    private boolean editable = true;
    private boolean erasing = false;
    private boolean eraserChanged = false;
    private int pointerId = -1;
    private PointF previousEraser;
    private PointF eraserPosition;
    private final float density;

    LineCanvasView(Context context, StrokeStore store, Tools tools) {
        super(context);
        this.store = store;
        this.tools = tools;
        this.density = getResources().getDisplayMetrics().density;
        setBackgroundColor(Color.TRANSPARENT);
    }

    void setProGlowLevel(int level) {
        proGlowLevel = Math.max(0, Math.min(3, level));
        invalidate();
    }

    void setProLineThickness(int level) {
        proLineThickness = Math.max(0, Math.min(2, level));
        invalidate();
    }

    void setPracticeSwipeListener(PracticeSwipeListener listener) {
        practiceListener = listener;
    }
    boolean practiceCaptureEnabled() { return practiceCaptureEnabled; }
    void setPracticeCaptureEnabled(boolean enabled) {
        if (enabled && editable) {
            // Never silently save a partially drawn line as a practice throw.
            finish();
            setEditable(false);
        }
        practiceCaptureEnabled = enabled;
        practicePointerId = -1;
        practicePoints.clear();
        invalidate();
    }

    /**
     * Capture just the swipe drawn on our overlay. Android does not allow a
     * pass-through overlay to read the same swipe sent straight to Darts Club.
     * The service can optionally replay it into the game afterwards.
     */
    private void addPracticePoint(float x, float y) {
        if (practicePoints.size() >= 200) return;
        PointF point = normalized(x, y);
        if (!practicePoints.isEmpty()) {
            PointF previous = practicePoints.get(practicePoints.size()-1);
            if (Math.hypot((point.x - previous.x)*getWidth(),
                    (point.y - previous.y)*getHeight()) < 1.2f*density) return;
        }
        practicePoints.add(point);
        invalidate();
    }

    private boolean onPracticeSwipeTouch(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            practicePointerId = event.getPointerId(0);
            // While the new gesture is drawn, hide the old unsaved preview.
            recordedGuidePreview = null;
            practicePoints.clear();
            practiceDownTimeMs = event.getEventTime();
            addPracticePoint(event.getX(), event.getY());
            return true;
        }
        if (practicePointerId < 0) return true;
        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            // Only one-pointer throws are supported. Do not replay multi-touch.
            practicePointerId = -1;
            practicePoints.clear();
            return true;
        }
        int index = event.findPointerIndex(practicePointerId);
        if (index < 0) return true;
        if (action == MotionEvent.ACTION_MOVE) {
            for (int i=0; i<event.getHistorySize(); i++)
                addPracticePoint(event.getHistoricalX(index,i), event.getHistoricalY(index,i));
            addPracticePoint(event.getX(index), event.getY(index));
            return true;
        }
        if (action == MotionEvent.ACTION_UP) {
            addPracticePoint(event.getX(index), event.getY(index));
            long elapsed = event.getEventTime()-practiceDownTimeMs;
            if (practicePoints.size() >= 2 && elapsed >= 25 && elapsed <= 1400) {
                PointF first = practicePoints.get(0);
                PointF last = practicePoints.get(practicePoints.size()-1);
                if (Math.hypot((first.x-last.x)*getWidth(),
                        (first.y-last.y)*getHeight()) >= 20f*density) {
                    if (practiceListener != null) {
                        practiceListener.onPracticeSwipe(
                                new ArrayList<>(practicePoints), elapsed);
                    }
                }
            }
            practicePointerId = -1;
            practicePoints.clear();
            return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            practicePointerId = -1;
            practicePoints.clear();
        }
        return true;
    }

    /**
     * Generate a longer version of the physical practice swipe, keeping its
     * start fixed and preserving curves/angles. We cap extension as a whole,
     * rather than clipping individual points and ruining the trajectory.
     */
    private StrokeStore.Stroke recordedStroke(List<PointF> recorded,
                                              float requestedLength, float widthDp) {
        if (recorded == null || recorded.size() < 2) return null;
        ArrayList<PointF> valid = new ArrayList<>();
        for (PointF p : recorded) {
            if (p != null && Float.isFinite(p.x) && Float.isFinite(p.y))
                valid.add(new PointF(p.x,p.y));
        }
        if (valid.size() < 2) return null;
        PointF start = valid.get(0);
        float extension = Math.max(1f, Math.min(8f, requestedLength));
        // Limit the *entire* geometry to the same scale. A small part may go
        // offscreen (to allow full-size guides) but never more than 20%.
        for (PointF p : valid) {
            float dx = p.x-start.x, dy = p.y-start.y;
            if (dx > 0f) extension = Math.min(extension,(1.2f-start.x)/dx);
            if (dx < 0f) extension = Math.min(extension,(-.2f-start.x)/dx);
            if (dy > 0f) extension = Math.min(extension,(1.2f-start.y)/dy);
            if (dy < 0f) extension = Math.min(extension,(-.2f-start.y)/dy);
        }
        extension = Math.max(1f,extension);
        StrokeStore.Stroke stroke = new StrokeStore.Stroke(
                0xFFB4FF5B, Math.max(1f, Math.min(4f, widthDp)), false);
        stroke.arrow = true;
        stroke.captured = true;
        for (PointF p : valid) {
            stroke.points.add(new PointF(start.x+(p.x-start.x)*extension,
                    start.y+(p.y-start.y)*extension));
        }
        return stroke;
    }

    void setRecordedGuidePreview(List<PointF> recorded, float length, float widthDp) {
        recordedGuidePreview = recordedStroke(recorded,length,widthDp);
        invalidate();
    }

    void clearRecordedGuidePreview() {
        recordedGuidePreview = null;
        invalidate();
    }

    /** Save the extended trajectory, without modifying the original PRO set. */
    boolean saveRecordedThrow(List<PointF> recorded, float length, float widthDp) {
        if (profile.isEmpty()) return false;
        StrokeStore.Stroke stroke = recordedStroke(recorded,length,widthDp);
        if (stroke == null || stroke.points.size() < 2) return false;
        finish();
        saveUndo();
        strokes.add(stroke);
        if (strokes.size() > 500) strokes.remove(0);
        store.save(layerProfile(), strokes);
        clearRecordedGuidePreview();
        invalidate();
        return true;
    }

    String profile() { return profile; }
    String target() { return target; }
    String layerProfile() { return StrokeStore.layerProfile(profile, target); }

    /**
     * Fine-adjust the entire PRO sketch in one step.
     * dxDp/dyDp are touch-friendly physical dp, scale is a multiplier,
     * angleDegrees is clockwise rotation around the calibrated bull.
     * All existing strokes, including arrows, are transformed together.
     */
    boolean fineAdjustPro(float dxDp, float dyDp, float scale,
                          float angleDegrees) {
        if (!editable || !"PRO".equals(target) || calibrationStage != 0 ||
                moveMode || strokes.isEmpty() || getWidth() <= 0 || getHeight() <= 0) {
            return false;
        }
        if (scale <= 0f || scale > 1.1f || Math.abs(angleDegrees) > 5f ||
                Math.abs(dxDp) > 20f || Math.abs(dyDp) > 20f) {
            return false;
        }
        String layer = layerProfile();
        float width = getWidth(), height = getHeight();
        float oldCx = store.proAnchorX(layer) * width;
        float oldCy = store.proAnchorY(layer) * height;
        float newCx = oldCx + dxDp * density;
        float newCy = oldCy + dyDp * density;
        double radians = Math.toRadians(angleDegrees);
        double cos = Math.cos(radians), sin = Math.sin(radians);

        // Calculate first; never partially modify saved lines.
        ArrayList<ArrayList<PointF>> transformed = new ArrayList<>();
        float left = Float.MAX_VALUE, right = -Float.MAX_VALUE;
        float top = Float.MAX_VALUE, bottom = -Float.MAX_VALUE;
        for (StrokeStore.Stroke stroke : strokes) {
            ArrayList<PointF> pts = new ArrayList<>();
            for (PointF point : stroke.points) {
                double x = (point.x * width - oldCx) * scale;
                double y = (point.y * height - oldCy) * scale;
                float newX = (float)(newCx + x*cos - y*sin);
                float newY = (float)(newCy + x*sin + y*cos);
                pts.add(new PointF(newX, newY));
                left = Math.min(left, newX);
                right = Math.max(right, newX);
                top = Math.min(top, newY);
                bottom = Math.max(bottom, newY);
            }
            transformed.add(pts);
        }
        // Keep the group intact; do not independently clamp every point.
        // Permit guide lines to extend a little beyond the visible game area.
        // The drawing canvas clips those portions; saved geometry stays intact.
        if (left < -width * .25f || right > width * 1.25f ||
                top < -height * .25f || bottom > height * 1.25f) {
            return false;
        }
        saveUndo();
        for (int i=0; i<strokes.size(); i++) {
            ArrayList<PointF> src = transformed.get(i);
            ArrayList<PointF> dst = strokes.get(i).points;
            for (int j=0; j<dst.size(); j++) {
                dst.get(j).x = src.get(j).x / width;
                dst.get(j).y = src.get(j).y / height;
            }
        }
        store.save(layer, strokes);
        store.saveProCalibration(layer, newCx / width, newCy / height,
                store.proRadiusY(layer) * scale);
        selectedLine = -1;
        invalidate();
        return true;
    }

    int calibrationStage() { return calibrationStage; }
    void setCalibrationListener(CalibrationListener callback) {
        calibrationListener = callback;
    }
    void cancelCalibration() {
        if (calibrationStage == 0) return;
        calibrationStage = 0;
        calibrationBull = null;
        if (calibrationListener != null) calibrationListener.onCalibrationStage(0, false);
        invalidate();
    }

    void toggleProCalibration() {
        if (calibrationStage != 0) {
            cancelCalibration();
            return;
        }
        if (!editable || !"PRO".equals(target)) return;
        finish();
        setMoveMode(false);
        calibrationStage = 1;
        calibrationBull = null;
        if (calibrationListener != null) calibrationListener.onCalibrationStage(1, false);
        invalidate();
    }

    private void completeCalibration(PointF center, PointF top) {
        if (center == null || top == null || !"PRO".equals(target)) return;
        float newRadius = (center.y - top.y) * getHeight();
        if (newRadius < 60f * density || newRadius > getHeight() * .6f) {
            if (calibrationListener != null) calibrationListener.onCalibrationStage(2, false);
            return;
        }
        float oldX = store.proAnchorX(layerProfile()) * getWidth();
        float oldY = store.proAnchorY(layerProfile()) * getHeight();
        float oldRadius = store.proRadiusY(layerProfile()) * getHeight();
        float scale = newRadius / Math.max(1f, oldRadius);
        if (scale < .45f || scale > 2.5f) {
            if (calibrationListener != null) calibrationListener.onCalibrationStage(2, false);
            return;
        }

        // A stored version is retained before any geometry is transformed.
        store.saveVersion(layerProfile(), "Back-up vóór PRO ijken");
        saveUndo();
        float newX = center.x * getWidth(), newY = center.y * getHeight();
        for (StrokeStore.Stroke line : strokes) {
            for (PointF p : line.points) {
                float x = newX + (p.x * getWidth() - oldX) * scale;
                float y = newY + (p.y * getHeight() - oldY) * scale;
                p.x = Math.max(-.25f, Math.min(1.25f, x / getWidth()));
                p.y = Math.max(-.25f, Math.min(1.25f, y / getHeight()));
            }
        }
        store.save(layerProfile(), strokes);
        store.saveProCalibration(layerProfile(), center.x, center.y, newRadius / getHeight());
        calibrationStage = 0;
        calibrationBull = null;
        if (calibrationListener != null) calibrationListener.onCalibrationStage(0, true);
        invalidate();
    }

    private boolean onCalibrationTouch(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
        PointF hit = normalized(event.getX(), event.getY());
        if (calibrationStage == 1) {
            calibrationBull = hit;
            calibrationStage = 2;
            if (calibrationListener != null) calibrationListener.onCalibrationStage(2, false);
        } else if (calibrationStage == 2) {
            completeCalibration(calibrationBull, hit);
        }
        invalidate();
        return true;
    }



    // Selecting an existing line is separate from drawing or erasing it.
    void setMoveMode(boolean enabled) {
        if (moveMode == enabled) return;
        finish();
        moveMode = enabled;
        selectedLine = enabled && !strokes.isEmpty() ? strokes.size() - 1 : -1;
        draggingLine = false;
        movedLine = false;
        movePointerId = -1;
        originalMovePoints.clear();
        invalidate();
    }

    boolean isMoveMode() { return moveMode; }
    boolean hasSelectedLine() { return selectedLine >= 0 && selectedLine < strokes.size(); }

    // dx/dy are physical dp, converted to normalized coordinates before storage.
    boolean nudgeSelected(float dxDp, float dyDp) {
        if (!editable || !moveMode || !hasSelectedLine()) return false;
        StrokeStore.Stroke stroke = strokes.get(selectedLine);
        float dx = dxDp * density / Math.max(1, getWidth());
        float dy = dyDp * density / Math.max(1, getHeight());
        float[] range = offsetBounds(stroke.points);
        dx = Math.max(-range[0], Math.min(1f - range[1], dx));
        dy = Math.max(-range[2], Math.min(1f - range[3], dy));
        if (Math.abs(dx) < .0000001f && Math.abs(dy) < .0000001f) return false;
        saveUndo();
        for (PointF p : stroke.points) { p.x += dx; p.y += dy; }
        store.save(layerProfile(), strokes);
        invalidate();
        return true;
    }

    private float[] offsetBounds(List<PointF> points) {
        float minX = 1f, maxX = 0f, minY = 1f, maxY = 0f;
        for (PointF p : points) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }
        return new float[]{minX, maxX, minY, maxY};
    }

    private void clearMoveGesture() {
        draggingLine = false;
        movedLine = false;
        movePointerId = -1;
        originalMovePoints.clear();
    }

    private int findClosestLine(float x, float y) {
        PointF pos = normalized(x, y);
        float best = 22f * density;
        int bestIndex = -1;
        for (int i = strokes.size() - 1; i >= 0; i--) {
            StrokeStore.Stroke line = strokes.get(i);
            if (line.points.isEmpty()) continue;
            float distance = Float.MAX_VALUE;
            if (line.points.size() == 1) {
                distance = distancePx(line.points.get(0), pos);
            } else {
                int count = line.straight ? 2 : line.points.size();
                for (int j = 1; j < count; j++) {
                    PointF a = line.straight ? line.points.get(0) : line.points.get(j-1);
                    PointF b = line.straight ? line.points.get(line.points.size()-1)
                            : line.points.get(j);
                    distance = Math.min(distance, pointSegmentDistance(pos, a, b));
                }
            }
            distance -= line.widthDp * density / 2f;
            if (distance < best) {
                best = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private boolean onMoveTouch(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            selectedLine = findClosestLine(event.getX(), event.getY());
            clearMoveGesture();
            if (hasSelectedLine()) {
                movePointerId = event.getPointerId(0);
                downX = event.getX();
                downY = event.getY();
                for (PointF p : strokes.get(selectedLine).points) {
                    originalMovePoints.add(new PointF(p.x, p.y));
                }
                draggingLine = true;
            }
            invalidate();
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE && draggingLine) {
            int index = event.findPointerIndex(movePointerId);
            if (index < 0 || !hasSelectedLine()) return true;
            float rawDx = (event.getX(index) - downX) / Math.max(1, getWidth());
            float rawDy = (event.getY(index) - downY) / Math.max(1, getHeight());
            if (!movedLine && Math.hypot(rawDx * getWidth(), rawDy * getHeight()) < 2f * density) return true;
            if (!movedLine) {
                saveUndo(); // One undo step for the entire drag gesture.
                movedLine = true;
            }
            float[] range = offsetBounds(originalMovePoints);
            float dx = Math.max(-range[0], Math.min(1f - range[1], rawDx));
            float dy = Math.max(-range[2], Math.min(1f - range[3], rawDy));
            ArrayList<PointF> points = strokes.get(selectedLine).points;
            for (int j = 0; j < points.size(); j++) {
                points.get(j).x = originalMovePoints.get(j).x + dx;
                points.get(j).y = originalMovePoints.get(j).y + dy;
            }
            invalidate();
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (movedLine && hasSelectedLine()) store.save(layerProfile(), strokes);
            clearMoveGesture();
            invalidate();
            return true;
        }
        return true;
    }

    void selectTarget(String chosen) {
        if (profile.isEmpty() || target.equals(chosen)) return;
        cancelCalibration();
        finish();
        target = chosen;
        recordedGuidePreview = null;
        store.selectedTarget(profile, target);
        strokes.clear();
        strokes.addAll(store.load(layerProfile()));
        history.clear();
        anchorHistory.clear();
        selectedLine = moveMode && !strokes.isEmpty() ? strokes.size() - 1 : -1;
        clearMoveGesture();
        preview = null;
        erasing = false;
        eraserPosition = null;
        pointerId = -1;
        invalidate();
    }

    void setEditable(boolean value) {
        if (!value) { cancelCalibration(); finish(); clearMoveGesture(); }
        editable = value;
        invalidate();
    }

    private ArrayList<StrokeStore.Stroke> copyLines() {
        ArrayList<StrokeStore.Stroke> copy = new ArrayList<>();
        for (StrokeStore.Stroke original : strokes) {
            StrokeStore.Stroke line = new StrokeStore.Stroke(
                    original.color, original.widthDp, original.straight);
            line.arrow = original.arrow;
            line.captured = original.captured;
            for (PointF point : original.points) line.points.add(new PointF(point.x, point.y));
            copy.add(line);
        }
        return copy;
    }

    private void saveUndo() {
        history.push(copyLines());
        if ("PRO".equals(target)) {
            anchorHistory.push(new float[]{store.proAnchorX(layerProfile()),
                    store.proAnchorY(layerProfile()), store.proRadiusY(layerProfile())});
        } else {
            anchorHistory.push(new float[0]);
        }
        while (history.size() > 12) {
            history.removeLast();
            anchorHistory.removeLast();
        }
    }

    void undo() {
        finish();
        if (history.isEmpty()) return;
        strokes.clear();
        strokes.addAll(history.pop());
        if (!anchorHistory.isEmpty()) {
            float[] calibration = anchorHistory.pop();
            if (calibration.length == 3 && "PRO".equals(target)) {
                store.saveProCalibration(layerProfile(),
                        calibration[0], calibration[1], calibration[2]);
            }
        }
        selectedLine = -1;
        clearMoveGesture();
        store.save(layerProfile(), strokes);
        invalidate();
    }

    int countLines() { return strokes.size(); }

    void clear() {
        finish();
        if (strokes.isEmpty()) return;
        saveUndo();
        strokes.clear();
        selectedLine = -1;
        clearMoveGesture();
        store.save(layerProfile(), strokes);
        invalidate();
    }

    void reloadFromStorage() {
        finish();
        saveUndo();
        strokes.clear();
        strokes.addAll(store.load(layerProfile()));
        selectedLine = -1;
        clearMoveGesture();
        invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        if (w <= 0 || h <= 0) return;
        float aspect = (float)Math.max(w, h) / Math.min(w, h);
        String next = (aspect < 1.5f ? "inner" : "cover") +
                (w > h ? "_landscape" : "_portrait");
        if (!next.equals(profile)) {
            if (!profile.isEmpty()) { cancelCalibration(); finish(); }
            profile = next;
            practiceCaptureEnabled = false;
            practicePointerId = -1;
            practicePoints.clear();
            recordedGuidePreview = null;
            target = store.selectedTarget(profile);
            strokes.clear();
            strokes.addAll(store.load(layerProfile()));
            history.clear();
            anchorHistory.clear();
            selectedLine = moveMode && !strokes.isEmpty() ? strokes.size() - 1 : -1;
            clearMoveGesture();
            preview = null;
            erasing = false;
            eraserPosition = null;
            pointerId = -1;
        }
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        for (StrokeStore.Stroke stroke : strokes) render(c, stroke);
        if (editable && moveMode && hasSelectedLine()) highlightSelected(c);
        if (editable && calibrationStage == 2 && calibrationBull != null) {
            paint.setColor(0xFF00FFAE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.5f * density);
            c.drawCircle(calibrationBull.x * getWidth(), calibrationBull.y * getHeight(),
                    13f * density, paint);
            c.drawLine(calibrationBull.x * getWidth() - 18f*density,
                    calibrationBull.y * getHeight(),
                    calibrationBull.x * getWidth() + 18f*density,
                    calibrationBull.y * getHeight(), paint);
        }
        if (preview != null) render(c, preview);
        if (recordedGuidePreview != null) render(c, recordedGuidePreview);
        if (practiceCaptureEnabled && practicePoints.size() >= 2) {
            Path capturePath = new Path();
            PointF start = practicePoints.get(0);
            capturePath.moveTo(start.x*getWidth(),start.y*getHeight());
            for (int i=1; i<practicePoints.size(); i++) {
                PointF p = practicePoints.get(i);
                capturePath.lineTo(p.x*getWidth(),p.y*getHeight());
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(1.75f*density);
            paint.setColor(0xFFB4FF5B);
            c.drawPath(capturePath,paint);
        }
        if (erasing && eraserPosition != null) {
            float x = eraserPosition.x * getWidth(), y = eraserPosition.y * getHeight();
            float radius = tools.eraserDp() * density / 2f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x44FFFFFF);
            c.drawCircle(x, y, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, density));
            paint.setColor(0xFFFA5252);
            c.drawCircle(x, y, radius, paint);
        }
    }


    private void highlightSelected(Canvas canvas) {
        StrokeStore.Stroke line = strokes.get(selectedLine);
        if (line.points.isEmpty() ||
                ("PRO".equals(target) && !line.arrow && !line.captured)) return;
        paint.setStrokeWidth(Math.max(2f * density, line.widthDp * density + 4f * density));
        paint.setColor(0x99FFFFFF);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        if (line.points.size() == 1) {
            PointF p = line.points.get(0);
            canvas.drawCircle(p.x*getWidth(), p.y*getHeight(), 6f*density, paint);
        } else if (line.straight) {
            PointF a = line.points.get(0), b = line.points.get(line.points.size()-1);
            canvas.drawLine(a.x*getWidth(), a.y*getHeight(),
                    b.x*getWidth(), b.y*getHeight(), paint);
        } else {
            Path path = new Path();
            PointF p = line.points.get(0);
            path.moveTo(p.x*getWidth(), p.y*getHeight());
            for (int i=1; i<line.points.size(); i++) {
                p = line.points.get(i);
                path.lineTo(p.x*getWidth(), p.y*getHeight());
            }
            canvas.drawPath(path, paint);
        }
        render(canvas, line); // Show original color in the middle of selection outline.
    }

    private void render(Canvas canvas, StrokeStore.Stroke stroke) {
        if (stroke.points.isEmpty()) return;
        if ("PRO".equals(target)) {
            // In PRO show only the gold aiming lines and separately saved
            // (green) practice throws. Keep the old cyan guides in storage
            // for safety, but never paint them.
            if (!stroke.arrow && !stroke.captured) return;
            renderNeonPro(canvas, stroke);
            return;
        }
        paint.setColor(stroke.color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke.widthDp * density);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        PointF first = stroke.points.get(0);
        if (stroke.points.size() == 1) {
            canvas.drawPoint(first.x * getWidth(), first.y * getHeight(), paint);
        } else if (stroke.straight) {
            PointF last = stroke.points.get(stroke.points.size() - 1);
            canvas.drawLine(first.x * getWidth(), first.y * getHeight(),
                    last.x * getWidth(), last.y * getHeight(), paint);
        } else {
            Path path = new Path();
            path.moveTo(first.x * getWidth(), first.y * getHeight());
            for (int i = 1; i < stroke.points.size(); i++) {
                PointF p = stroke.points.get(i);
                path.lineTo(p.x * getWidth(), p.y * getHeight());
            }
            canvas.drawPath(path, paint);
        }
        if (stroke.arrow && stroke.points.size() >= 2) drawArrowHead(canvas, stroke);
    }


    /**
     * High-contrast neon stroke renderer. It deliberately uses layered strokes
     * instead of blur filters: this works on hardware-accelerated transparent
     * overlay windows, including the Honor Magic V5, without bitmap allocations.
     *
     * A dark under-stroke maintains contrast against bright dartboard areas;
     * translucent bright bands simulate glow against dark board areas.
     */
    private void renderNeonPro(Canvas canvas, StrokeStore.Stroke stroke) {
        boolean isArrow = stroke.arrow;
        boolean captured = stroke.captured;
        int neon = captured ? Color.rgb(180, 255, 91) : Color.rgb(255, 235, 48);
        int halo = captured ? Color.rgb(86, 220, 76) : Color.rgb(255, 192, 35);
        int brightCenter = captured ? Color.rgb(242, 255, 208) : Color.rgb(255, 255, 195);
        // Wider solid cores (not huge diffuse halos) make PRO guidelines
        // readable on the dartboard while keeping their saved paths intact.
        float[] widthsDp = {3.25f, 4.75f, 6.5f};
        float base = (captured ? stroke.widthDp :
                widthsDp[proLineThickness] + (isArrow ? .1f : 0f)) * density;

        Path geometry = new Path();
        PointF first = stroke.points.get(0);
        geometry.moveTo(first.x * getWidth(), first.y * getHeight());
        if (stroke.points.size() == 1) {
            geometry.lineTo(first.x * getWidth() + .01f, first.y * getHeight() + .01f);
        } else if (stroke.straight) {
            PointF last = stroke.points.get(stroke.points.size() - 1);
            geometry.lineTo(last.x * getWidth(), last.y * getHeight());
        } else {
            for (int i = 1; i < stroke.points.size(); i++) {
                PointF p = stroke.points.get(i);
                geometry.lineTo(p.x * getWidth(), p.y * getHeight());
            }
        }

        // No triangle or arrowhead: every PRO guide ends as a rounded line.

        neonPaint.setStyle(Paint.Style.STROKE);
        neonPaint.setStrokeCap(Paint.Cap.ROUND);
        neonPaint.setStrokeJoin(Paint.Join.ROUND);
        neonPaint.clearShadowLayer();
        neonPaint.setAlpha(255);

        // Moderate bright glow. Keep the solid central line dominant, rather
        // than filling the board with a thick transparent fog.
        if (proGlowLevel > 0) {
            float intensity = proGlowLevel == 1 ? .55f : proGlowLevel == 2 ? 1f : 1.5f;
            neonPaint.setColor(Color.argb((int)(18f * intensity),
                    Color.red(halo), Color.green(halo), Color.blue(halo)));
            neonPaint.setStrokeWidth(base + (3.8f + proGlowLevel * .55f) * density);
            canvas.drawPath(geometry, neonPaint);
            neonPaint.setColor(Color.argb((int)(38f * intensity),
                    Color.red(halo), Color.green(halo), Color.blue(halo)));
            neonPaint.setStrokeWidth(base + (1.7f + proGlowLevel * .3f) * density);
            canvas.drawPath(geometry, neonPaint);
        }

        // Pronounced dark edge separates bright neon from green/white sectors.
        neonPaint.setColor(Color.argb(235, 5, 13, 25));
        neonPaint.setStrokeWidth(base + 2.25f * density);
        canvas.drawPath(geometry, neonPaint);
        neonPaint.setColor(neon);
        neonPaint.setStrokeWidth(base);
        canvas.drawPath(geometry, neonPaint);
        neonPaint.setColor(brightCenter);
        neonPaint.setStrokeWidth(Math.max(.35f * density, base * .37f));
        canvas.drawPath(geometry, neonPaint);

        // No starting markers either: just the saved line geometry.
    }

    private void drawArrowHead(Canvas canvas, StrokeStore.Stroke stroke) {
        PointF end = stroke.points.get(stroke.points.size() - 1);
        PointF prev = stroke.straight ? stroke.points.get(0)
                : stroke.points.get(stroke.points.size() - 2);
        float dx = (end.x - prev.x) * getWidth();
        float dy = (end.y - prev.y) * getHeight();
        float length = (float)Math.hypot(dx, dy);
        if (length < 0.0001f) return;
        float ux = dx / length, uy = dy / length;
        float tipX = end.x * getWidth(), tipY = end.y * getHeight();
        float size = Math.max(9f*density, stroke.widthDp*4f*density);
        float bx = tipX - ux*size, by = tipY - uy*size;
        float wing = size*0.45f;
        canvas.drawLine(tipX, tipY, bx - uy*wing, by + ux*wing, paint);
        canvas.drawLine(tipX, tipY, bx + uy*wing, by - ux*wing, paint);
        // Small dot at the starting point as in the photo.
        PointF start = stroke.points.get(0);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(start.x*getWidth(), start.y*getHeight(),
                Math.max(2f*density, stroke.widthDp*density*1.2f), paint);
        paint.setStyle(Paint.Style.STROKE);
    }

    private PointF normalized(float x, float y) {
        return new PointF(Math.max(0f, Math.min(1f, x / Math.max(1, getWidth()))),
                Math.max(0f, Math.min(1f, y / Math.max(1, getHeight()))));
    }

    private void finish() {
        if (preview != null && !preview.points.isEmpty() && !profile.isEmpty()) {
            saveUndo();
            strokes.add(preview);
            if (strokes.size() > 500) strokes.remove(0);
            store.save(layerProfile(), strokes);
        }
        if (erasing && eraserChanged && !profile.isEmpty()) store.save(layerProfile(), strokes);
        preview = null;
        erasing = false;
        eraserChanged = false;
        previousEraser = null;
        eraserPosition = null;
        pointerId = -1;
        invalidate();
    }

    private float distancePx(PointF a, PointF b) {
        return (float)Math.hypot((a.x - b.x) * getWidth(), (a.y - b.y) * getHeight());
    }

    private float pointSegmentDistance(PointF p, PointF a, PointF b) {
        float ax = a.x * getWidth(), ay = a.y * getHeight();
        float bx = b.x * getWidth(), by = b.y * getHeight();
        float px = p.x * getWidth(), py = p.y * getHeight();
        float dx = bx - ax, dy = by - ay;
        float lenSq = dx * dx + dy * dy;
        float t = lenSq <= 0.00001f ? 0f
                : Math.max(0, Math.min(1, ((px-ax)*dx+(py-ay)*dy)/lenSq));
        return (float)Math.hypot(px - (ax + t*dx), py - (ay + t*dy));
    }

    private boolean intersects(StrokeStore.Stroke line, PointF brush, float radius) {
        if (line.points.isEmpty()) return false;
        float reach = radius + line.widthDp * density / 2f;
        if (line.points.size() == 1)
            return distancePx(line.points.get(0), brush) <= reach;
        for (int i = 1; i < line.points.size(); i++) {
            if (pointSegmentDistance(brush, line.points.get(i-1),
                    line.points.get(i)) <= reach) return true;
        }
        return false;
    }

    private StrokeStore.Stroke segment(StrokeStore.Stroke original) {
        StrokeStore.Stroke stroke = new StrokeStore.Stroke(
                original.color, original.widthDp, false);
        stroke.captured = original.captured;
        return stroke;
    }

    private void eraseAt(PointF brush) {
        float radius = tools.eraserDp() * density / 2f;
        ArrayList<StrokeStore.Stroke> result = new ArrayList<>();
        boolean changed = false;
        for (StrokeStore.Stroke source : strokes) {
            if (!intersects(source, brush, radius)) {
                result.add(source);
                continue;
            }
            changed = true;
            StrokeStore.Stroke current = segment(source);
            float reach = radius + source.widthDp * density / 2f;
            if (source.points.size() == 1) continue;
            // Sample the stroke at a fine interval so erasing a short piece
            // splits the line rather than deleting the complete stroke.
            float totalLength = 0;
            for (int i = 1; i < source.points.size(); i++)
                totalLength += distancePx(source.points.get(i-1), source.points.get(i));
            float step = Math.max(2f * density, totalLength / 3000f);
            for (int i = 1; i < source.points.size(); i++) {
                PointF a = source.points.get(i-1), b = source.points.get(i);
                int steps = Math.max(1, Math.min(4000,
                        (int)Math.ceil(distancePx(a,b) / step)));
                for (int j = (i == 1 ? 0 : 1); j <= steps; j++) {
                    float t = (float)j / steps;
                    PointF p = new PointF(a.x + (b.x - a.x)*t,
                            a.y + (b.y - a.y)*t);
                    boolean erased = distancePx(p, brush) <= reach;
                    if (erased) {
                        if (current.points.size() >= 2) {
                current.arrow = source.arrow;
                result.add(current);
            }
                        current = segment(source);
                    } else if (current.points.size() < 3900) {
                        current.points.add(p);
                    }
                }
            }
            if (current.points.size() >= 2) result.add(current);
        }
        if (changed) {
            eraserChanged = true;
            strokes.clear();
            // Keep within storage's 500-stroke maximum.
            if (result.size() > 500) {
                strokes.addAll(result.subList(result.size() - 500, result.size()));
            } else strokes.addAll(result);
        }
    }

    private void moveEraser(PointF point) {
        if (previousEraser == null) {
            eraseAt(point);
        } else {
            int parts = Math.max(1, Math.min(150,
                    (int)Math.ceil(distancePx(previousEraser, point) /
                            Math.max(2f*density, tools.eraserDp()*density/3f))));
            for (int i = 1; i <= parts; i++) {
                float t = (float)i / parts;
                eraseAt(new PointF(previousEraser.x+(point.x-previousEraser.x)*t,
                        previousEraser.y+(point.y-previousEraser.y)*t));
            }
        }
        previousEraser = point;
        eraserPosition = point;
        invalidate();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (profile.isEmpty()) return false;
        if (practiceCaptureEnabled) return onPracticeSwipeTouch(event);
        if (!editable) return false;
        if (calibrationStage != 0) return onCalibrationTouch(event);
        if (moveMode) return onMoveTouch(event);
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            pointerId = event.getPointerId(0);
            if (tools.eraser()) {
                saveUndo();
                erasing = true;
                eraserChanged = false;
                moveEraser(normalized(event.getX(), event.getY()));
            } else {
                preview = new StrokeStore.Stroke(tools.color(), tools.widthDp(),
                        tools.straight());
                // New strokes in PRO follow the remaining gold, round-ended
                // line style; they are not rendered as hidden cyan guides.
                if ("PRO".equals(target)) preview.arrow = true;
                preview.points.add(normalized(event.getX(), event.getY()));
                invalidate();
            }
            return true;
        }
        if (pointerId < 0) return true;
        int index = event.findPointerIndex(pointerId);
        if (action == MotionEvent.ACTION_MOVE && index >= 0) {
            PointF point = normalized(event.getX(index), event.getY(index));
            if (erasing) {
                moveEraser(point);
            } else if (preview != null) {
                if (preview.straight) {
                    if (preview.points.size() == 1) preview.points.add(point);
                    else preview.points.set(1, point);
                } else if (preview.points.size() < 4000) {
                    preview.points.add(point);
                }
                invalidate();
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (action == MotionEvent.ACTION_UP && index >= 0) {
                PointF point = normalized(event.getX(index), event.getY(index));
                if (erasing) {
                    moveEraser(point);
                } else if (preview != null) {
                    if (preview.straight) {
                        if (preview.points.size() == 1) preview.points.add(point);
                        else preview.points.set(1, point);
                    } else if (preview.points.size() < 4000) {
                        preview.points.add(point);
                    }
                }
            }
            finish();
            return true;
        }
        return true;
    }
}
