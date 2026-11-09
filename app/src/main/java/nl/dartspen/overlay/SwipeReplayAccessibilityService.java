package nl.dartspen.overlay;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Explicit, opt-in recorded swipe playback. Does not monitor another app's
 * input or edit live game touch events. Gesture replays are synthetic.
 *
 * v3.4 preserves variable finger speed using consecutive continued strokes:
 * each stroke represents a short time slice of the original gesture. Older
 * saved throws (without timestamps) keep the one-stroke uniform replay.
 */
public final class SwipeReplayAccessibilityService extends AccessibilityService {
    interface Result { void onComplete(boolean dispatched); }

    private static volatile SwipeReplayAccessibilityService instance;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final int MAX_TIMED_STROKES = 8;

    static boolean isAvailable() { return instance != null; }

    static boolean replay(List<PointF> points, float w, float h,
                          float originX, float originY, long elapsedMs,
                          float snapStrength, Result result) {
        return replay(points, null, w, h, originX, originY, elapsedMs,
                snapStrength, result);
    }

    static boolean replay(List<PointF> points, List<Long> pointTimes,
                          float w, float h, float originX, float originY,
                          long elapsedMs, float snapStrength, Result result) {
        SwipeReplayAccessibilityService service = instance;
        if (service == null || points == null || points.size() < 2 ||
                w <= 0f || h <= 0f || elapsedMs < 1L) {
            if (result != null) result.onComplete(false);
            return false;
        }
        try {
            final float[] xs = new float[points.size()];
            final float[] ys = new float[points.size()];
            // Angle snapping is disabled unconditionally. Old saved gestures
            // may contain a nonzero legacy snapStrength, but they now replay
            // their raw recorded path. Timestamped speed shaping is retained.
            for (int i = 0; i < points.size(); i++) {
                PointF p = points.get(i);
                xs[i] = originX + p.x*w;
                ys[i] = originY + p.y*h;
            }

            long duration = Math.max(25L,
                    Math.min(GestureDescription.getMaxGestureDuration(), elapsedMs));
            ArrayList<StrokeSegment> pieces = buildSegments(xs,ys,pointTimes,duration);
            if (pieces.isEmpty()) {
                if(result != null) result.onComplete(false);
                return false;
            }
            PlaybackSession session = new PlaybackSession(service,pieces,result);
            return session.start();
        } catch(RuntimeException e) {
            if (result != null) result.onComplete(false);
            return false;
        }
    }

    private static final class StrokeSegment {
        final Path path;
        final long duration;
        StrokeSegment(Path path, long duration) {
            this.path=path;
            this.duration=duration;
        }
    }

    private static ArrayList<StrokeSegment> buildSegments(float[] xs,float[] ys,
                                                           List<Long> times,long duration) {
        ArrayList<StrokeSegment> result=new ArrayList<>();
        boolean timed=times!=null && times.size()==xs.length && !times.isEmpty()
                && times.get(0)!=null && times.get(0)==0L;
        long last=0L;
        if (timed) {
            for (Long t : times) {
                if (t==null || t<last) { timed=false; break; }
                last=t;
            }
            if (last<1) timed=false;
        }
        if (!timed) {
            // Backwards compatibility: old T/D/S/Bull throws were stored with
            // only a total duration. Keep their original constant-speed replay.
            Path path=new Path();
            path.moveTo(xs[0],ys[0]);
            for(int i=1;i<xs.length;i++) path.lineTo(xs[i],ys[i]);
            result.add(new StrokeSegment(path,duration));
            return result;
        }
        int count=(int)Math.max(1, Math.min(MAX_TIMED_STROKES,duration/80L));
        if (count==1) {
            Path path=new Path();
            path.moveTo(xs[0],ys[0]);
            for(int i=1;i<xs.length;i++) path.lineTo(xs[i],ys[i]);
            result.add(new StrokeSegment(path,duration));
            return result;
        }

        // Time slices rather than distance slices: different distances
        // covered in equal intervals approximate slow starts, quick finishes
        // and stationary holds without turning the motion into separate taps.
        for(int part=0;part<count;part++) {
            long from=duration*part/count, to=duration*(part+1)/count;
            if(to<=from)continue;
            long sourceFrom=last*part/count, sourceTo=last*(part+1)/count;
            float[] start=atTime(xs,ys,times,sourceFrom);
            float[] end=atTime(xs,ys,times,sourceTo);
            Path path=new Path();
            path.moveTo(start[0],start[1]);
            for(int i=1;i<times.size()-1;i++) {
                long t=times.get(i);
                if(t>sourceFrom && t<sourceTo) path.lineTo(xs[i],ys[i]);
            }
            path.lineTo(end[0],end[1]);
            result.add(new StrokeSegment(path,to-from));
        }
        return result;
    }

    private static float[] atTime(float[] xs,float[] ys,List<Long> times,long t) {
        if(t<=0L)return new float[]{xs[0],ys[0]};
        int n=times.size();
        for(int i=1;i<n;i++) {
            long end=times.get(i);
            if(t<=end) {
                long begin=times.get(i-1);
                float mix=end<=begin?1f:(float)(t-begin)/(float)(end-begin);
                return new float[]{xs[i-1]+mix*(xs[i]-xs[i-1]),
                        ys[i-1]+mix*(ys[i]-ys[i-1])};
            }
        }
        return new float[]{xs[n-1],ys[n-1]};
    }

    private static final class PlaybackSession {
        final SwipeReplayAccessibilityService service;
        final ArrayList<StrokeSegment> segments;
        final Result result;
        final AtomicBoolean finished=new AtomicBoolean(false);
        GestureDescription.StrokeDescription previous;
        int index=0;

        PlaybackSession(SwipeReplayAccessibilityService service,
                        ArrayList<StrokeSegment> segments,Result result) {
            this.service=service;
            this.segments=segments;
            this.result=result;
        }

        boolean start() { return dispatchNext(); }

        private boolean dispatchNext() {
            if(finished.get())return false;
            if(index>=segments.size()) { finish(true); return true; }
            StrokeSegment segment=segments.get(index);
            boolean continues=index<segments.size()-1;
            try {
                // continueStroke keeps the *same* pointer held down between
                // successive dispatchGesture callbacks, rather than a sequence
                // of separate ACTION_DOWN / ACTION_UP taps.
                GestureDescription.StrokeDescription stroke = previous==null
                        ? new GestureDescription.StrokeDescription(
                                segment.path,0L,segment.duration,continues)
                        : previous.continueStroke(segment.path,0L,segment.duration,continues);
                GestureDescription gesture=new GestureDescription.Builder()
                        .addStroke(stroke).build();
                boolean accepted=service.dispatchGesture(gesture,
                        new GestureResultCallback() {
                            @Override public void onCompleted(GestureDescription description) {
                                if(finished.get())return;
                                previous=stroke;
                                index++;
                                if(index>=segments.size()) finish(true);
                                else dispatchNext();
                            }
                            @Override public void onCancelled(GestureDescription description) {
                                finish(false);
                            }
                        },MAIN);
                if(!accepted) finish(false);
                return accepted;
            } catch(RuntimeException ex) {
                finish(false);
                return false;
            }
        }

        private void finish(boolean success) {
            if(finished.compareAndSet(false,true) && result!=null)result.onComplete(success);
        }
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance=this;
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
    @Override public void onDestroy() {
        if(instance==this)instance=null;
        super.onDestroy();
    }
}
