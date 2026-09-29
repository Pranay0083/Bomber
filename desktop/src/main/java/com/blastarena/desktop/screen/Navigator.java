package com.blastarena.desktop.screen;

import com.badlogic.gdx.Screen;
import com.blastarena.core.engine.Match;
import com.blastarena.core.level.EditableLevel;
import com.blastarena.core.level.MapSource;

/** Moves between screens. Screens only ask for where to go next, never build each other's dependencies. */
public interface Navigator {

    void showMenu();

    /** The settings last chosen in the menu. */
    MatchSettings settings();

    void changeSettings(MatchSettings settings);

    /** Plays a match on the map, and calls {@code onExit} when the player leaves. */
    void play(MapSource mapSource, MatchSettings settings, Runnable onExit);

    /** Shows the final scores of a match, offering to play the same map again. */
    void showResults(Match match, MapSource mapSource, MatchSettings settings, Runnable onExit);

    /** Opens the editor on a level; {@code savedName} is the name it is stored under, or null if unsaved. */
    void edit(EditableLevel level, String savedName);

    /** Goes back to a screen that is still alive, such as the editor after a test play. */
    void resume(Screen screen);
}
