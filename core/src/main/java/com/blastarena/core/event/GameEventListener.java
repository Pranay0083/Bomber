package com.blastarena.core.event;

/** Reacts to game events: the renderer, sound and scoreboard are all listeners. */
@FunctionalInterface
public interface GameEventListener {

    void onEvent(GameEvent event);
}
