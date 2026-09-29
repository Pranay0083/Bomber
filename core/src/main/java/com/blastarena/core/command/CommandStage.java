package com.blastarena.core.command;

/** When in a tick a command runs. All moves run before any bomb is placed. */
public enum CommandStage {
    MOVE,
    PLACE_BOMB,
    NONE
}
