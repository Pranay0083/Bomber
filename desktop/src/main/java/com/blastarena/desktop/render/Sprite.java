package com.blastarena.desktop.render;

import com.blastarena.core.model.Direction;

/** Every sprite in sprites.png, looked up by name in sprites.txt; both are written by tools/make_sprites.py. */
public enum Sprite {
    FLOOR_A,
    FLOOR_B,
    WALL,
    CRATE,
    BOMB_0,
    BOMB_1,
    POWER_UP_BOMB,
    POWER_UP_RANGE,
    POWER_UP_SPEED,
    WARNING,
    SPAWN_MARKER,
    CRATE_ZONE,
    SHADOW,
    FLOOR_SHADOW,
    FIRE_CENTRE_0,
    FIRE_CENTRE_1,
    FIRE_CENTRE_2,
    FIRE_CENTRE_3,
    FIRE_ARM_0,
    FIRE_ARM_1,
    FIRE_ARM_2,
    FIRE_ARM_3,
    FIRE_END_0,
    FIRE_END_1,
    FIRE_END_2,
    FIRE_END_3,
    PLAYER_DOWN_0,
    PLAYER_DOWN_1,
    PLAYER_UP_0,
    PLAYER_UP_1,
    PLAYER_LEFT_0,
    PLAYER_LEFT_1,
    PLAYER_RIGHT_0,
    PLAYER_RIGHT_1,
    POOF_0,
    POOF_1,
    POOF_2,
    DEBRIS_0,
    DEBRIS_1,
    DEBRIS_2;

    public static Sprite fire(FirePiece piece, int frame) {
        return valueOf("FIRE_" + piece.name() + "_" + frame);
    }

    public static Sprite player(Direction facing, int step) {
        return valueOf("PLAYER_" + facing.name() + "_" + step);
    }

    /** The three kinds of explosion tile, drawn turned to fit how the blast runs. */
    public enum FirePiece {
        CENTRE,
        ARM,
        END
    }
}
