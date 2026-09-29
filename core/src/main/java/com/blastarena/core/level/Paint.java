package com.blastarena.core.level;

import com.blastarena.core.powerup.PowerUpType;

/** What the editor paints with: a kind of cell, or a power-up placed on top of one. */
public sealed interface Paint {

    record OfCell(Cell cell) implements Paint {
    }

    record OfPowerUp(PowerUpType type) implements Paint {
    }

    /** Removes power-ups and leaves cells alone. */
    record RemovePowerUp() implements Paint {
    }
}
