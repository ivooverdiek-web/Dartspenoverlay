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

/**
 * In-game, touch-intercepting editor for ONE saved swipe. This view draws over
 * the board but never forwards or synthesizes gestures to the game.
 *
 * Coordinates stay normalized to the display; transforms run in real pixels
 * (important on portrait and foldable screens). Changes remain in memory
 * until the explicit Save action in the floating control panel.
 */
final class ThrowPathEditorView extends View {
    interface Listener { void onChanged(); }

    static final int WHOLE = 0;
    static final int START = 1;
    static final int END = 2;

    private final Listener listener;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayDeque<ArrayList<PointF>> undo = new ArrayDeque<>();
    private final float density;
    private final ArrayList<PointF> original;
    private ArrayList<PointF> points;
    private int mode = WHOLE;
    private int dragMode = -1;
    private int activePointer = -1;
    private float downX, downY;
    private ArrayList<PointF> dragStart;
    private boolean changedOnDrag;

    ThrowPathEditorView(Context context, List<PointF> captured, Listener listener) {
        super(context);
        this.listener = listener;
        density = getResources().getDisplayMetrics().density;
        original = copy(captured);
        points = copy(captured);
        setContentDescription("Edit saved swipe: drag yellow path or colored end handles; no throws are sent");
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private static ArrayList<PointF> copy(List<PointF> source) {
        ArrayList<PointF> result = new ArrayList<>();
        for (PointF p : source) result.add(new PointF(p.x, p.y));
        return result;
    }

    ArrayList<PointF> getPoints() { return copy(points); }

    void setMode(int value) {
        mode = Math.max(WHOLE, Math.min(END, value));
        invalidate();
    }

    int getMode() { return mode; }

    boolean reset() {
        if (equalsPoints(points, original)) return false;
        pushUndo();
        points = copy(original);
        invalidate();
        listener.onChanged();
        return true;
    }

    boolean undo() {
        if (undo.isEmpty()) return false;
        points = undo.removeLast();
        invalidate();
        listener.onChanged();
        return true;
    }

    void replaceWith(List<PointF> replacement) {
        if (replacement == null || replacement.size() != points.size()) return;
        if (!valid(replacement)) return;
        pushUndo();
        points = copy(replacement);
        invalidate();
        listener.onChanged();
    }

    boolean fineAdjust(float dxDp, float dyDp, float scale, float degrees) {
        if (points.size() < 2 || getWidth() == 0 || getHeight() == 0) return false;
        float dx = dxDp * density, dy = dyDp * density;
        ArrayList<PointF> proposed;
        if (scale != 1f || degrees != 0f) {
            proposed = transform(points, scale, degrees, dx, dy);
        } else {
            proposed = shift(points, dx, dy, mode);
        }
        if (!valid(proposed) || equalsPoints(proposed, points)) return false;
        pushUndo();
        points = proposed;
        invalidate();
        listener.onChanged();
        return true;
    }

    private ArrayList<PointF> shift(List<PointF> source, float dxPx, float dyPx, int which) {
        ArrayList<PointF> result = new ArrayList<>();
        float w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
        int n = source.size();
        for (int i = 0; i < n; i++) {
            PointF p = source.get(i);
            float t = n < 2 ? 0f : (float)i / (n - 1);
            float weight = which == START ? (1f-t) : which == END ? t : 1f;
            result.add(new PointF(p.x + (dxPx/w) * weight,
                    p.y + (dyPx/h) * weight));
        }
        return result;
    }

    private ArrayList<PointF> transform(List<PointF> source, float scale, float degrees,
                                         float dxPx, float dyPx) {
        float w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
        PointF first = source.get(0), last = source.get(source.size()-1);
        float cx = (first.x + last.x) * w * 0.5f;
        float cy = (first.y + last.y) * h * 0.5f;
        double r = Math.toRadians(degrees);
        float cos = (float)Math.cos(r), sin = (float)Math.sin(r);
        ArrayList<PointF> result = new ArrayList<>();
        for (PointF p : source) {
            float x = (p.x*w - cx) * scale;
            float y = (p.y*h - cy) * scale;
            result.add(new PointF((cx + x*cos-y*sin + dxPx)/w,
                    (cy + x*sin+y*cos + dyPx)/h));
        }
        return result;
    }

    private static boolean valid(List<PointF> proposed) {
        if (proposed.size() < 2 || proposed.size() > 200) return false;
        for (PointF p : proposed) {
            if (!Float.isFinite(p.x) || !Float.isFinite(p.y)
                    || p.x < 0f || p.x > 1f || p.y < 0f || p.y > 1f) return false;
        }
        return true;
    }

    private static boolean equalsPoints(List<PointF> a, List<PointF> b) {
        if (a.size() != b.size()) return false;
        for (int i=0;i<a.size();i++) {
            if (Math.abs(a.get(i).x - b.get(i).x) > 0.000001f
                    || Math.abs(a.get(i).y - b.get(i).y) > 0.000001f) return false;
        }
        return true;
    }

    private void pushUndo() {
        if (undo.size() >= 30) undo.removeFirst();
        undo.addLast(copy(points));
    }

    private void stroke(Canvas c, List<PointF> ps, int color, float width) {
        if (ps.size()<2) return;
        Path path = new Path();
        path.moveTo(ps.get(0).x*getWidth(), ps.get(0).y*getHeight());
        for (int i=1;i<ps.size();i++) {
            PointF p=ps.get(i);
            path.lineTo(p.x*getWidth(),p.y*getHeight());
        }
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width*density);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        c.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        // Tint the game slightly while preserving visible aiming references.
        c.drawColor(0x25000000);
        stroke(c, original, 0x88FFFFFF, 2.2f);
        stroke(c, points, 0xFFFACC15, 4.0f);
        if (points.size() < 2) return;
        PointF a=points.get(0), b=points.get(points.size()-1);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(mode == START ? Color.WHITE : 0xFF22D3EE);
        c.drawCircle(a.x*getWidth(),a.y*getHeight(),(mode==START?12f:9f)*density,paint);
        paint.setColor(mode == END ? Color.WHITE : 0xFFFB923C);
        c.drawCircle(b.x*getWidth(),b.y*getHeight(),(mode==END?12f:9f)*density,paint);
    }

