package org.telegram.messenger;

public class CookiegramConfig {

    private static final String KEY_OLD_DESIGN = "cookiegram_old_design";

    public static boolean isOldDesign() {
        return MessagesController.getGlobalMainSettings().getBoolean(KEY_OLD_DESIGN, true);
    }

    public static void setOldDesign(boolean value) {
        MessagesController.getGlobalMainSettings().edit().putBoolean(KEY_OLD_DESIGN, value).apply();
    }
}