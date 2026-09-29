package com.blastarena.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.blastarena.core.model.GameConfig;
import com.blastarena.desktop.render.Layout;

public final class DesktopLauncher {

    private DesktopLauncher() {
    }

    /** Usage: {@code --screenshot=/path/to/file.png} saves one frame and quits. */
    private static String screenshotPath(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--screenshot=")) {
                return arg.substring("--screenshot=".length());
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
        new Lwjgl3Application(new BlastArenaGame(layout, screenshotPath(args)), config);
    }
}
