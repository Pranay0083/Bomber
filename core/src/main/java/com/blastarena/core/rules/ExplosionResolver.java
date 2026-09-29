package com.blastarena.core.rules;

import com.blastarena.core.board.Board;
import com.blastarena.core.board.Destructible;
import com.blastarena.core.board.Tile;
import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.entity.Bomb;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.Position;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Works out what a blast reaches, without changing the world.
 * A blast spreads from the bomb in all four directions up to its range. It stops before a solid wall,
 * and stops on the first destructible tile it hits after burning it. Other bombs do not stop it;
 * they are triggered.
 */
public final class ExplosionResolver {

    /** Bombs that go off this tick: those whose fuse ran out and those sitting in fire, in placement order. */
    public List<Bomb> bombsToExplode(GameWorld world) {
        return world.bombs().stream()
                .filter(bomb -> bomb.isDue() || world.isBurning(bomb.position()))
                .toList();
    }

    /**
     * Explodes the given bombs and every bomb their blasts trigger, until no new bomb is caught.
     * Each bomb explodes once, in the order it was reached. Crates are not removed in between,
     * so every blast in the chain is stopped by the same crates.
     */
    public List<Explosion> resolveChain(GameWorld world, List<Bomb> initial) {
        List<Explosion> explosions = new ArrayList<>();
        Set<Bomb> reached = new HashSet<>(initial);
        Deque<Bomb> pending = new ArrayDeque<>(initial);
        while (!pending.isEmpty()) {
            Explosion explosion = resolve(world, pending.removeFirst());
            explosions.add(explosion);
            for (Bomb caught : explosion.triggered()) {
                if (reached.add(caught)) {
                    pending.addLast(caught);
                }
            }
        }
        return explosions;
    }

    public Explosion resolve(GameWorld world, Bomb bomb) {
        Board board = world.board();
        Set<Position> fire = new LinkedHashSet<>();
        Set<Position> cratesHit = new LinkedHashSet<>();
        List<Bomb> triggered = new ArrayList<>();

        fire.add(bomb.position());
        for (Direction direction : Direction.values()) {
            Position position = bomb.position();
            for (int step = 1; step <= bomb.range(); step++) {
                position = position.step(direction);
                if (!board.isInside(position)) {
                    break;
                }
                Tile tile = board.tileAt(position);
                if (tile.blocksBlast()) {
                    if (tile instanceof Destructible) {
                        fire.add(position);
                        cratesHit.add(position);
                    }
                    break;
                }
                fire.add(position);
                Optional<Bomb> caught = world.bombAt(position);
                caught.ifPresent(triggered::add);
            }
        }
        return new Explosion(bomb, fire, cratesHit, triggered);
    }
}
