package com.blastarena.desktop.render;

import com.badlogic.gdx.graphics.Color;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.powerup.PowerUpType;

/** Every colour the game draws with, kept in one place. */
public final class Palette {

    public static final Color BACKGROUND = Color.valueOf("1e232d");
    public static final Color FLOOR = Color.valueOf("3d6b4f");
    public static final Color FLOOR_ALT = Color.valueOf("386349");
    public static final Color WALL = Color.valueOf("6b7280");
    public static final Color WALL_LIGHT = Color.valueOf("9ca3af");
    public static final Color WALL_DARK = Color.valueOf("4b5563");
    public static final Color CRATE = Color.valueOf("a16207");
    public static final Color CRATE_DARK = Color.valueOf("713f12");
    public static final Color CRATE_ZONE = new Color(0.63f, 0.38f, 0.03f, 0.55f);
    public static final Color SPAWN = Color.valueOf("e5e7eb");
    public static final Color PROBLEM = Color.valueOf("ef4444");
    public static final Color GOOD = Color.valueOf("4ade80");
    public static final Color PREVIEW = new Color(1f, 1f, 1f, 0.35f);
    public static final Color BUTTON = Color.valueOf("1f2937");
    public static final Color BUTTON_SELECTED = Color.valueOf("2563eb");
    public static final Color BUTTON_DISABLED = Color.valueOf("1a2130");
    public static final Color BOMB = Color.valueOf("111827");
    public static final Color BOMB_FLASH = Color.valueOf("dc2626");
    public static final Color BOMB_SHINE = Color.valueOf("6b7280");
    public static final Color FIRE_OUTER = Color.valueOf("f97316");
    public static final Color FIRE_INNER = Color.valueOf("fde047");
    public static final Color POWER_UP_BASE = Color.valueOf("1f2937");
    public static final Color HUD = Color.valueOf("111827");
    public static final Color TEXT = Color.valueOf("f9fafb");
    public static final Color TEXT_DIM = Color.valueOf("9ca3af");
    public static final Color OVERLAY = new Color(0f, 0f, 0f, 0.55f);

    private static final Color[] PLAYERS = {
        Color.valueOf("f9fafb"), Color.valueOf("ef4444"), Color.valueOf("3b82f6"), Color.valueOf("facc15"),
    };

    private Palette() {
    }

    public static Color player(PlayerId id) {
        return PLAYERS[(id.id() - 1) % PLAYERS.length];
    }

    public static Color powerUp(PowerUpType type) {
        return switch (type) {
            case EXTRA_BOMB -> Color.valueOf("a78bfa");
            case BLAST_RANGE -> Color.valueOf("f87171");
            case SPEED -> Color.valueOf("22d3ee");
        };
    }
}
