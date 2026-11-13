package com.danz.gametools;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONObject;

public class LicenseManager {

    private static final String PREF_NAME = "danz_license_vault_v10";
    private static final String KEY_DB_JSON = "license_records_json";
    private static final String KEY_ACTIVE_CODE = "active_license_key";
    private static final String HMAC_SECRET = "DANZ_TOOLS_SA_TEAM_SECRET_2026";
    private static final String MASTER_DEV_KEY = "DANZ";

    private final SharedPreferences prefs;
    private final String deviceHwid;

    public enum Tier {
        DAY_1("1DAY", "1 Day", 86400000L, "USER"),
        DAY_3("3DAY", "3 Day", 259200000L, "USER"),
        DAY_7("7DAY", "7 Day", 604800000L, "USER"),
        DAY_15("15DAY", "15 Day", 1296000000L, "USER"),
        DAY_30("30DAY", "30 Day", 2592000000L, "USER"),
        PERMANENT("PERM", "Permanent", 315360000000L, "VIP_PERM"),
        RESELLER("RSLR", "Reseller", 315360000000L, "RESELLER");

        public final String code;
        public final String label;
        public final long durationMs;
        public final String role;

        Tier(String code, String label, long durationMs, String role) {
            this.code = code;
            this.label = label;
            this.durationMs = durationMs;
            this.role = role;
        }
    }

    public static class ValidationResult {
        public final boolean valid;
        public final String message;
        public final String role;
        public final String key;
        public final String hwid;
        public final long expiresAt;

        public ValidationResult(boolean valid, String message, String role, String key, String hwid, long expiresAt) {
            this.valid = valid;
            this.message = message;
            this.role = role;
            this.key = key;
            this.hwid = hwid;
            this.expiresAt = expiresAt;
        }
    }

