package com.blastarena.desktop.render;

import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import java.util.List;

/** Everything about the board that moves between ticks: where players are drawn, short effects, screen shake. */
public interface BoardAnimation {

    /** Where and how to draw a player: tile coordinates that may fall between tiles, facing, and walk step. */
    record Pose(float x, float y, Direction facing, int step) {
    }

    /** A short animation on one tile. {@code age} is in ticks, fractional between ticks. */
    record Effect(Kind kind, Position position, PlayerId player, float age) {

        public enum Kind {
            POOF,
            DEBRIS
        }
    }

    Pose pose(PlayerSnapshot player, float alpha);

    List<Effect> effects(float alpha);

    /** How far to nudge the board this frame, in tiles. */
    float[] shake(float alpha);

    /** Players standing on their tiles facing down, with nothing else going on. */
    static BoardAnimation still() {
        return new BoardAnimation() {
            @Override
            public Pose pose(PlayerSnapshot player, float alpha) {
                return new Pose(player.position().x(), player.position().y(), Direction.DOWN, 0);
            }

            @Override
            public List<Effect> effects(float alpha) {
                return List.of();
            }

            @Override
            public float[] shake(float alpha) {
                return new float[] {0f, 0f};
            }
        };
    }
}
