package com.blastarena.core.rules;

import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.entity.Player;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.Position;

/** Decides whether a player may step onto a tile right now. Players may share tiles with each other. */
public final class MovementValidator {

    public boolean canMove(GameWorld world, Player player, Direction direction) {
        return player.canMove() && canEnter(world, player, player.position().step(direction));
    }

    /**
     * A tile can be entered if it is on the board and walkable, and holds no bomb,
     * unless it is a bomb this player is still standing on and has not yet walked off.
     */
    public boolean canEnter(GameWorld world, Player player, Position target) {
        if (!world.board().isInside(target) || !world.board().tileAt(target).isWalkable()) {
            return false;
        }
        return world.bombAt(target)
                .map(bomb -> bomb.mayBeOccupiedBy(player.id()))
                .orElse(true);
    }
}