    public LicenseManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.deviceHwid = computeDeviceHwid(context);
        ensureMasterKeySeeded();
    }

    private String computeDeviceHwid(Context context) {
        try {
            String aid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (aid == null) aid = "NO_AID";
            String raw = aid + "|" + Build.BRAND + "|" + Build.MODEL + "|" + Build.HARDWARE;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "5bffa90c";
        }
    }

    public String getDeviceHwid() {
        return deviceHwid;
    }

    public String getSavedKey() {
        return prefs.getString(KEY_ACTIVE_CODE, "");
    }

    private JSONObject loadDb() {
        try {
            return new JSONObject(prefs.getString(KEY_DB_JSON, "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private void saveDb(JSONObject db) {
        prefs.edit().putString(KEY_DB_JSON, db.toString()).apply();
    }

    private void ensureMasterKeySeeded() {
        try {
            JSONObject db = loadDb();
            String[] masters = new String[]{MASTER_DEV_KEY, "DANZDEV"};
            for (int i = 0; i < masters.length; i++) {
                String k = masters[i];
                if (!db.has(k)) {
                    JSONObject obj = new JSONObject();
                    obj.put("tier", "PERM");
                    obj.put("role", "DEVELOPER");
                    obj.put("durationMs", 315360000000L);
                    obj.put("boundHwid", "");
                    obj.put("activatedAt", 0L);
                    obj.put("expiresAt", System.currentTimeMillis() + 315360000000L);
                    db.put(k, obj);
                }
            }
            saveDb(db);
        } catch (Exception ignored) {
        }
    }

    private String signBlock(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String b64 = Base64.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
            String clean = b64.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.US);
            return clean.length() >= 4 ? clean.substring(0, 4) : "9999";
        } catch (Exception e) {
            return "9999";
        }
    }

    public String generateLicenseKey(Tier tier, String optionalHwid) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random rnd = new Random();
        StringBuilder randPart = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            randPart.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        String sig = signBlock(tier.code + "|" + randPart.toString());
        String fullKey = "DANZ-" + tier.code + "-" + randPart.toString() + "-" + sig;

        try {
            JSONObject db = loadDb();
            JSONObject obj = new JSONObject();
            obj.put("tier", tier.code);
            obj.put("role", tier.role);
            obj.put("durationMs", tier.durationMs);
            obj.put("boundHwid", optionalHwid == null ? "" : optionalHwid.trim());
            obj.put("activatedAt", 0L);
            obj.put("expiresAt", 0L);
            db.put(fullKey, obj);
            saveDb(db);
        } catch (Exception ignored) {
        }
        return fullKey;
    }

    private boolean tryImportSelfSignedKey(String key, JSONObject db) {
        try {
            String[] parts = key.split("-");
            if (parts.length != 4 || !parts[0].equals("DANZ")) return false;
            String tierCode = parts[1];
            String randPart = parts[2];
            String sig = parts[3];
            if (!signBlock(tierCode + "|" + randPart).equals(sig)) return false;

            Tier matched = null;
            for (Tier t : Tier.values()) {
                if (t.code.equals(tierCode)) {
                    matched = t;
                    break;
                }
            }
            if (matched == null) return false;

            JSONObject obj = new JSONObject();
            obj.put("tier", matched.code);
            obj.put("role", matched.role);
            obj.put("durationMs", matched.durationMs);
            obj.put("boundHwid", "");
            obj.put("activatedAt", 0L);
            obj.put("expiresAt", 0L);
            db.put(key, obj);
            saveDb(db);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public ValidationResult validateAndBindKey(String rawKey) {
        if (rawKey == null || rawKey.trim().isEmpty()) {
            return new ValidationResult(false, "Key tidak boleh kosong!", "NONE", "", deviceHwid, 0L);
        }
        String key = rawKey.trim().toUpperCase(Locale.US);
        try {
            JSONObject db = loadDb();
            if (!db.has(key) && !tryImportSelfSignedKey(key, db)) {
                return new ValidationResult(false, "Key tidak valid atau tidak terdaftar!", "NONE", key, deviceHwid, 0L);
            }
            JSONObject rec = db.getJSONObject(key);
            String bound = rec.optString("boundHwid", "");
            long activatedAt = rec.optLong("activatedAt", 0L);
            long durationMs = rec.optLong("durationMs", 86400000L);
            long expiresAt = rec.optLong("expiresAt", 0L);
            String role = rec.optString("role", "USER");

            if (!key.equals(MASTER_DEV_KEY) && !key.equals("DANZDEV")) {
                if (!bound.isEmpty() && !bound.equalsIgnoreCase(deviceHwid)) {
                    return new ValidationResult(false, "DITOLAK (1 Key 1 Device)! Key sudah terikat di HWID: " + bound, "NONE", key, deviceHwid, 0L);
                }
            }

            long now = System.currentTimeMillis();
            if (activatedAt == 0L || expiresAt == 0L) {
                activatedAt = now;
                expiresAt = now + durationMs;
                rec.put("activatedAt", activatedAt);
                rec.put("expiresAt", expiresAt);
            }
            if (now > expiresAt) {
                return new ValidationResult(false, "Key sudah EXPIRED pada " + formatTimestamp(expiresAt), "NONE", key, deviceHwid, expiresAt);
            }

            rec.put("boundHwid", deviceHwid);
            db.put(key, rec);
            saveDb(db);
            prefs.edit().putString(KEY_ACTIVE_CODE, key).apply();
            return new ValidationResult(true, "Verified Access (" + role + ")", role, key, deviceHwid, expiresAt);
        } catch (Exception e) {
            return new ValidationResult(false, "Database error", "NONE", key, deviceHwid, 0L);
        }
    }

    public boolean resetHwidForKey(String rawKey) {
        if (rawKey == null) return false;
        String key = rawKey.trim().toUpperCase(Locale.US);
        try {
            JSONObject db = loadDb();
            if (!db.has(key)) return false;
            JSONObject rec = db.getJSONObject(key);
            rec.put("boundHwid", "");
            db.put(key, rec);
            saveDb(db);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String dumpAllKeysSummary() {
        StringBuilder sb = new StringBuilder();
        try {
            JSONObject db = loadDb();
            Iterator<String> it = db.keys();
            while (it.hasNext()) {
                String k = it.next();
                JSONObject r = db.optJSONObject(k);
                if (r != null) {
                    String role = r.optString("role", "USER");
                    String b = r.optString("boundHwid", "");
                    sb.append("• ").append(k).append(" [").append(role).append("] ")
                      .append(b.isEmpty() ? "(Free)" : "(HWID: " + b + ")").append("\n");
                }
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    public static String formatTimestamp(long ms) {
        if (ms <= 0) return "-";
        return new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID")).format(new Date(ms));
    }
}
