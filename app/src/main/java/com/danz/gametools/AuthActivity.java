package com.danz.gametools;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class AuthActivity extends Activity {

    private LicenseManager licenseManager;
    private TextView shizukuBadge;
    private EditText keyInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        licenseManager = new LicenseManager(this);

        ScrollView rootScroll = new ScrollView(this);
        rootScroll.setFillViewport(true);
        rootScroll.setBackgroundColor(Color.parseColor("#070D0A"));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(48, 80, 48, 48);

        ImageView logoImg = new ImageView(this);
        int logoResId = getResources().getIdentifier("danz_logo", "drawable", getPackageName());
        if (logoResId != 0) {
            logoImg.setImageResource(logoResId);
        }
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(260, 260);
        logoLp.bottomMargin = 24;
        logoImg.setLayoutParams(logoLp);
        container.addView(logoImg);

        TextView titleTv = new TextView(this);
        titleTv.setText("KALYVRA TOOLS");
        titleTv.setTextColor(Color.parseColor("#00E676"));
        titleTv.setTextSize(26f);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleTv.setGravity(Gravity.CENTER);
        container.addView(titleTv);

        TextView subTv = new TextView(this);
        subTv.setText("By DanzOfficial • SHIZUKU ENGINE A11+");
        subTv.setTextColor(Color.parseColor("#8FA89B"));
        subTv.setTextSize(12f);
        subTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subLp.topMargin = 8;
        subLp.bottomMargin = 40;
        subTv.setLayoutParams(subLp);
        container.addView(subTv);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(36, 36, 36, 36);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0E1914"));
        cardBg.setCornerRadius(28f);
        cardBg.setStroke(2, Color.parseColor("#1E3F2E"));
        card.setBackground(cardBg);

        shizukuBadge = new TextView(this);
        shizukuBadge.setTextSize(13f);
        shizukuBadge.setPadding(0, 0, 0, 20);
        card.addView(shizukuBadge);

        Button btnShizuku = new Button(this);
        btnShizuku.setText("HUBUNGKAN / VERIFIKASI SHIZUKU ADB");
        btnShizuku.setAllCaps(false);
        btnShizuku.setTextColor(Color.parseColor("#070D0A"));
        btnShizuku.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable shizBtnBg = new GradientDrawable();
        shizBtnBg.setColor(Color.parseColor("#00E676"));
        shizBtnBg.setCornerRadius(18f);
        btnShizuku.setBackground(shizBtnBg);
        btnShizuku.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShizukuShellManager.grantManualAuthorization();
                Intent launch = getPackageManager().getLaunchIntentForPackage(ShizukuShellManager.SHIZUKU_PKG);
                if (launch != null && !ShizukuShellManager.isShizukuRunning(AuthActivity.this)) {
                    startActivity(launch);
                } else {
                    Toast.makeText(AuthActivity.this, "Shizuku ADB Binder Terhubung (UID 2000)!", Toast.LENGTH_SHORT).show();
                }
                updateShizukuBadge();
            }
        });
        card.addView(btnShizuku);

        LinearLayout hwidRow = new LinearLayout(this);
        hwidRow.setOrientation(LinearLayout.HORIZONTAL);
        hwidRow.setGravity(Gravity.CENTER_VERTICAL);
        hwidRow.setPadding(0, 28, 0, 12);

        TextView hwidTv = new TextView(this);
        hwidTv.setText("HWID Perangkat: " + licenseManager.getDeviceHwid());
        hwidTv.setTextColor(Color.parseColor("#A5C4B4"));
        hwidTv.setTextSize(13f);
        LinearLayout.LayoutParams hwidLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        hwidTv.setLayoutParams(hwidLp);
        hwidRow.addView(hwidTv);

        Button btnCopyHwid = new Button(this);
        btnCopyHwid.setText("SALIN HWID");
        btnCopyHwid.setTextColor(Color.parseColor("#00E676"));
        btnCopyHwid.setTextSize(11f);
        GradientDrawable copyBg = new GradientDrawable();
        copyBg.setColor(Color.parseColor("#162920"));
        copyBg.setCornerRadius(14f);
        copyBg.setStroke(1, Color.parseColor("#00E676"));
        btnCopyHwid.setBackground(copyBg);
        btnCopyHwid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("HWID", licenseManager.getDeviceHwid()));
                Toast.makeText(AuthActivity.this, "HWID disalin!", Toast.LENGTH_SHORT).show();
            }
        });
        hwidRow.addView(btnCopyHwid);
        card.addView(hwidRow);

        TextView keyLabel = new TextView(this);
        keyLabel.setText("\nMasukkan License Key (1 Key 1 Device):");
        keyLabel.setTextColor(Color.WHITE);
        keyLabel.setTextSize(13f);
        card.addView(keyLabel);

        keyInput = new EditText(this);
        keyInput.setHint("Masukkan Key Lisensi... (Key: DANZ)");
        keyInput.setHintTextColor(Color.parseColor("#526B5E"));
        keyInput.setTextColor(Color.WHITE);
        keyInput.setSingleLine(true);
        keyInput.setPadding(28, 24, 28, 24);
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#12211A"));
        inputBg.setCornerRadius(16f);
        inputBg.setStroke(2, Color.parseColor("#264D3B"));
        keyInput.setBackground(inputBg);
        String saved = licenseManager.getSavedKey();
        keyInput.setText(saved.isEmpty() ? "DANZ" : saved);
        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inLp.topMargin = 14;
        inLp.bottomMargin = 28;
        keyInput.setLayoutParams(inLp);
        card.addView(keyInput);

        Button btnLogin = new Button(this);
        btnLogin.setText("MASUK KE KALYVRA TOOLS");
        btnLogin.setTextColor(Color.parseColor("#070D0A"));
        btnLogin.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable loginBg = new GradientDrawable();
        loginBg.setColor(Color.parseColor("#00E676"));
        loginBg.setCornerRadius(18f);
        btnLogin.setBackground(loginBg);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doLogin();
            }
        });
        card.addView(btnLogin);

        container.addView(card);
        rootScroll.addView(container);
        setContentView(rootScroll);
        updateShizukuBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateShizukuBadge();
    }

    private void updateShizukuBadge() {
        boolean running = ShizukuShellManager.isShizukuRunning(this);
        boolean perm = ShizukuShellManager.hasShizukuPermission(this);
        String text = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n" +
                      "Status Shizuku: " + (running ? "ONLINE (UID 2000)" : "SIAP HUBUNGKAN") + "\n" +
                      "Izin Shizuku  : " + (perm ? "DIIZINKAN ✔" : "KLIK TOMBOL DI BAWAH");
        shizukuBadge.setText(text);
        shizukuBadge.setTextColor(running && perm ? Color.parseColor("#00E676") : Color.parseColor("#FFB300"));
    }

    private void doLogin() {
        LicenseManager.ValidationResult res = licenseManager.validateAndBindKey(keyInput.getText().toString());
        if (!res.valid) {
            Toast.makeText(this, res.message, Toast.LENGTH_LONG).show();
            return;
        }
        Intent i = new Intent(this, MainDashboardActivity.class);
        i.putExtra("ROLE", res.role);
        i.putExtra("KEY", res.key);
        i.putExtra("HWID", res.hwid);
        i.putExtra("EXPIRES", res.expiresAt);
        startActivity(i);
        finish();
    }
}
