package org.telegram.messenger;

import android.net.Uri;

import java.util.List;
import java.util.Locale;

public class CookiegramLinks {

    public static final String SETTINGS_USERNAME = "cookiesettings";

    /**
     * null: это не наша ссылка;
     * "": открыть настройки Cookiegram;
     * иначе "тема" или "тема/настройка" в нижнем регистре.
     */
    public static String parseSettingsLink(Uri uri) {
        if (uri == null || uri.getHost() == null) {
            return null;
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }
        if (!host.equals("t.me") && !host.equals("telegram.me") && !host.equals("telegram.dog")) {
            return null;
        }
        List<String> segments = uri.getPathSegments();
        if (segments.isEmpty() || !SETTINGS_USERNAME.equalsIgnoreCase(segments.get(0))) {
            return null;
        }
        String query = uri.getQuery(); // для ?design/toggledesign это "design/toggledesign"
        if (query == null) {
            return "";
        }
        query = query.trim();
        while (query.startsWith("/")) {
            query = query.substring(1);
        }
        while (query.endsWith("/")) {
            query = query.substring(0, query.length() - 1);
        }
        return query.toLowerCase(Locale.ROOT);
    }
}