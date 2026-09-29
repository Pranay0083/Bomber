package com.blastarena.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.blastarena.core.board.MapGenerator;
import com.blastarena.core.bot.Difficulty;
import com.blastarena.core.level.CustomLevelSource;
import com.blastarena.core.level.EditableLevel;
import com.blastarena.core.level.LevelData;
import com.blastarena.core.level.LevelValidator;
import com.blastarena.core.level.RandomMapSource;
import com.blastarena.desktop.screen.MatchSettings;
import com.blastarena.desktop.screen.MenuScreen;
import com.blastarena.desktop.screen.Navigator;
import com.blastarena.desktop.storage.FileLevelRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Starts the game on the menu. For checking things without clicking through:
 * {@code --start=menu|editor|random}, {@code --play=<level name>}, {@code --edit=<level name>},
 * {@code --bots=1..3}, {@code --difficulty=easy|medium|hard}, {@code --best-of=1|3|5},
 * {@code --autoplay} to put a bot in your seat and run the match by itself, {@code --speed=<n>} to run faster,
 * and {@code --screenshot=<file.png> [--screenshot-after=<seconds>]} to save one frame and quit.
 */
public final class DesktopLauncher {

    /** Default delay for {@code --screenshot}: just past the three-second countdown. */
    private static final float DEFAULT_SCREENSHOT_SECONDS = 4f;

    private DesktopLauncher() {
    }

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
        FileLevelRepository repository = FileLevelRepository.inHomeDirectory();

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Blast Arena");
        config.setWindowedMode((int) MenuScreen.WIDTH, (int) MenuScreen.HEIGHT);
        config.useVsync(true);
        config.setForegroundFPS(60);

        String screenshot = option(args, "screenshot");
        if (screenshot != null) {
            // A screenshot run needs no one at the keyboard: keep the window hidden so it cannot take focus
            // and swallow keys typed into other apps.
            config.setInitialVisible(false);
        }
        String after = option(args, "screenshot-after");
        float screenshotAfter = after == null ? DEFAULT_SCREENSHOT_SECONDS : Float.parseFloat(after);
        boolean autoplay = List.of(args).contains("--autoplay");
        String speed = option(args, "speed");
        new Lwjgl3Application(new BlastArenaGame(repository, settings(args), autoplay,
                speed == null ? 1f : Float.parseFloat(speed),
                firstScreen(args, repository), screenshot, screenshotAfter), config);
    }

    private static MatchSettings settings(String[] args) {
        MatchSettings defaults = MatchSettings.DEFAULT;
        String bots = option(args, "bots");
        String difficulty = option(args, "difficulty");
        String bestOf = option(args, "best-of");
        return new MatchSettings(
                bots == null ? defaults.bots() : Integer.parseInt(bots),
                difficulty == null ? defaults.difficulty() : Difficulty.valueOf(difficulty.toUpperCase(Locale.ROOT)),
                bestOf == null ? defaults.bestOf() : Integer.parseInt(bestOf));
    }

    private static Consumer<Navigator> firstScreen(String[] args, FileLevelRepository repository) {
        String play = option(args, "play");
        String edit = option(args, "edit");
        String start = Optional.ofNullable(option(args, "start")).orElse("menu");
        return navigator -> {
            if (play != null) {
                load(repository, play).ifPresentOrElse(level -> {
                    if (LevelValidator.standard().isPlayable(level)) {
                        navigator.play(new CustomLevelSource(level), navigator.settings(), navigator::showMenu);
                    } else {
                        navigator.edit(EditableLevel.from(level), level.name());
                    }
                }, navigator::showMenu);
            } else if (edit != null) {
                load(repository, edit).ifPresentOrElse(
                        level -> navigator.edit(EditableLevel.from(level), level.name()),
                        navigator::showMenu);
            } else if (start.equals("editor")) {
                navigator.edit(EditableLevel.blank("Untitled", 13, 11), null);
            } else if (start.equals("random")) {
                navigator.play(new RandomMapSource(new MapGenerator()), navigator.settings(), navigator::showMenu);
            } else {
                navigator.showMenu();
            }
        };
    }

    private static Optional<LevelData> load(FileLevelRepository repository, String name) {
        try {
            return repository.load(name);
        } catch (Exception e) {
            System.err.println("Could not load level " + name + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}
