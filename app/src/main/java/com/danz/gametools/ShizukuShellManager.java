package com.danz.gametools;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ShizukuShellManager {

    public static final String SHIZUKU_PKG = "moe.shizuku.privileged.api";
    private static boolean manualAuthorized = false;

    public static boolean isAndroid11OrAbove() {
        return Build.VERSION.SDK_INT >= 30;
    }

    public static boolean isShizukuInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(SHIZUKU_PKG, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isShizukuRunning(Context context) {
        if (!isShizukuInstalled(context)) return false;
        try {
            Bundle res = context.getContentResolver().call(
                Uri.parse("content://moe.shizuku.privileged.api.shizuku"),
                "pingBinder", null, null
            );
            return res != null || manualAuthorized;
        } catch (Exception e) {
            return manualAuthorized;
        }
    }

    public static boolean hasShizukuPermission(Context context) {
        if (!isShizukuInstalled(context)) return false;
        try {
            int check = context.checkSelfPermission("moe.shizuku.manager.permission.API_V23");
            return check == PackageManager.PERMISSION_GRANTED || manualAuthorized;
        } catch (Exception e) {
            return manualAuthorized;
        }
    }

    public static void grantManualAuthorization() {
        manualAuthorized = true;
    }

    public static String execAdbCommand(String cmd) {
        StringBuilder out = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "rish -c '" + cmd + "' 2>/dev/null || (" + cmd + ")"});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line).append("\n");
            }
            p.waitFor();
        } catch (Exception e) {
            return "";
        }
        return out.toString().trim();
    }

    public static void execAdbAsync(final String cmd) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                execAdbCommand(cmd);
            }
        }).start();
    }

    public static void applyDragHs(boolean enable, String pkg) {
        if (enable) {
            execAdbAsync(
                "settings put secure view_configuration_touch_slop 2; " +
                "settings put secure touch_blocking_period 0; " +
                "setprop debug.touch.drag_hs 1; " +
                "cmd game mode performance " + pkg
            );
        } else {
            execAdbAsync(
                "settings delete secure view_configuration_touch_slop; " +
                "setprop debug.touch.drag_hs 0"
            );
        }
    }

    public static void applyAimDrag(boolean enable, int level) {
        if (enable) {
            int speed = (level == 0) ? 3 : (level == 1 ? 5 : 7);
            int scale = (level == 0) ? 2 : (level == 1 ? 3 : 4);
            execAdbAsync(
                "settings put system pointer_speed " + speed + "; " +
                "setprop debug.touch.velocity_scale " + scale
            );
        } else {
            execAdbAsync("settings put system pointer_speed 0; setprop debug.touch.velocity_scale 1");
        }
    }

    public static void applyAimSmooth(boolean enable, int level) {
        if (enable) {
            int timeout = (level == 0) ? 300 : (level == 1 ? 250 : 200);
            execAdbAsync(
                "setprop debug.sf.early_phase_offset_ns 500000; " +
                "setprop debug.sf.early_gl_phase_offset_ns 500000; " +
                "settings put secure long_press_timeout " + timeout
            );
        } else {
            execAdbAsync("setprop debug.sf.early_phase_offset_ns 0; settings put secure long_press_timeout 400");
        }
    }

    public static void applyControlRecoil(boolean enable, int level) {
        if (enable) {
            int damp = (level == 0) ? 25 : (level == 1 ? 50 : 100);
            execAdbAsync(
                "settings put secure tap_duration_threshold 120; " +
                "settings put secure ignore_repeat_click_timeout 100; " +
                "setprop debug.touch.recoil_damp " + damp
            );
        } else {
            execAdbAsync("settings delete secure tap_duration_threshold; setprop debug.touch.recoil_damp 0");
        }
    }

    public static void applyAimAssist(boolean enable, int level) {
        if (enable) {
            int pSpeed = (level == 0) ? 4 : (level == 1 ? 6 : 7);
            execAdbAsync(
                "settings put system pointer_speed " + pSpeed + "; " +
                "settings put secure multi_press_timeout 150; " +
                "settings put secure long_press_timeout 180; " +
                "setprop debug.input.aimassist.safe 1; " +
                "setprop debug.touch.sampling_rate 240"
            );
        } else {
            execAdbAsync(
                "settings put system pointer_speed 0; " +
                "settings put secure multi_press_timeout 300; " +
                "setprop debug.input.aimassist.safe 0"
            );
        }
    }

    public static void applyGameSpaceRogPc(final boolean enable, final int level, final String pkg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (enable) {
                    String downscale = (level == 0) ? "0.80" : (level == 1 ? "0.70" : "0.60");
                    execAdbCommand(
                        "cmd game mode performance " + pkg + ";\n" +
                        "cmd game set --mode 2 --downscale " + downscale + " --fps 120 " + pkg + ";\n" +
                        "setprop debug.hwui.renderer skiagl;\n" +
                        "setprop debug.egl.hw 1;\n" +
                        "setprop debug.sf.hw 1;\n" +
                        "setprop debug.sf.latch_unsignaled 1;\n" +
                        "settings put global window_animation_scale 0.0;\n" +
                        "settings put global transition_animation_scale 0.0;\n" +
                        "settings put global animator_duration_scale 0.0;\n" +
                        "am kill-all;\n"
                    );
                } else {
                    execAdbCommand(
                        "cmd game mode standard " + pkg + ";\n" +
                        "cmd game set --mode 1 --downscale disable " + pkg + ";\n" +
                        "settings put global window_animation_scale 1.0;\n" +
                        "settings put global transition_animation_scale 1.0;\n" +
                        "settings put global animator_duration_scale 1.0;\n"
                    );
                }
            }
        }).start();
    }

    public static void applyDanzWarePerformance(final boolean enable, final int level, final String pkg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (enable) {
                    String downscale = (level == 0) ? "0.90" : (level == 1 ? "0.85" : "disabled");
                    execAdbCommand(
                        "cmd game mode performance " + pkg + ";\n" +
                        "cmd game set --mode 2 --downscale " + downscale + " --fps 120 " + pkg + ";\n" +
                        "cmd power set-fixed-performance-mode-enabled true;\n" +
                        "cmd thermalservice override-status 0;\n" +
                        "setprop debug.sf.latch_unsignaled 1;\n" +
                        "setprop debug.sf.disable_backpressure 1;\n" +
                        "setprop debug.hwui.renderer skiagl;\n" +
                        "setprop debug.egl.hw 1;\n" +
                        "setprop debug.performance.tuning 1;\n" +
                        "settings put global window_animation_scale 0.0;\n" +
                        "settings put global transition_animation_scale 0.0;\n" +
                        "settings put global animator_duration_scale 0.0;\n" +
                        "cmd package compile -m speed-profile -f " + pkg + ";\n" +
                        "am kill-all;\n" +
                        "pm trim-caches 4096M;\n"
                    );
                } else {
                    execAdbCommand(
                        "cmd game mode standard " + pkg + ";\n" +
                        "cmd power set-fixed-performance-mode-enabled false;\n" +
                        "cmd thermalservice reset;\n" +
                        "setprop debug.sf.latch_unsignaled 0;\n" +
                        "setprop debug.sf.disable_backpressure 0;\n" +
                        "settings put global window_animation_scale 1.0;\n" +
                        "settings put global transition_animation_scale 1.0;\n" +
                        "settings put global animator_duration_scale 1.0;\n"
                    );
                }
            }
        }).start();
    }
}
