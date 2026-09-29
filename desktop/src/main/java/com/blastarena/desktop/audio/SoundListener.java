package com.blastarena.desktop.audio;

import com.blastarena.core.event.BombExploded;
import com.blastarena.core.event.BombPlaced;
import com.blastarena.core.event.CrateDestroyed;
import com.blastarena.core.event.GameEvent;
import com.blastarena.core.event.GameEventListener;
import com.blastarena.core.event.PlayerDied;
import com.blastarena.core.event.PlayerMoved;
import com.blastarena.core.event.PowerUpBurned;
import com.blastarena.core.event.PowerUpCollected;
import com.blastarena.core.event.PowerUpDropped;
import com.blastarena.core.event.RoundEnded;
import com.blastarena.core.event.WallDropped;
import com.blastarena.desktop.audio.SoundEffects.Effect;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * Plays a sound for each game event (an Observer: the engine never knows sound exists).
 * The same effect plays at most once per tick, so a chain of ten bombs is one bang, not ten stacked on top.
 */
public final class SoundListener implements GameEventListener {

    private final SoundEffects effects;
    private final LongSupplier currentTick;
    private final Set<Effect> playedThisTick = EnumSet.noneOf(Effect.class);
    private long tick = -1;

    public SoundListener(SoundEffects effects, LongSupplier currentTick) {
        this.effects = effects;
        this.currentTick = currentTick;
    }

    @Override
    public void onEvent(GameEvent event) {
        long now = currentTick.getAsLong();
        if (now != tick) {
            tick = now;
            playedThisTick.clear();
        }
        effectFor(event).filter(playedThisTick::add).ifPresent(effects::play);
    }

    static Optional<Effect> effectFor(GameEvent event) {
        Effect effect = switch (event) {
            case BombPlaced placed -> Effect.PLACE;
            case BombExploded exploded -> Effect.EXPLOSION;
            case PowerUpCollected collected -> Effect.PICKUP;
            case PlayerDied died -> Effect.DEATH;
            case WallDropped dropped -> Effect.WALL;
            case RoundEnded ended -> Effect.WIN;
            case PlayerMoved moved -> null;
            case CrateDestroyed destroyed -> null;
            case PowerUpDropped dropped -> null;
            case PowerUpBurned burned -> null;
        };
        return Optional.ofNullable(effect);
    }
}
