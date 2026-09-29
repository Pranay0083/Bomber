package com.blastarena.desktop.render;

import com.blastarena.core.control.PlayerSnapshot;

/** Where to draw a player, in tile coordinates that may fall between tiles while they move. */
@FunctionalInterface
public interface PlayerPlacement {

    /** Returns {x, y} in tiles. */
    float[] tileCoordinates(PlayerSnapshot player);

    /** Draws players exactly on their tiles, with no sliding. */
    static PlayerPlacement onTiles() {
        return player -> new float[] {player.position().x(), player.position().y()};
    }
}