    private float sqDist(float x, float y, PointF p) {
        float dx=x-p.x*getWidth(), dy=y-p.y*getHeight();
        return dx*dx+dy*dy;
    }

    private float distToPath(float x, float y) {
        float best=Float.MAX_VALUE;
        for(int i=1;i<points.size();i++) {
            PointF a=points.get(i-1),b=points.get(i);
            float ax=a.x*getWidth(), ay=a.y*getHeight();
            float bx=b.x*getWidth(), by=b.y*getHeight();
            float dx=bx-ax, dy=by-ay;
            float t=(dx*dx+dy*dy)<0.01f?0f:
                    Math.max(0f,Math.min(1f,((x-ax)*dx+(y-ay)*dy)/(dx*dx+dy*dy)));
            float ex=x-(ax+t*dx), ey=y-(ay+t*dy);
            best=Math.min(best,(float)Math.hypot(ex,ey));
        }
        return best;
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        // Always consume touches while editor is open: the game must never
        // receive finger movements as unplanned throws.
        if (points.size()<2) return true;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activePointer=e.getPointerId(0);
                downX=e.getX(); downY=e.getY();
                dragStart=copy(points);
                dragMode=-1;
                changedOnDrag=false;
                float threshold=30*density;
                PointF start=points.get(0), end=points.get(points.size()-1);
                if (sqDist(downX,downY,start) <= threshold*threshold) dragMode=START;
                else if (sqDist(downX,downY,end)<=threshold*threshold) dragMode=END;
                else if (distToPath(downX,downY)<=35*density) dragMode=WHOLE;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (activePointer<0 || dragMode<0 || dragStart==null) return true;
                int index=e.findPointerIndex(activePointer);
                if(index<0)return true;
                ArrayList<PointF> candidate=shift(dragStart,
                        e.getX(index)-downX,e.getY(index)-downY,dragMode);
                if (valid(candidate)) {
                    if (!changedOnDrag && !equalsPoints(candidate,points)) {
                        pushUndo();
                        changedOnDrag=true;
                    }
                    if (changedOnDrag) {
                        points=candidate;
                        invalidate();
                        listener.onChanged();
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activePointer=-1;
                dragMode=-1;
                dragStart=null;
                return true;
            default:
                return true;
        }
    }
}
