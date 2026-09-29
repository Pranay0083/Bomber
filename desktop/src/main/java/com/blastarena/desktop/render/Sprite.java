package com.blastarena.desktop.render;

/** Every sprite in sprites.png, in sheet order. Must match tools/make_sprites.py. */
public enum Sprite {
    FLOOR_A,
    FLOOR_B,
    WALL,
    CRATE,
    BOMB_0,
    BOMB_1,
    FIRE_0,
    FIRE_1,
    FIRE_2,
    FIRE_3,
    PLAYER,
    POWER_UP_BOMB,
    POWER_UP_RANGE,
    POWER_UP_SPEED,
    WARNING,
    SPAWN_MARKER,
    CRATE_ZONE;

    public static final int SIZE = 16;
    public static final int COLUMNS = 8;

    public int column() {
        return ordinal() % COLUMNS;
    }

    public int row() {
        return ordinal() / COLUMNS;
    }
}
