package com.blastarena.core.rules;

import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.entity.Bomb;
import com.blastarena.core.entity.Player;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Places bombs under players, enforcing bomb capacity and one bomb per tile. */
public final class BombPlacer {

    public boolean canPlace(GameWorld world, Player player) {
        return player.hasBombAvailable() && world.bombAt(player.position()).isEmpty();
    }

    /**
     * Places a bomb on the player's tile with their current range and the configured fuse.
     * Everyone standing on that tile may walk off it. Returns empty if the player cannot place one.
     */
    public Optional<Bomb> place(GameWorld world, Player player) {
        if (!canPlace(world, player)) {
            return Optional.empty();
        }
        Position position = player.position();
        Set<PlayerId> standingOnIt = world.livingPlayers().stream()
                .filter(other -> other.position().equals(position))
                .map(Player::id)
                .collect(Collectors.toSet());
        Bomb bomb = new Bomb(player.id(), position, player.stats().blastRange(), world.config().fuseTicks(), standingOnIt);
        world.addBomb(bomb);
        return Optional.of(bomb);
    }
}
