package com.blastarena.core.event;

/**
 * Something that happened during a tick. The engine collects events while it runs a tick
 * and publishes them at the end, so listeners never see a half-finished tick.
 */
public sealed interface GameEvent
        permits PlayerMoved, BombPlaced, BombExploded, CrateDestroyed, PowerUpDropped, PowerUpCollected,
                PowerUpBurned, PlayerDied, WallDropped, RoundEnded {
}
