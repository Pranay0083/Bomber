package com.blastarena.desktop.render;

import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.event.GameEvent;
import com.blastarena.core.event.GameEventListener;
import com.blastarena.core.event.PlayerMoved;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import java.util.HashMap;
import java.util.Map;

/**
 * Slides players between tiles. The engine moves a player a whole tile in one tick; this remembers each move
 * and spreads it over the player's move delay, so a player walking steadily glides without stopping.
 * Drawing runs one move behind the rules, which is the usual price of interpolation.
 */
public final class AnimationListener implements GameEventListener {

    private record Slide(Position from, Position to, long startTick, int durationTicks) {
    }

    private final WorldView view;
    private final Map<PlayerId, Slide> slides = new HashMap<>();

    public AnimationListener(WorldView view) {
        this.view = view;
    }

    @Override
    public void onEvent(GameEvent event) {
        if (event instanceof PlayerMoved moved) {
            int duration = view.player(moved.player())
                    .map(player -> player.stats().moveDelayTicks())
                    .orElse(1);
            slides.put(moved.player(), new Slide(moved.from(), moved.to(), view.tick(), duration));
        }
    }

    /**
     * Where to draw each player right now.
     *
     * @param alpha how far we are into the tick after {@code view.tick()}, from 0 to 1
     */
    public PlayerPlacement placement(float alpha) {
        long now = view.tick();
        return player -> tileCoordinates(player, now, alpha);
    }

    private float[] tileCoordinates(PlayerSnapshot player, long now, float alpha) {
        Slide slide = slides.get(player.id());
        if (slide == null || !slide.to().equals(player.position())) {
            return new float[] {player.position().x(), player.position().y()};
        }
        float progress = Math.clamp((now - slide.startTick() + alpha) / slide.durationTicks(), 0f, 1f);
        return new float[] {
            slide.from().x() + (slide.to().x() - slide.from().x()) * progress,
            slide.from().y() + (slide.to().y() - slide.from().y()) * progress,
        };
    }
}
