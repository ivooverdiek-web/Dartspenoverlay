package nl.dartspen.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

final class LineCanvasView extends View {
    interface Tools { int color(); float widthDp(); boolean straight(); }
    private final StrokeStore store;
    private final Tools tools;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<StrokeStore.Stroke> strokes = new ArrayList<>();
    private StrokeStore.Stroke preview;
    private String profile = "";
    private boolean editable = true;
    private int pointerId = -1;
    private final float density;

    LineCanvasView(Context context, StrokeStore store, Tools tools) {
        super(context);
        this.store = store;
        this.tools = tools;
        this.density = getResources().getDisplayMetrics().density;
        setBackgroundColor(Color.TRANSPARENT);
    }

    String profile() { return profile; }

    void setEditable(boolean value) {
        if (!value) finish();
        editable = value;
        invalidate();
    }

    void undo() {
        if (strokes.isEmpty()) return;
        strokes.remove(strokes.size() - 1);
        store.save(profile, strokes);
        invalidate();
    }

    int countLines() { return strokes.size(); }

    void clear() {
        preview = null;
        strokes.clear();
        store.save(profile, strokes);
        invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        if (w <= 0 || h <= 0) return;
        float aspect = (float)Math.max(w, h) / Math.min(w, h);
        String next = (aspect < 1.5f ? "inner" : "cover") +
                (w > h ? "_landscape" : "_portrait");
        if (!next.equals(profile)) {
            if (preview != null && !profile.isEmpty()) finish();
            profile = next;
            strokes.clear();
            strokes.addAll(store.load(profile));
            preview = null;
            pointerId = -1;
        }
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (StrokeStore.Stroke stroke : strokes) render(canvas, stroke);
        if (preview != null) render(canvas, preview);
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
            return;
        }
        if (stroke.straight) {
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
    }

    private PointF normalized(float x, float y) {
        return new PointF(Math.max(0, Math.min(1, x / Math.max(1, getWidth()))),
                Math.max(0, Math.min(1, y / Math.max(1, getHeight()))));
    }

    private void finish() {
        if (preview != null && !preview.points.isEmpty() && !profile.isEmpty()) {
            strokes.add(preview);
            if (strokes.size() > 500) strokes.remove(0);
            store.save(profile, strokes);
        }
        preview = null;
        pointerId = -1;
        invalidate();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!editable || profile.isEmpty()) return false;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            preview = new StrokeStore.Stroke(tools.color(), tools.widthDp(), tools.straight());
            pointerId = event.getPointerId(0);
            preview.points.add(normalized(event.getX(), event.getY()));
            invalidate();
            return true;
        }
        if (preview == null) return true;
        if (action == MotionEvent.ACTION_MOVE) {
            int index = event.findPointerIndex(pointerId);
            if (index < 0) return true;
            PointF point = normalized(event.getX(index), event.getY(index));
            if (preview.straight) {
                if (preview.points.size() == 1) preview.points.add(point);
                else preview.points.set(1, point);
            } else if (preview.points.size() < 4000) {
                preview.points.add(point);
            }
            invalidate();
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (action == MotionEvent.ACTION_UP) {
                PointF point = normalized(event.getX(), event.getY());
                if (preview.straight) {
                    if (preview.points.size() == 1) preview.points.add(point);
                    else preview.points.set(1, point);
                } else if (preview.points.size() < 4000) {
                    preview.points.add(point);
                }
            }
            finish();
            return true;
        }
        return true;
    }
}
