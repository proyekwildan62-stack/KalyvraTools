package com.danz.gametools;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OverlayEngineService extends Service {

    private static OverlayEngineService instance;
    private WindowManager wm;
    private CustomCrosshairView crosshairView;
    private WindowManager.LayoutParams chParams;

    private LinearLayout rogBarContainer;
    private TextView rogTextView;
    private WindowManager.LayoutParams rogParams;
    private final boolean[] rogFlags = new boolean[]{true, true, true, true, true, true};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int frameCount = 0;
    private long lastSecondNs = 0;
    private int currentFps = 60;

    public static class CustomCrosshairView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        public int styleIndex = 0;
        public float sizePx = 58f;
        public float angleDeg = 80f;
        public int alpha = 255;
        public int color = Color.parseColor("#00E676");

        public CustomCrosshairView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = sizePx / 2f;

            paint.setColor(color);
            paint.setAlpha(alpha);
            paint.setStrokeWidth(4f);

            canvas.save();
            canvas.rotate(angleDeg, cx, cy);

            if (styleIndex == 0) {
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(cx - r, cy, cx + r, cy, paint);
                canvas.drawLine(cx, cy - r, cx, cy + r, paint);
                canvas.drawCircle(cx, cy, r * 0.45f, paint);
            } else if (styleIndex == 1) {
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(cx, cy, r * 0.7f, paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy, 5f, paint);
            } else if (styleIndex == 2) {
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(cx - r, cy, cx + r, cy, paint);
                canvas.drawLine(cx, cy - r, cx, cy + r, paint);
            } else {
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(cx, cy, r * 0.8f, paint);
                canvas.drawLine(cx - r, cy, cx + r, cy, paint);
                canvas.drawLine(cx, cy - r, cx, cy + r, paint);
            }
            canvas.restore();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        initCrosshairOverlay();
        initRogHudOverlay();
        startFpsAndRogLoop();
    }

    private int getOverlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void initCrosshairOverlay() {
        crosshairView = new CustomCrosshairView(this);
        crosshairView.setVisibility(View.GONE);
        chParams = new WindowManager.LayoutParams(
            180, 180, getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        chParams.gravity = Gravity.CENTER;
        chParams.x = 0;
        chParams.y = 0;
        try {
            wm.addView(crosshairView, chParams);
        } catch (Exception ignored) {
        }
    }

    private void initRogHudOverlay() {
        rogBarContainer = new LinearLayout(this);
        rogBarContainer.setOrientation(LinearLayout.HORIZONTAL);
        rogBarContainer.setPadding(24, 12, 24, 12);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#E60B110E"));
        bg.setCornerRadius(20f);
        bg.setStroke(2, Color.parseColor("#264D3B"));
        rogBarContainer.setBackground(bg);

        rogTextView = new TextView(this);
        rogTextView.setTextColor(Color.parseColor("#00E676"));
        rogTextView.setTextSize(11f);
        rogTextView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        rogBarContainer.addView(rogTextView);
        rogBarContainer.setVisibility(View.GONE);

        rogParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        rogParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        rogParams.y = 40;

        rogBarContainer.setOnTouchListener(new View.OnTouchListener() {
            int ix, iy;
            float tx, ty;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ix = rogParams.x;
                    iy = rogParams.y;
                    tx = event.getRawX();
                    ty = event.getRawY();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    rogParams.x = ix + (int) (event.getRawX() - tx);
                    rogParams.y = iy + (int) (event.getRawY() - ty);
                    try {
                        wm.updateViewLayout(rogBarContainer, rogParams);
                    } catch (Exception ignored) {}
                    return true;
                }
                return false;
            }
        });

        try {
            wm.addView(rogBarContainer, rogParams);
        } catch (Exception ignored) {
        }
    }

    private void startFpsAndRogLoop() {
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                frameCount++;
                if (lastSecondNs == 0) lastSecondNs = frameTimeNanos;
                if (frameTimeNanos - lastSecondNs >= 1000000000L) {
                    currentFps = frameCount;
                    frameCount = 0;
                    lastSecondNs = frameTimeNanos;
                }
                if (instance != null) {
                    Choreographer.getInstance().postFrameCallback(this);
                }
            }
        });

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (instance != null) {
                    refreshRogText();
                    handler.postDelayed(this, 1000L);
                }
            }
        }, 1000L);
    }

    private void refreshRogText() {
        if (rogBarContainer == null || rogBarContainer.getVisibility() != View.VISIBLE) return;
        StringBuilder sb = new StringBuilder();
        try {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            int ramPct = (int) (((mi.totalMem - mi.availMem) * 100L) / mi.totalMem);

            Intent batt = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = batt != null ? batt.getIntExtra(BatteryManager.EXTRA_LEVEL, 100) : 100;
            int temp = batt != null ? batt.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 340) / 10 : 34;

            if (rogFlags[0]) sb.append("CPU:MAX  ");
            if (rogFlags[1]) sb.append("RAM:").append(ramPct).append("%  ");
            if (rogFlags[2]) sb.append("TEMP:").append(temp).append("°C  ");
            if (rogFlags[3]) sb.append("BAT:").append(level).append("%  ");
            if (rogFlags[4]) sb.append("FPS:").append(currentFps).append("  ");
            if (rogFlags[5]) sb.append(new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()));
        } catch (Exception ignored) {
        }
        rogTextView.setText(sb.toString().trim());
    }

    public static void setCrosshairVisible(boolean visible) {
        if (instance != null && instance.crosshairView != null) {
            instance.crosshairView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public static void setCrosshairStyle(int style) {
        if (instance != null && instance.crosshairView != null) {
            instance.crosshairView.styleIndex = style;
            instance.crosshairView.invalidate();
        }
    }

    public static void nudgeCrosshairPosition(int dx, int dy) {
        if (instance != null && instance.crosshairView != null) {
            instance.chParams.x += dx;
            instance.chParams.y += dy;
            try {
                instance.wm.updateViewLayout(instance.crosshairView, instance.chParams);
            } catch (Exception ignored) {}
        }
    }

    public static int getCrosshairX() {
        return instance != null && instance.chParams != null ? instance.chParams.x : 0;
    }

    public static int getCrosshairY() {
        return instance != null && instance.chParams != null ? instance.chParams.y : 0;
    }

    public static void setCrosshairAngle(float angle) {
        if (instance != null && instance.crosshairView != null) {
            instance.crosshairView.angleDeg = angle;
            instance.crosshairView.invalidate();
        }
    }

    public static void setCrosshairSize(float size) {
        if (instance != null && instance.crosshairView != null) {
            instance.crosshairView.sizePx = size;
            instance.crosshairView.invalidate();
        }
    }

    public static void setCrosshairAlpha(int alpha) {
        if (instance != null && instance.crosshairView != null) {
            instance.crosshairView.alpha = alpha;
            instance.crosshairView.invalidate();
        }
    }

    public static void setCrosshairColor(int color) {
        if (instance != null && instance.crosshairView != null) {
            instance.crosshairView.color = color;
            instance.crosshairView.invalidate();
        }
    }

    public static void setRogVisible(boolean visible) {
        if (instance != null && instance.rogBarContainer != null) {
            instance.rogBarContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
            instance.refreshRogText();
        }
    }

    public static void setRogMetric(int idx, boolean enabled) {
        if (instance != null && idx >= 0 && idx < instance.rogFlags.length) {
            instance.rogFlags[idx] = enabled;
            instance.refreshRogText();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (crosshairView != null) wm.removeView(crosshairView);
            if (rogBarContainer != null) wm.removeView(rogBarContainer);
        } catch (Exception ignored) {}
        instance = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
