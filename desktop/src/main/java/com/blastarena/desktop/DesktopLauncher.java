package com.blastarena.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.blastarena.core.model.GameConfig;
import com.blastarena.desktop.render.Layout;

public final class DesktopLauncher {

    private DesktopLauncher() {
    }

    /** Default delay for {@code --screenshot}: just past the three-second countdown. */
    private static final float DEFAULT_SCREENSHOT_SECONDS = 4f;

    /** Usage: {@code --screenshot=/path/to/file.png [--screenshot-after=seconds]} saves one frame and quits. */
    private static String option(String[] args, String name) {
        String prefix = "--" + name + "=";
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }
        return null;
    }

    public static void main(String[] args) {
        GameConfig defaults = GameConfig.builder().build();
        Layout layout = Layout.forBoard(defaults.width(), defaults.height());

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Blast Arena");
        config.setWindowedMode((int) layout.boardWidth(), (int) layout.totalHeight());
        config.useVsync(true);
        config.setForegroundFPS(60);
        String after = option(args, "screenshot-after");
        float screenshotAfter = after == null ? DEFAULT_SCREENSHOT_SECONDS : Float.parseFloat(after);
        new Lwjgl3Application(new BlastArenaGame(layout, option(args, "screenshot"), screenshotAfter), config);
    }
}
