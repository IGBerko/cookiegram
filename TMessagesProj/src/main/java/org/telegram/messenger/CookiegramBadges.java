package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

public class CookiegramBadges {

    public static class Badge {
        public final String text;
        public final String iconUrl;
        public final int bgColor;
        public final int textColor;

        Badge(String text, String iconUrl, int bgColor, int textColor) {
            this.text = text;
            this.iconUrl = iconUrl;
            this.bgColor = bgColor;
            this.textColor = textColor;
        }
    }

    private static final String SOURCE_URL =
            "https://raw.githubusercontent.com/IGBerko/cookiegram/master/badges.json";
    private static final String PREFS = "cookiegram_badges";
    private static final long REFRESH_MS = 6 * 60 * 60 * 1000L;
    private static final int MAX_BYTES = 256 * 1024;
    private static final int MAX_TEXT = 120;

    private static volatile HashMap<Long, Badge> badges = new HashMap<>();

    public static Badge get(long userId) {
        return badges.get(userId);
    }

    public static void init() {
        Utilities.globalQueue.postRunnable(() -> {
            SharedPreferences prefs = ApplicationLoader.applicationContext
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String cached = prefs.getString("json", null);
            if (cached != null) {
                badges = parse(cached);
            }
            long last = prefs.getLong("time", 0);
            if (System.currentTimeMillis() - last > REFRESH_MS) {
                refresh(prefs);
            }
        });
    }

    private static void refresh(SharedPreferences prefs) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(SOURCE_URL).openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(10000);
            String etag = prefs.getString("etag", null);
            if (etag != null) c.setRequestProperty("If-None-Match", etag);

            int code = c.getResponseCode();
            if (code == 304) {
                prefs.edit().putLong("time", System.currentTimeMillis()).apply();
                return;
            }
            if (code != 200) return;

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = c.getInputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    if (out.size() > MAX_BYTES) return; // слишком большой файл
                }
            }
            String json = out.toString("UTF-8");
            HashMap<Long, Badge> parsed = parse(json);
            badges = parsed;
            prefs.edit()
                    .putString("json", json)
                    .putString("etag", c.getHeaderField("ETag"))
                    .putLong("time", System.currentTimeMillis())
                    .apply();
        } catch (Throwable e) {
            FileLog.e(e);
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static HashMap<Long, Badge> parse(String json) {
        HashMap<Long, Badge> result = new HashMap<>();
        try {
            JSONObject root = new JSONObject(json);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    long id = Long.parseLong(key);
                    JSONObject o = root.getJSONObject(key);
                    String text = o.optString("text", "");
                    if (text.isEmpty()) continue;
                    if (text.length() > MAX_TEXT) text = text.substring(0, MAX_TEXT);
                    String icon = o.optString("icon_url", "");
                    if (!icon.startsWith("https://")) icon = "";
                    int bg = parseColor(o.optString("bg_color"), 0xFF2D1B10);
                    int fg = parseColor(o.optString("text_color"), 0xFFFFFFFF);
                    result.put(id, new Badge(text, icon, bg, fg));
                } catch (Throwable ignore) {
                    // битая запись пропускается, остальные работают
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return result;
    }

    private static int parseColor(String s, int fallback) {
        try {
            return Color.parseColor(s);
        } catch (Throwable e) {
            return fallback;
        }
    }
}