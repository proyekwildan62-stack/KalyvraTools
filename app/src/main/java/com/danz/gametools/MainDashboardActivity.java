package com.danz.gametools;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.StatFs;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.Locale;

public class MainDashboardActivity extends Activity {

    private LicenseManager licenseManager;
    private String userRole = "DEVELOPER";
    private String activeKey = "DANZ";
    private String userHwid = "";
    private long expiresAt = 0L;

    private String targetPackage = "com.dts.freefireth";
    private int aimDragLevel = 1;
    private int aimSmoothLevel = 1;
    private int recoilLevel = 1;
    private int aimAssistLevel = 1;
    private int gameSpaceRogLevel = 1;
    private int danzWareLevel = 2;

    private TextView headerTitleTv;
    private TextView uptimeClockTv;
    private LinearLayout pageProfile;
    private LinearLayout pageQuickLaunch;
    private LinearLayout pageGameTools;
    private Button navBtnProfile;
    private Button navBtnBolt;
    private Button navBtnTools;
    private FrameLayout lightningOverlay;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long startTimeMs = 0L;

    public interface SliderListener {
        void onChanged(int progress);
    }

    public interface ToggleAction {
        void onToggle(boolean checked);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        licenseManager = new LicenseManager(this);
        startTimeMs = System.currentTimeMillis();

        if (getIntent() != null) {
            String r = getIntent().getStringExtra("ROLE");
            String k = getIntent().getStringExtra("KEY");
            String h = getIntent().getStringExtra("HWID");
            if (r != null) userRole = r;
            if (k != null) activeKey = k;
            if (h != null) userHwid = h;
            expiresAt = getIntent().getLongExtra("EXPIRES", System.currentTimeMillis() + 315360000000L);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent ov = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivity(ov);
        }
        startService(new Intent(this, OverlayEngineService.class));

        buildVideoExactUi();
        startUptimeTicker();
    }

    private void buildVideoExactUi() {
        FrameLayout rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(Color.parseColor("#070D0A"));

        LinearLayout mainVertical = new LinearLayout(this);
        mainVertical.setOrientation(LinearLayout.VERTICAL);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(36, 36, 36, 24);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleColLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleCol.setLayoutParams(titleColLp);

        headerTitleTv = new TextView(this);
        headerTitleTv.setText("Profile");
        headerTitleTv.setTextColor(Color.WHITE);
        headerTitleTv.setTextSize(22f);
        headerTitleTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleCol.addView(headerTitleTv);

        TextView subHeader = new TextView(this);
        subHeader.setText("By DanzOfficial • KALYVRA TOOLS");
        subHeader.setTextColor(Color.parseColor("#7C9488"));
        subHeader.setTextSize(11f);
        titleCol.addView(subHeader);
        topBar.addView(titleCol);

        TextView shizChip = new TextView(this);
        shizChip.setText("SHIZUKU");
        shizChip.setTextColor(Color.parseColor("#00E676"));
        shizChip.setTextSize(11f);
        shizChip.setTypeface(Typeface.DEFAULT_BOLD);
        shizChip.setPadding(24, 10, 24, 10);
        GradientDrawable shizBg = new GradientDrawable();
        shizBg.setColor(Color.parseColor("#112E20"));
        shizBg.setCornerRadius(30f);
        shizBg.setStroke(2, Color.parseColor("#00E676"));
        shizChip.setBackground(shizBg);
        topBar.addView(shizChip);

        mainVertical.addView(topBar);

        ScrollView scrollArea = new ScrollView(this);
        scrollArea.setFillViewport(true);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        );
        scrollArea.setLayoutParams(scrollLp);

        FrameLayout pageContainer = new FrameLayout(this);
        pageContainer.setPadding(32, 8, 32, 32);

        pageProfile = new LinearLayout(this);
        pageProfile.setOrientation(LinearLayout.VERTICAL);
        buildProfilePage();

        pageQuickLaunch = new LinearLayout(this);
        pageQuickLaunch.setOrientation(LinearLayout.VERTICAL);
        pageQuickLaunch.setVisibility(View.GONE);
        buildQuickLaunchAndKeygenPage();

        pageGameTools = new LinearLayout(this);
        pageGameTools.setOrientation(LinearLayout.VERTICAL);
        pageGameTools.setVisibility(View.GONE);
        buildGameToolsPage();

        pageContainer.addView(pageProfile);
        pageContainer.addView(pageQuickLaunch);
        pageContainer.addView(pageGameTools);
        scrollArea.addView(pageContainer);
        mainVertical.addView(scrollArea);

        LinearLayout bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding(24, 16, 24, 16);
        GradientDrawable navBg = new GradientDrawable();
        navBg.setColor(Color.parseColor("#101D17"));
        navBg.setStroke(2, Color.parseColor("#1F382C"));
        bottomNav.setBackground(navBg);

        navBtnProfile = makeNavTabButton("⊞  Profile", true);
        navBtnBolt = makeNavTabButton("⚡", false);
        navBtnTools = makeNavTabButton("🛠️  Game Tools", false);

        navBtnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchPage(0);
            }
        });
        navBtnBolt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchPage(1);
            }
        });
        navBtnTools.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchPage(2);
            }
        });

        bottomNav.addView(navBtnProfile, new LinearLayout.LayoutParams(0, 110, 1f));
        bottomNav.addView(navBtnBolt, new LinearLayout.LayoutParams(0, 110, 0.7f));
        bottomNav.addView(navBtnTools, new LinearLayout.LayoutParams(0, 110, 1f));
        mainVertical.addView(bottomNav);

        rootFrame.addView(mainVertical);

        lightningOverlay = new FrameLayout(this);
        lightningOverlay.setBackgroundColor(Color.parseColor("#3300E676"));
        lightningOverlay.setVisibility(View.GONE);
        rootFrame.addView(lightningOverlay);

        setContentView(rootFrame);
    }

    private void switchPage(int idx) {
        pageProfile.setVisibility(idx == 0 ? View.VISIBLE : View.GONE);
        pageQuickLaunch.setVisibility(idx == 1 ? View.VISIBLE : View.GONE);
        pageGameTools.setVisibility(idx == 2 ? View.VISIBLE : View.GONE);

        if (idx == 0) headerTitleTv.setText("Profile");
        else if (idx == 1) headerTitleTv.setText("KALYVRA TOOLS");
        else headerTitleTv.setText("GameTools");

        styleNavTab(navBtnProfile, idx == 0);
        styleNavTab(navBtnBolt, idx == 1);
        styleNavTab(navBtnTools, idx == 2);
    }

    private Button makeNavTabButton(String text, boolean active) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13f);
        styleNavTab(b, active);
        return b;
    }

    private void styleNavTab(Button b, boolean active) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(22f);
        if (active) {
            bg.setColor(Color.parseColor("#183D2B"));
            bg.setStroke(2, Color.parseColor("#00E676"));
            b.setTextColor(Color.parseColor("#00E676"));
            b.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            bg.setColor(Color.TRANSPARENT);
            b.setTextColor(Color.parseColor("#8CA396"));
            b.setTypeface(Typeface.DEFAULT);
        }
        b.setBackground(bg);
    }

    private void playGreenLightningAnimation() {
        if (lightningOverlay == null) return;
        lightningOverlay.setVisibility(View.VISIBLE);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                lightningOverlay.setVisibility(View.GONE);
            }
        }, 180L);
    }

    private void buildProfilePage() {
        LinearLayout envCard = makeVideoCard("#163828", "#00E676");
        envCard.setOrientation(LinearLayout.HORIZONTAL);
        envCard.setGravity(Gravity.CENTER_VERTICAL);

        TextView scoreCircle = makeLabel("48", 22f, "#00E676", true);
        scoreCircle.setGravity(Gravity.CENTER);
        GradientDrawable scBg = new GradientDrawable();
        scBg.setShape(GradientDrawable.OVAL);
        scBg.setColor(Color.parseColor("#10291D"));
        scBg.setStroke(4, Color.parseColor("#00E676"));
        scoreCircle.setBackground(scBg);
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(130, 130);
        scLp.rightMargin = 28;
        scoreCircle.setLayoutParams(scLp);
        envCard.addView(scoreCircle);

        LinearLayout envTextCol = new LinearLayout(this);
        envTextCol.setOrientation(LinearLayout.VERTICAL);
        envTextCol.addView(makeLabel("Skor Lingkungan", 17f, "#FFFFFF", true));
        envTextCol.addView(makeLabel("Indeks Kesehatan & Stabilitas Sistem", 12f, "#A5C4B4", false));
        envCard.addView(envTextCol);
        pageProfile.addView(envCard);

        LinearLayout profCard = makeVideoCard("#0E1914", "#1B3026");
        profCard.addView(makeLabel("Profile Pengguna", 16f, "#FFFFFF", true));
        profCard.addView(makeLabel("ID : 192\nHWID : " + (userHwid.isEmpty() ? licenseManager.getDeviceHwid() : userHwid), 12f, "#7E968A", false));
        profCard.addView(makeLabel("Verified Access  ✔", 13f, "#00E676", true));

        LinearLayout ancareBox = makeVideoCard("#1B4D35", "#2B6E4E");
        ancareBox.addView(makeLabel("Ancare • KALYVRA VIP", 15f, "#A5D6BE", true));
        ancareBox.addView(makeLabel("Key Aktif: " + activeKey + " (" + userRole + ")", 12f, "#FFFFFF", false));
        ancareBox.addView(makeLabel("Berlaku sampai: " + LicenseManager.formatTimestamp(expiresAt), 12f, "#D0EFE0", false));

        boolean isDevOrReseller = "DEVELOPER".equals(userRole) || "RESELLER".equals(userRole);
        Button btnRenew = makeGreenBtn(isDevOrReseller ? "CREATE KEY" : "PERBARUI");
        btnRenew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchPage(1);
            }
        });
        ancareBox.addView(btnRenew);
        profCard.addView(ancareBox);

        Button btnBuyVipChat = makeGreenBtn("💬 BUY VIP CHAT 085602595546");
        LinearLayout.LayoutParams vipLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        vipLp.topMargin = 16;
        btnBuyVipChat.setLayoutParams(vipLp);
        btnBuyVipChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playGreenLightningAnimation();
                String url = "https://api.whatsapp.com/send?phone=6285602595546&text=" +
                             Uri.encode("Halo Admin, saya mau BUY VIP KALYVRA TOOLS.");
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(i);
            }
        });
        profCard.addView(btnBuyVipChat);
        pageProfile.addView(profCard);

        LinearLayout srvCard = makeVideoCard("#10241B", "#1E4231");
        srvCard.addView(makeLabel("Layanan Pengguna", 15f, "#B0C9BC", true));
        srvCard.addView(makeLabel("● Online", 13f, "#00E676", true));

        uptimeClockTv = makeLabel("00:00:05 Waktu Aktif", 14f, "#FFFFFF", true);
        srvCard.addView(uptimeClockTv);
        srvCard.addView(makeLabel("PID   " + Process.myPid() + "      UID   2000      VER   1.0.0(1)", 11f, "#A0B8AC", false));
        pageProfile.addView(srvCard);

        LinearLayout sysCard = makeVideoCard("#0E1914", "#1B3026");
        sysCard.addView(makeLabel("Informasi Sistem                                 Version 1.0.0", 13f, "#FFFFFF", true));

        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        int ramPct = (int) (((mi.totalMem - mi.availMem) * 100L) / mi.totalMem);
        double totalGb = mi.totalMem / (1024.0 * 1024.0 * 1024.0);

        File dataDir = Environment.getDataDirectory();
        StatFs stat = new StatFs(dataDir.getPath());
        long totalBytes = stat.getTotalBytes();
        long availBytes = stat.getAvailableBytes();
        int storagePct = (int) (((totalBytes - availBytes) * 100L) / totalBytes);
        long storageGb = totalBytes / (1024L * 1024L * 1024L);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(makeSysTile("RAM Usage", ramPct + "%", String.format(Locale.US, "Total %.1f GB", totalGb)), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row1.addView(makeSysTile("Storage", storagePct + "%", "Total " + storageGb + " GB"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sysCard.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(makeSysTile("CPU Speed", "2.00 GHz", "Max 2.00 GHz"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row2.addView(makeSysTile("Network", "- ms", "8.8.8.8"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sysCard.addView(row2);

        sysCard.addView(makeLabel(Build.MANUFACTURER + " " + Build.MODEL + " - Android " + Build.VERSION.RELEASE + " - " + Build.BOARD, 11f, "#7C9488", false));
        pageProfile.addView(sysCard);

        LinearLayout chanCard = makeVideoCard("#0E1914", "#1B3026");
        chanCard.addView(makeLabel("Gabung ke Saluran Resmi", 15f, "#FFFFFF", true));
        chanCard.addView(makeLabel("Bergabung ke saluran resmi untuk memantau update, info penting, dan pengumuman resmi secara langsung", 12f, "#7C9488", false));
        pageProfile.addView(chanCard);
    }

    private void buildQuickLaunchAndKeygenPage() {
        LinearLayout launchCard = makeVideoCard("#122B1F", "#00E676");
        launchCard.addView(makeLabel("⚡ PELUNCUR CEPAT IN-GAME FREE FIRE", 16f, "#00E676", true));
        launchCard.addView(makeLabel("Aktifkan fitur di tab Game Tools terlebih dahulu, lalu luncurkan Free Fire dari tombol di bawah agar profil Shizuku bekerja penuh di dalam game.", 12f, "#B0C9BC", false));

        Button btnLaunchFf = makeGreenBtn("BUKA FREE FIRE (" + targetPackage + ")");
        btnLaunchFf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playGreenLightningAnimation();
                Intent launch = getPackageManager().getLaunchIntentForPackage(targetPackage);
                if (launch != null) {
                    startActivity(launch);
                } else {
                    Toast.makeText(MainDashboardActivity.this, "Paket " + targetPackage + " belum terinstal di HP ini!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        launchCard.addView(btnLaunchFf);
        pageQuickLaunch.addView(launchCard);

        LinearLayout devCard = makeVideoCard("#0E1914", "#1B3026");
        devCard.addView(makeLabel("🔑 PANEL DEVELOPER — CREATE KEY (1 KEY 1 DEVICE)", 15f, "#00E676", true));
        devCard.addView(makeLabel("Pilih durasi key untuk pelanggan (Otomatis mengunci HWID perangkat pertama yang login):", 12f, "#8CA396", false));

        final Spinner tierSpinner = new Spinner(this);
        String[] tiers = new String[]{
            "1 Day (1Key 1Device)", "3 Day (1Key 1Device)", "7 Day (1Key 1Device)",
            "15 Day (1Key 1Device)", "30 Day (1Key 1Device)", "Permanent (1Key 1Device)", "Reseller (1Key 1Device)"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, tiers);
        tierSpinner.setAdapter(adapter);
        GradientDrawable spBg = new GradientDrawable();
        spBg.setColor(Color.parseColor("#183326"));
        spBg.setCornerRadius(14f);
        tierSpinner.setBackground(spBg);
        devCard.addView(tierSpinner);

        final EditText etCustomHwid = new EditText(this);
        etCustomHwid.setHint("Opsional: Isi HWID user (Kosongkan = Auto Lock saat login)");
        etCustomHwid.setHintTextColor(Color.parseColor("#5C786A"));
        etCustomHwid.setTextColor(Color.WHITE);
        devCard.addView(etCustomHwid);

        final TextView tvGeneratedKey = makeLabel("Key hasil generate akan muncul di sini...", 13f, "#00E676", true);
        devCard.addView(tvGeneratedKey);

        final TextView tvDbSummary = makeLabel(licenseManager.dumpAllKeysSummary(), 11f, "#A5C4B4", false);

        Button btnCreateKey = makeGreenBtn("CREATE KEY & SALIN OTOMATIS");
        btnCreateKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playGreenLightningAnimation();
                int pos = tierSpinner.getSelectedItemPosition();
                LicenseManager.Tier selected = LicenseManager.Tier.values()[pos];
                String newKey = licenseManager.generateLicenseKey(selected, etCustomHwid.getText().toString());
                tvGeneratedKey.setText("KEY BERHASIL DIBUAT:\n" + newKey + " (" + selected.label + ")");
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("KEY", newKey));
                tvDbSummary.setText(licenseManager.dumpAllKeysSummary());
                Toast.makeText(MainDashboardActivity.this, "Key " + newKey + " disalin ke Clipboard!", Toast.LENGTH_SHORT).show();
            }
        });
        devCard.addView(btnCreateKey);

        final EditText etResetKey = new EditText(this);
        etResetKey.setHint("Ketik Key untuk Reset HWID");
        etResetKey.setHintTextColor(Color.parseColor("#5C786A"));
        etResetKey.setTextColor(Color.WHITE);
        devCard.addView(etResetKey);

        Button btnResetHwid = makeDarkBtn("RESET HWID KEY");
        btnResetHwid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean ok = licenseManager.resetHwidForKey(etResetKey.getText().toString());
                tvDbSummary.setText(licenseManager.dumpAllKeysSummary());
                Toast.makeText(MainDashboardActivity.this, ok ? "HWID di-reset!" : "Key tidak ditemukan", Toast.LENGTH_SHORT).show();
            }
        });
        devCard.addView(btnResetHwid);

        devCard.addView(makeLabel("\nDatabase Key Aktif:", 13f, "#FFFFFF", true));
        devCard.addView(tvDbSummary);
        pageQuickLaunch.addView(devCard);
    }

    private void buildGameToolsPage() {
        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setPadding(0, 0, 0, 16);
        chipRow.addView(makeChip("✔ Semua", true));
        chipRow.addView(makeChip("👁️ Visual", false));
        chipRow.addView(makeChip("⚙ Konfigurasi", false));
        pageGameTools.addView(chipRow);

        pageGameTools.addView(makeExpandableFeatureCard(
            "🎯", "DRAG HS", "VIP",
            "Kalibrasi sensitivitas vertikal & touch slop agar tarikan tembakan langsung naik ke kepala di dalam game (100% Safe No-Root).",
            null,
            new ToggleAction() {
                @Override
                public void onToggle(boolean checked) {
                    ShizukuShellManager.applyDragHs(checked, targetPackage);
                }
            }
        ));

        View bodyAimDrag = makeVideoSlider("Kecepatan Tarikan Y-Axis [VIP]", "Rendah", "Sedang", "Tertinggi", 2, 1, new SliderListener() {
            @Override
            public void onChanged(int progress) {
                aimDragLevel = progress;
                ShizukuShellManager.applyAimDrag(true, aimDragLevel);
            }
        });
        pageGameTools.addView(makeExpandableFeatureCard(
            "⚡", "AIMDRAG", "VIP",
            "Meningkatkan akselerasi swipe layar ke atas agar drag lebih ringan dan responsif (Safe No-Root).",
            bodyAimDrag,
            new ToggleAction() {
                @Override
                public void onToggle(boolean checked) {
                    ShizukuShellManager.applyAimDrag(checked, aimDragLevel);
                }
            }
        ));

        View bodyAimSmooth = makeVideoSlider("Tingkat Kehalusan Bidikan [VIP]", "Cepat", "Stabil", "Halus", 2, 1, new SliderListener() {
            @Override
            public void onChanged(int progress) {
                aimSmoothLevel = progress;
                ShizukuShellManager.applyAimSmooth(true, aimSmoothLevel);
            }
        });
        pageGameTools.addView(makeExpandableFeatureCard(
            "⌖", "AIM SMOOTH", "VIP",
            "Menstabilkan interpolasi sentuhan layar agar pergerakan crosshair tidak patah-patah (Safe No-Root).",
            bodyAimSmooth,
            new ToggleAction() {
                @Override
                public void onToggle(boolean checked) {
                    ShizukuShellManager.applyAimSmooth(checked, aimSmoothLevel);
                }
            }
        ));

        View bodyRecoil = makeVideoSlider("Peredam Getaran Recoil [VIP]", "25%", "50%", "100%", 2, 1, new SliderListener() {
            @Override
            public void onChanged(int progress) {
                recoilLevel = progress;
                ShizukuShellManager.applyControlRecoil(true, recoilLevel);
            }
        });
        pageGameTools.addView(makeExpandableFeatureCard(
            "🛡️", "CONTROL RECOIL", "VIP",
            "Meredam guncangan titik sentuh saat menahan tombol tembak beruntun (Safe No-Root).",
            bodyRecoil,
            new ToggleAction() {
                @Override
                public void onToggle(boolean checked) {
                    ShizukuShellManager.applyControlRecoil(checked, recoilLevel);
                }
            }
        ));

        View bodyAimAssist = makeVideoSlider("Presisi Lock & Gyro Matrix [VIP]", "Ringan", "Medium", "Max Lock", 2, 1, new SliderListener() {
            @Override
            public void onChanged(int progress) {
                aimAssistLevel = progress;
                ShizukuShellManager.applyAimAssist(true, aimAssistLevel);
            }
        });
        pageGameTools.addView(makeExpandableFeatureCard(
            "🔥", "AIM ASSIST", "VIP",
            "Optimalisasi presisi pointer speed & 240Hz touch sampling matrix agar bidikan lengket di target (Safe No-Root).",
            bodyAimAssist,
            new ToggleAction() {
                @Override
                public void onToggle(boolean checked) {
                    ShizukuShellManager.applyAimAssist(checked, aimAssistLevel);
                }
            }
        ));

        View bodyRogPc = makeVideoSlider("Skala Grafik Ringan ROG/PC [VIP]", "ROG Smooth", "PC Balanced", "PC Ultra 120FPS", 2, 2, new SliderListener() {
            @Override
            public void onChanged(int progress) {
                gameSpaceRogLevel = progress;
                ShizukuShellManager.applyGameSpaceRogPc(true, gameSpaceRogLevel, targetPackage);
            }
        });
        pageGameTools.addView(makeExpandableFeatureCard(
            "💻", "GAME SPACE GRAFIK RINGAN ROG/PC", "VIP",
            "Render grafik ringan ala ROG Phone / PC Emulator: Downscale resolusi GPU enteng, paksa 120FPS & SkiaGL tanpa root.",
            bodyRogPc,
            new ToggleAction() {
                @Override
                public void onToggle(boolean checked) {
                    ShizukuShellManager.applyGameSpaceRogPc(checked, gameSpaceRogLevel, targetPackage);
                }
            }
        ));

        View bodyWare = makeVideoSlider("Tingkat Peningkatan Performa [VIP]", "Seimbang", "Tinggi", "Ekstrem 120FPS", 2, 2, new SliderListener() {
            @Override
            public void onChanged(int progress) {
                danzWareLevel = progress;
                ShizukuShellManager.applyDanzWarePerformance(true, danzWareLevel, targetPackage);
            }
        });
        pageGameTools.addView(makeExpandableFeatureCard(
            "🚀", "KALYVRA / DANZ WARE", "VIP",
            "Mesin peningkat performa ekstrem in-game: Unlock 120FPS Game Mode, kunci frekuensi CPU/GPU anti-drop, Skiagl Render, dan AOT Speed.",
            bodyWare,
            new ToggleAction() {
                @Override
                public void onToggle(boolean checked) {
                    ShizukuShellManager.applyDanzWarePerformance(checked, danzWareLevel, targetPackage);
                }
            }
        ));

        LinearLayout dpiBody = new LinearLayout(this);
        dpiBody.setOrientation(LinearLayout.VERTICAL);
        dpiBody.addView(makeLabel("\nPengaturan Awal", 13f, "#FFFFFF", true));
        dpiBody.addView(makeLabel("Alat untuk membuka pengaturan DPI dan resolusi secara tersembunyi.", 11f, "#7C9488", false));
        Button btnSetDpi = makeGreenBtn("KETUK PENGATURAN");
        btnSetDpi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDpiDialog();
            }
        });
        dpiBody.addView(btnSetDpi);

        dpiBody.addView(makeLabel("\nAtur Ulang Resolusi dan DPI", 13f, "#FFFFFF", true));
        dpiBody.addView(makeLabel("Untuk mereset semua sistem resolusi pada tampilan, dengan fitur ini memudahkan untuk kembali normal.", 11f, "#7C9488", false));
        Button btnResetDpi = makeGreenBtn("KETUK UNTUK MENGATUR ULANG");
        btnResetDpi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playGreenLightningAnimation();
                ShizukuShellManager.execAdbAsync("wm density reset && wm size reset");
                Toast.makeText(MainDashboardActivity.this, "Resolusi & DPI dikembalikan ke normal!", Toast.LENGTH_SHORT).show();
            }
        });
        dpiBody.addView(btnResetDpi);

        pageGameTools.addView(makeExpandableFeatureCard(
            "📱", "DPI Smart Costum", "VIP",
            "Fitur ini mengoptimalkan skala DPI secara otomatis ketika bermain dan kembali normal saat digunakan dalam aktivitas standar.",
            dpiBody,
            new ToggleAction() {
                @Override
                public void onToggle(boolean checked) {}
            }
        ));

        LinearLayout pkgRow = new LinearLayout(this);
        pkgRow.setOrientation(LinearLayout.HORIZONTAL);
        Button btnFfTh = makeGreenBtn("com.dts.freefireth");
        Button btnFfMax = makeDarkBtn("com.dts.freefiremax");
        btnFfTh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                targetPackage = "com.dts.freefireth";
                Toast.makeText(MainDashboardActivity.this, "Target aktif: com.dts.freefireth", Toast.LENGTH_SHORT).show();
            }
        });
        btnFfMax.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                targetPackage = "com.dts.freefiremax";
                Toast.makeText(MainDashboardActivity.this, "Target aktif: com.dts.freefiremax", Toast.LENGTH_SHORT).show();
            }
        });
        pkgRow.addView(btnFfTh, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        pkgRow.addView(btnFfMax, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        pageGameTools.addView(pkgRow);

        View bodyPatchSens = makeVideoSlider("Tingkat Sensitivitas [VIP]", "Terendah", "Sedang", "Tertinggi", 7, 4, new SliderListener() {
            @Override
            public void onChanged(int progress) {
                ShizukuShellManager.execAdbAsync("settings put system pointer_speed " + progress);
            }
        });
        pageGameTools.addView(makeExpandableFeatureCard(
            "🎚️", "Patch Sensitivitas 1.0", "",
            "Penyesuaian otomatis untuk sensitivitas yang lebih optimal ( TIDAK SEMUA DEVICE BEKERJA ).",
            bodyPatchSens,
            new ToggleAction() {
                @Override
                public void onToggle(boolean checked) {
                    ShizukuShellManager.execAdbAsync("settings put system pointer_speed " + (checked ? 5 : 0));
                }
            }
        ));

        LinearLayout touchAccBody = new LinearLayout(this);
        touchAccBody.setOrientation(LinearLayout.VERTICAL);
        touchAccBody.addView(makeVideoSlider("Klik Batas Waktu [VIP]", "Terendah", "Sedang", "Tertinggi", 2, 1, new SliderListener() {
            @Override
            public void onChanged(int progress) {
                int val = 300 - (progress * 50);
                ShizukuShellManager.execAdbAsync("settings put secure long_press_timeout " + val);
            }
        }));
        touchAccBody.addView(makeVideoSlider("Batas Waktu Berganda [VIP]", "Bawaan", "Lebih cepat", "Perangkat", 2, 1, new SliderListener() {
            @Override
            public void onChanged(int progress) {
                int val = 250 - (progress * 50);
                ShizukuShellManager.execAdbAsync("settings put secure multi_press_timeout " + val);
            }
        }));
        pageGameTools.addView(makeExpandableFeatureCard(
            "👆", "Akselerator Sentuh", "",
            "Mempercepat respon multi-tap dan gesture detection.",
            touchAccBody,
            new ToggleAction() {
                @Override
                public void onToggle(boolean checked) {
                    ShizukuShellManager.execAdbAsync("settings put secure long_press_timeout " + (checked ? 200 : 400));
                }
            }
        ));

        pageGameTools.addView(makeExpandableFeatureCard(
            "⚡", "Pelempar Permukaan", "VIP",
            "Menyesuaikan konfigurasi SurfaceFlinger dengan refresh rate aktif guna meningkatkan respons compositing dan stabilitas frame rendering.",
            null,
            new ToggleAction() {
                @Override
                public void onToggle(boolean checked) {
                    int v = checked ? 1 : 0;
                    ShizukuShellManager.execAdbAsync("service call SurfaceFlinger 1008 i32 " + v + " && setprop debug.sf.latch_unsignaled " + v);
                }
            }
        ));

        LinearLayout goPhoneBody = new LinearLayout(this);
        goPhoneBody.setOrientation(LinearLayout.VERTICAL);
        goPhoneBody.addView(makeGoPhoneCheck("Freezer scheduler", "device_config put activity_manager use_freezer true"));
        goPhoneBody.addView(makeGoPhoneCheck("Max Cached", "device_config put activity_manager max_cached_processes 4"));
        goPhoneBody.addView(makeGoPhoneCheck("Memory Compaction", "device_config put activity_manager use_compaction true"));
        goPhoneBody.addView(makeGoPhoneCheck("Imperceptible Kill", "am kill-all"));
        pageGameTools.addView(makeExpandableFeatureCard(
            "🧹", "Optimalkan GoPhone", "VIP",
            "Dengan fitur ini dapat mengurangi beban CPU GPU sehingga device terasa lebih ringan dan responsif.",
            goPhoneBody,
            new ToggleAction() {
                @Override
                public void onToggle(boolean checked) {
                    if (checked) ShizukuShellManager.execAdbAsync("am kill-all");
                }
            }
        ));

        pageGameTools.addView(makeExpandableFeatureCard(
            "🗑️", "Bersihkan Cache", "",
            "Membersihkan sebagian cache pada direktori data melalui sistem scan otomatis.",
            null,
            new ToggleAction() {
                @Override
                public void onToggle(boolean checked) {
                    if (checked) {
                        ShizukuShellManager.execAdbAsync("pm trim-caches 4096M && rm -rf /sdcard/Android/data/" + targetPackage + "/cache/*");
                        Toast.makeText(MainDashboardActivity.this, "Cache " + targetPackage + " & sistem dibersihkan!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        ));

        pageGameTools.addView(makeExpandableFeatureCard(
            "🎮", "ANGLE Rendering", "",
            "Menjalankan OpenGL ES melalui Vulkan (ANGLE) untuk meningkatkan kompatibilitas dan pada beberapa perangkat dapat meningkatkan performa.",
            null,
            new ToggleAction() {
                @Override
                public void onToggle(boolean checked) {
                    String mode = checked ? "angle" : "default";
                    ShizukuShellManager.execAdbAsync(
                        "settings put global angle_gl_driver_selection_pkgs " + targetPackage +
                        " && settings put global angle_gl_driver_selection_values " + mode
                    );
                }
            }
        ));

        pageGameTools.addView(makeExpandableFeatureCard(
            "➕", "Kustomisasi Bidik Anda", "",
            "Memudahkan untuk membidik ketika anda menggunakan senjata yang tidak memiliki crosshair.",
            buildCrosshairCustomizerBody(),
            new ToggleAction() {
                @Override
                public void onToggle(boolean checked) {
                    OverlayEngineService.setCrosshairVisible(checked);
                }
            }
        ));

        LinearLayout rogBody = new LinearLayout(this);
        rogBody.setOrientation(LinearLayout.VERTICAL);
        String[] metrics = new String[]{
            "CPU Information", "RAM Information", "Temperature Information",
            "Battery Information", "FPS Information", "Time Information"
        };
        for (int i = 0; i < metrics.length; i++) {
            final int idx = i;
            CheckBox cb = new CheckBox(this);
            cb.setText(metrics[i]);
            cb.setTextColor(Color.WHITE);
            cb.setChecked(true);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    OverlayEngineService.setRogMetric(idx, isChecked);
                }
            });
            rogBody.addView(cb);
        }
        pageGameTools.addView(makeExpandableFeatureCard(
            "📊", "Pemantauan ROG", "",
            "Pemantauan Suhu, jam, GPU, CPU, FPS secara real-time dengan menggunakan style ROG.",
            rogBody,
            new ToggleAction() {
                @Override
                public void onToggle(boolean checked) {
                    OverlayEngineService.setRogVisible(checked);
                }
            }
        ));
    }

    private View buildCrosshairCustomizerBody() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout styleRow = new LinearLayout(this);
        styleRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] icons = new String[]{"✛", "⌖", "➕", "⊕"};
        for (int i = 0; i < icons.length; i++) {
            final int sIdx = i;
            Button b = makeDarkBtn(icons[i]);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OverlayEngineService.setCrosshairStyle(sIdx);
                }
            });
            styleRow.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        box.addView(styleRow);

        final TextView posInfo = makeLabel("X = 0   Y = 0          Sudut: 80°          Size: 58", 12f, "#00E676", true);
        box.addView(posInfo);

        LinearLayout dpadRow = new LinearLayout(this);
        dpadRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] dLabels = new String[]{"◀ X-", "▲ Y-", "▼ Y+", "▶ X+"};
        final int[][] deltas = new int[][]{{-4, 0}, {0, -4}, {0, 4}, {4, 0}};
        for (int i = 0; i < 4; i++) {
            final int dx = deltas[i][0];
            final int dy = delts(deltas[i][1]);
            Button b = makeDarkBtn(dLabels[i]);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OverlayEngineService.nudgeCrosshairPosition(dx, dy);
                    posInfo.setText("X = " + OverlayEngineService.getCrosshairX() + "   Y = " + OverlayEngineService.getCrosshairY());
                }
            });
            dpadRow.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        box.addView(dpadRow);

        box.addView(makeVideoSlider("Rotasi Sudut Bidik (0° - 360°)", "0°", "180°", "360°", 360, 80, new SliderListener() {
            @Override
            public void onChanged(int progress) {
                OverlayEngineService.setCrosshairAngle(progress);
                posInfo.setText("X = " + OverlayEngineService.getCrosshairX() + "   Y = " + OverlayEngineService.getCrosshairY() + "   Sudut: " + progress + "°");
            }
        }));

        box.addView(makeVideoSlider("Ukuran Bidik (Size)", "Kecil", "58", "Besar", 140, 58, new SliderListener() {
            @Override
            public void onChanged(int progress) {
                OverlayEngineService.setCrosshairSize(Math.max(20, progress));
            }
        }));

        box.addView(makeVideoSlider("Opacity", "Transparan", "Sedang", "Penuh", 255, 255, new SliderListener() {
            @Override
            public void onChanged(int progress) {
                OverlayEngineService.setCrosshairAlpha(progress);
            }
        }));

        LinearLayout colorRow = new LinearLayout(this);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        final int[] colors = new int[]{
            Color.parseColor("#00E676"), Color.parseColor("#FF6D00"), Color.parseColor("#00B0FF"), Color.WHITE
        };
        String[] cNames = new String[]{"HIJAU", "ORANYE", "BIRU", "PUTIH"};
        for (int i = 0; i < colors.length; i++) {
            final int cVal = colors[i];
            Button b = makeDarkBtn(cNames[i]);
            b.setTextColor(cVal);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OverlayEngineService.setCrosshairColor(cVal);
                }
            });
            colorRow.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        box.addView(colorRow);

        return box;
    }

    private int delts(int v) { return v; }

    private void showDpiDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("DPI Smart Costum (Shizuku ADB)");
        b.setMessage("Masukkan nilai DPI untuk Free Fire:");
        final EditText input = new EditText(this);
        input.setHint("Contoh: 580");
        b.setView(input);
        b.setPositiveButton("TERAPKAN", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String dpi = input.getText().toString().trim();
                if (!dpi.isEmpty()) {
                    playGreenLightningAnimation();
                    ShizukuShellManager.execAdbAsync("wm density " + dpi);
                    Toast.makeText(MainDashboardActivity.this, "DPI diatur ke " + dpi, Toast.LENGTH_SHORT).show();
                }
            }
        });
        b.setNegativeButton("BATAL", null);
        b.show();
    }

    private View makeGoPhoneCheck(String label, final String cmd) {
        CheckBox cb = new CheckBox(this);
        cb.setText(label);
        cb.setTextColor(Color.WHITE);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    playGreenLightningAnimation();
                    ShizukuShellManager.execAdbAsync(cmd);
                }
            }
        });
        return cb;
    }

    private LinearLayout makeExpandableFeatureCard(String icon, String title, String badge, String desc, final View extraBody, final ToggleAction action) {
        final LinearLayout card = makeVideoCard("#0E1914", "#1B3026");

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleTv = makeLabel(icon + "  " + title, 15f, "#FFFFFF", true);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleTv.setLayoutParams(tLp);
        headerRow.addView(titleTv);

        if (badge != null && !badge.isEmpty()) {
            TextView badgeTv = makeLabel(badge, 10f, "#00E676", true);
            badgeTv.setPadding(16, 4, 16, 4);
            GradientDrawable bBg = new GradientDrawable();
            bBg.setColor(Color.parseColor("#143826"));
            bBg.setCornerRadius(12f);
            badgeTv.setBackground(bBg);
            headerRow.addView(badgeTv);
        }

        Switch sw = new Switch(this);
        headerRow.addView(sw);
        card.addView(headerRow);

        card.addView(makeLabel(desc, 12f, "#7C9488", false));

        if (extraBody != null) {
            extraBody.setVisibility(View.GONE);
            card.addView(extraBody);
        }

        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                playGreenLightningAnimation();
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(24f);
                bg.setColor(Color.parseColor(isChecked ? "#11241B" : "#0E1914"));
                bg.setStroke(2, Color.parseColor(isChecked ? "#00E676" : "#1B3026"));
                card.setBackground(bg);

                if (extraBody != null) {
                    extraBody.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
                if (action != null) {
                    action.onToggle(isChecked);
                }
            }
        });

        return card;
    }

    private View makeVideoSlider(String title, String leftLbl, String midLbl, String rightLbl, int max, int def, final SliderListener listener) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(0, 16, 0, 8);
        wrap.addView(makeLabel(title, 12f, "#FFFFFF", true));

        SeekBar sb = new SeekBar(this);
        sb.setMax(max);
        sb.setProgress(def);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (listener != null) listener.onChanged(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        wrap.addView(sb);

        LinearLayout lblRow = new LinearLayout(this);
        lblRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView l1 = makeLabel(leftLbl, 10f, "#7C9488", false);
        TextView l2 = makeLabel(midLbl, 10f, "#7C9488", false);
        l2.setGravity(Gravity.CENTER);
        TextView l3 = makeLabel(rightLbl, 10f, "#7C9488", false);
        l3.setGravity(Gravity.END);
        lblRow.addView(l1, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        lblRow.addView(l2, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        lblRow.addView(l3, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        wrap.addView(lblRow);
        return wrap;
    }

    private LinearLayout makeSysTile(String title, String bigVal, String subVal) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(24, 20, 24, 20);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#13221B"));
        bg.setCornerRadius(18f);
        tile.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(8, 8, 8, 8);
        tile.setLayoutParams(lp);

        tile.addView(makeLabel(title, 11f, "#00E676", true));
        tile.addView(makeLabel(bigVal, 18f, "#FFFFFF", true));
        tile.addView(makeLabel(subVal, 10f, "#7C9488", false));
        return tile;
    }

    private LinearLayout makeVideoCard(String bgColorHex, String strokeHex) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(32, 28, 32, 28);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(bgColorHex));
        bg.setCornerRadius(24f);
        bg.setStroke(2, Color.parseColor(strokeHex));
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = 24;
        card.setLayoutParams(lp);
        return card;
    }

    private TextView makeLabel(String text, float sizeSp, String colorHex, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(sizeSp);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        tv.setPadding(0, 4, 0, 4);
        return tv;
    }

    private Button makeGreenBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.parseColor("#070D0A"));
        b.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#00E676"));
        bg.setCornerRadius(16f);
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = 14;
        b.setLayoutParams(lp);
        return b;
    }

    private Button makeDarkBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#172B22"));
        bg.setCornerRadius(14f);
        bg.setStroke(1, Color.parseColor("#284A3A"));
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(6, 10, 6, 6);
        b.setLayoutParams(lp);
        return b;
    }

    private TextView makeChip(String text, boolean active) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(12f);
        tv.setPadding(28, 12, 28, 12);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(30f);
        if (active) {
            bg.setColor(Color.parseColor("#00E676"));
            tv.setTextColor(Color.parseColor("#070D0A"));
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            bg.setColor(Color.parseColor("#12211A"));
            tv.setTextColor(Color.parseColor("#A0B8AC"));
        }
        tv.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.rightMargin = 16;
        tv.setLayoutParams(lp);
        return tv;
    }

    private void startUptimeTicker() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long sec = (System.currentTimeMillis() - startTimeMs) / 1000L;
                long h = sec / 3600L;
                long m = (sec % 3600L) / 60L;
                long s = sec % 60L;
                if (uptimeClockTv != null) {
                    uptimeClockTv.setText(String.format(Locale.US, "%02d:%02d:%02d Waktu Aktif", h, m, s));
                }
                handler.postDelayed(this, 1000L);
            }
        }, 1000L);
    }
}
