package com.example.gravitydemo;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class GravityView extends View {

    private static final float G = 9000f;
    private static final float FIXED_DT = 1f / 60f;
    private static final long FRAME_MS = 16L;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Body> bodies = new ArrayList<Body>(3);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final GestureDetector gestureDetector;

    private boolean initialized;
    private boolean running;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            step(FIXED_DT);
            invalidate();
            handler.postDelayed(this, FRAME_MS);
        }
    };

    public GravityView(Context context) {
        super(context);
        paint.setStyle(Paint.Style.FILL);
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        handler.post(tick);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(tick);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initializeBodies(w, h);
        initialized = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.rgb(17, 17, 17));

        if (!initialized) {
            return;
        }

        for (Body body : bodies) {
            paint.setColor(body.color);
            canvas.drawCircle(body.x, body.y, body.radius, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private void initializeBodies(int width, int height) {
        bodies.clear();

        Body a = new Body(1800f, 30f, Color.parseColor("#4CAF50"));
        Body b = new Body(1800f, 30f, Color.parseColor("#2196F3"));
        Body c = new Body(160f, 14f, Color.parseColor("#FFC107"));

        float cx = width * 0.5f;
        float cy = height * 0.5f;

        float distance = Math.min(width, height) * 0.22f;

        a.x = cx - distance;
        a.y = cy;

        b.x = cx + distance;
        b.y = cy;

        c.x = cx;
        c.y = cy - distance * 0.7f;

        // 让两个大天体以相反方向绕行。
        float orbitalSpeed = 0.75f * (float) Math.sqrt((G * b.mass) / ((2f * distance) + 1f));
        a.vx = 0f;
        a.vy = orbitalSpeed;
        b.vx = 0f;
        b.vy = -orbitalSpeed;

        // 通过小天体速度补偿，保证总动量初始为零。
        float px = a.mass * a.vx + b.mass * b.vx;
        float py = a.mass * a.vy + b.mass * b.vy;
        c.vx = -px / c.mass;
        c.vy = -py / c.mass;

        bodies.add(a);
        bodies.add(b);
        bodies.add(c);

        centerMassAt(cx, cy);
        zeroTotalMomentum();
    }

    private void centerMassAt(float targetX, float targetY) {
        float totalMass = 0f;
        float weightedX = 0f;
        float weightedY = 0f;

        for (Body body : bodies) {
            totalMass += body.mass;
            weightedX += body.mass * body.x;
            weightedY += body.mass * body.y;
        }

        if (totalMass <= 0f) {
            return;
        }

        float cmX = weightedX / totalMass;
        float cmY = weightedY / totalMass;
        float dx = targetX - cmX;
        float dy = targetY - cmY;

        for (Body body : bodies) {
            body.x += dx;
            body.y += dy;
        }
    }

    private void zeroTotalMomentum() {
        float totalMass = 0f;
        float totalPx = 0f;
        float totalPy = 0f;

        for (Body body : bodies) {
            totalMass += body.mass;
            totalPx += body.mass * body.vx;
            totalPy += body.mass * body.vy;
        }

        if (totalMass <= 0f) {
            return;
        }

        float offsetVx = totalPx / totalMass;
        float offsetVy = totalPy / totalMass;

        for (Body body : bodies) {
            body.vx -= offsetVx;
            body.vy -= offsetVy;
        }
    }

    private void step(float dt) {
        final int n = bodies.size();

        float[] ax = new float[n];
        float[] ay = new float[n];

        for (int i = 0; i < n; i++) {
            Body bi = bodies.get(i);
            for (int j = i + 1; j < n; j++) {
                Body bj = bodies.get(j);
                float dx = bj.x - bi.x;
                float dy = bj.y - bi.y;
                float dist2 = dx * dx + dy * dy + 40f;
                float dist = (float) Math.sqrt(dist2);
                float invDist3 = 1f / (dist2 * dist);
                float factor = G * invDist3;

                ax[i] += factor * bj.mass * dx;
                ay[i] += factor * bj.mass * dy;
                ax[j] -= factor * bi.mass * dx;
                ay[j] -= factor * bi.mass * dy;
            }
        }

        for (int i = 0; i < n; i++) {
            Body body = bodies.get(i);
            body.vx += ax[i] * dt;
            body.vy += ay[i] * dt;
            body.x += body.vx * dt;
            body.y += body.vy * dt;
        }

        handleCollisions();
    }

    private void handleCollisions() {
        int n = bodies.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Body a = bodies.get(i);
                Body b = bodies.get(j);

                float dx = b.x - a.x;
                float dy = b.y - a.y;
                float dist2 = dx * dx + dy * dy;
                float minDist = a.radius + b.radius;

                if (dist2 >= minDist * minDist) {
                    continue;
                }

                float dist = (float) Math.sqrt(dist2);
                float nx;
                float ny;

                if (dist < 1e-4f) {
                    nx = 1f;
                    ny = 0f;
                    dist = 1e-4f;
                } else {
                    nx = dx / dist;
                    ny = dy / dist;
                }

                // 重置法向距离到刚好接触，避免持续重叠。
                float penetration = minDist - dist;
                float invMassA = 1f / a.mass;
                float invMassB = 1f / b.mass;
                float invMassSum = invMassA + invMassB;
                float moveA = penetration * (invMassA / invMassSum);
                float moveB = penetration * (invMassB / invMassSum);

                a.x -= nx * moveA;
                a.y -= ny * moveA;
                b.x += nx * moveB;
                b.y += ny * moveB;

                // 消除法向相对动量：碰撞后法向相对速度置为 0。
                float rvx = b.vx - a.vx;
                float rvy = b.vy - a.vy;
                float vn = rvx * nx + rvy * ny;

                if (vn < 0f) {
                    float impulse = -vn / invMassSum;
                    a.vx -= impulse * nx * invMassA;
                    a.vy -= impulse * ny * invMassA;
                    b.vx += impulse * nx * invMassB;
                    b.vy += impulse * ny * invMassB;
                }
            }
        }
    }

    private final class GestureListener implements GestureDetector.OnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            // no-op
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (bodies.size() < 3) {
                return false;
            }
            Body small = bodies.get(2);
            small.vx += -distanceX * 0.025f;
            small.vy += -distanceY * 0.025f;
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // no-op
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (bodies.size() < 3) {
                return false;
            }
            Body small = bodies.get(2);
            small.vx += velocityX * 0.0003f;
            small.vy += velocityY * 0.0003f;
            return true;
        }
    }

    private static final class Body {
        final float mass;
        final float radius;
        final int color;
        float x;
        float y;
        float vx;
        float vy;

        Body(float mass, float radius, int color) {
            this.mass = mass;
            this.radius = radius;
            this.color = color;
        }
    }
}
