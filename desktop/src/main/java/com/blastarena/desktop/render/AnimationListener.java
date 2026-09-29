package com.blastarena.desktop.render;

import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.event.BombExploded;
import com.blastarena.core.event.CrateDestroyed;
import com.blastarena.core.event.GameEvent;
import com.blastarena.core.event.GameEventListener;
import com.blastarena.core.event.PlayerDied;
import com.blastarena.core.event.PlayerMoved;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns game events into movement on screen. The engine moves a player a whole tile in one tick; this spreads each
 * move over the player's move delay, so walking glides (drawing runs one move behind the rules, the usual price of
 * interpolation). It also remembers which way players face, plays short effects where players fall and crates
 * break, and shakes the board a little when bombs go off.
 */
public final class AnimationListener implements GameEventListener, BoardAnimation {

    /** Effects last this many ticks. */
    public static final int EFFECT_TICKS = 9;
    private static final int SHAKE_TICKS = 6;
    private static final float SHAKE_TILES = 0.06f;

    private record Slide(Position from, Position to, long startTick, int durationTicks) {
    }

    private record Started(Effect.Kind kind, Position position, PlayerId player, long tick) {
    }

    private final WorldView view;
    private final Map<PlayerId, Slide> slides = new HashMap<>();
    private final Map<PlayerId, Direction> facing = new HashMap<>();
    private final List<Started> effects = new ArrayList<>();
    private long lastBlastTick = Long.MIN_VALUE;

    public AnimationListener(WorldView view) {
        this.view = view;
    }

    @Override
    public void onEvent(GameEvent event) {
        switch (event) {
            case PlayerMoved moved -> {
                int duration = view.player(moved.player())
                        .map(player -> player.stats().moveDelayTicks())
                        .orElse(1);
                slides.put(moved.player(), new Slide(moved.from(), moved.to(), view.tick(), duration));
                facing.put(moved.player(), directionOf(moved.from(), moved.to()));
            }
            case PlayerDied died -> effects.add(new Started(Effect.Kind.POOF, died.position(), died.player(), view.tick()));
            case CrateDestroyed destroyed ->
                    effects.add(new Started(Effect.Kind.DEBRIS, destroyed.position(), null, view.tick()));
            case BombExploded exploded -> lastBlastTick = view.tick();
            default -> {
                // Other events do not move anything on screen.
            }
        }
    }

    /**
     * Where to draw each player right now.
     *
     * @param alpha how far we are into the tick after {@code view.tick()}, from 0 to 1
     */
    @Override
    public Pose pose(PlayerSnapshot player, float alpha) {
        Direction looking = facing.getOrDefault(player.id(), Direction.DOWN);
        Slide slide = slides.get(player.id());
        if (slide == null || !slide.to().equals(player.position())) {
            return new Pose(player.position().x(), player.position().y(), looking, 0);
        }
        float progress = Math.clamp((view.tick() - slide.startTick() + alpha) / slide.durationTicks(), 0f, 1f);
        // Lift a foot for the middle of each step.
        int step = progress > 0.25f && progress < 0.75f ? 1 : 0;
        return new Pose(
                slide.from().x() + (slide.to().x() - slide.from().x()) * progress,
                slide.from().y() + (slide.to().y() - slide.from().y()) * progress,
                looking, step);
    }

    @Override
    public List<Effect> effects(float alpha) {
        long now = view.tick();
        effects.removeIf(started -> now - started.tick() >= EFFECT_TICKS);
        return effects.stream()
                .map(started -> new Effect(started.kind(), started.position(), started.player(),
                        now - started.tick() + alpha))
                .toList();
    }

    @Override
    public float[] shake(float alpha) {
        float since = view.tick() - lastBlastTick + alpha;
        if (lastBlastTick == Long.MIN_VALUE || since >= SHAKE_TICKS) {
            return new float[] {0f, 0f};
        }
        float strength = SHAKE_TILES * (1f - since / SHAKE_TICKS);
        return new float[] {
            strength * (float) Math.sin(since * 9.1), strength * (float) Math.cos(since * 7.3),
        };
    }

    private static Direction directionOf(Position from, Position to) {
        for (Direction direction : Direction.values()) {
            if (from.step(direction).equals(to)) {
                return direction;
            }
        }
        return Direction.DOWN;
    }
}
