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

    private final StrokeStore store;
    private final Tools tools;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<StrokeStore.Stroke> strokes = new ArrayList<>();
    private final ArrayDeque<ArrayList<StrokeStore.Stroke>> history = new ArrayDeque<>();
    private StrokeStore.Stroke preview;
    private String profile = "";
    private String target = "Vrij";
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

    String profile() { return profile; }
    String target() { return target; }
    String layerProfile() { return StrokeStore.layerProfile(profile, target); }


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
        finish();
        target = chosen;
        store.selectedTarget(profile, target);
        strokes.clear();
        strokes.addAll(store.load(layerProfile()));
        history.clear();
        selectedLine = moveMode && !strokes.isEmpty() ? strokes.size() - 1 : -1;
        clearMoveGesture();
        preview = null;
        erasing = false;
        eraserPosition = null;
        pointerId = -1;
        invalidate();
    }

    void setEditable(boolean value) {
        if (!value) { finish(); clearMoveGesture(); }
        editable = value;
        invalidate();
    }

    private ArrayList<StrokeStore.Stroke> copyLines() {
        ArrayList<StrokeStore.Stroke> copy = new ArrayList<>();
        for (StrokeStore.Stroke original : strokes) {
            StrokeStore.Stroke line = new StrokeStore.Stroke(
                    original.color, original.widthDp, original.straight);
            line.arrow = original.arrow;
            for (PointF point : original.points) line.points.add(new PointF(point.x, point.y));
            copy.add(line);
        }
        return copy;
    }

    private void saveUndo() {
        history.push(copyLines());
        while (history.size() > 12) history.removeLast();
    }

    void undo() {
        finish();
        if (history.isEmpty()) return;
        strokes.clear();
        strokes.addAll(history.pop());
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
            if (!profile.isEmpty()) finish();
            profile = next;
            target = store.selectedTarget(profile);
            strokes.clear();
            strokes.addAll(store.load(layerProfile()));
            history.clear();
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
        if (preview != null) render(c, preview);
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
        if (line.points.isEmpty()) return;
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
        return new StrokeStore.Stroke(original.color, original.widthDp, false);
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
        if (!editable || profile.isEmpty()) return false;
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
