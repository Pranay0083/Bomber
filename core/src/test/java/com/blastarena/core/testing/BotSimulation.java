package com.blastarena.core.testing;

import com.blastarena.core.board.Board;
import com.blastarena.core.board.MapGenerator;
import com.blastarena.core.bot.BotController;
import com.blastarena.core.bot.Difficulty;
import com.blastarena.core.control.Controller;
import com.blastarena.core.engine.GameEngine;
import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.event.BombExploded;
import com.blastarena.core.event.BombPlaced;
import com.blastarena.core.event.PlayerDied;
import com.blastarena.core.event.SynchronousEventPublisher;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Plays bot-only rounds headless and reports who died and how. */
public final class BotSimulation {

    /**
     * How one round went.
     *
     * @param selfKills bots killed only by their own blast, with no other bomb placed while that bomb was ticking:
     *                  a mistake the bot made on its own
     * @param trapped   bots killed only by their own blast after an opponent dropped a bomb near them while it was
     *                  ticking, which can block the escape route; the opponent earned those
     */
    public record Result(long ticks, Set<PlayerId> survivors, Set<PlayerId> selfKills, Set<PlayerId> trapped,
                         List<PlayerId> deaths) {
    }

    /** An opponent's bomb this close to where a bot died could have cut off its escape. */
    private static final int INTERFERENCE_DISTANCE = 5;

    private BotSimulation() {
    }

    public static Result play(long seed, List<Difficulty> bots, int maxTicks) {
        GameConfig config = GameConfig.builder().seed(seed).countdownTicks(0).build();
        Board board = new MapGenerator().generate(config, new Random(seed));
        GameWorld world = GameWorld.withPlayersOnSpawns(config, board, bots.size());
        Map<PlayerId, Controller> controllers = new HashMap<>();
        for (int i = 0; i < bots.size(); i++) {
            PlayerId id = new PlayerId(i + 1);
            controllers.put(id, BotController.of(bots.get(i), id, seed * 131 + i));
        }
        SynchronousEventPublisher publisher = new SynchronousEventPublisher();
        GameEngine engine = new GameEngine(world, controllers, publisher);

        List<BombExploded> blastsThisTick = new ArrayList<>();
        long[] blastTick = {-1};
        Set<PlayerId> selfKills = new HashSet<>();
        Set<PlayerId> trapped = new HashSet<>();
        Map<Position, Long> placedAt = new HashMap<>();
        List<BombPlaced> placements = new ArrayList<>();
        List<Long> placementTicks = new ArrayList<>();
        List<PlayerId> deaths = new ArrayList<>();
        publisher.subscribe(event -> {
            if (blastTick[0] != engine.tickCount()) {
                blastsThisTick.clear();
                blastTick[0] = engine.tickCount();
            }
            if (event instanceof BombPlaced placed) {
                placedAt.put(placed.position(), engine.tickCount());
                placements.add(placed);
                placementTicks.add(engine.tickCount());
            } else if (event instanceof BombExploded blast) {
                blastsThisTick.add(blast);
            } else if (event instanceof PlayerDied died) {
                deaths.add(died.player());
                boolean own = blastsThisTick.stream().anyMatch(blast ->
                        blast.owner().equals(died.player()) && blast.fireTiles().contains(died.position()));
                boolean other = blastsThisTick.stream().anyMatch(blast ->
                        !blast.owner().equals(died.player()) && blast.fireTiles().contains(died.position()));
                if (own && !other) {
                    long since = blastsThisTick.stream()
                            .filter(blast -> blast.owner().equals(died.player())
                                    && blast.fireTiles().contains(died.position()))
                            .mapToLong(blast -> placedAt.getOrDefault(blast.position(), 0L))
                            .min().orElse(0L);
                    boolean interfered = false;
                    for (int i = 0; i < placements.size(); i++) {
                        BombPlaced nearby = placements.get(i);
                        if (placementTicks.get(i) >= since && !nearby.owner().equals(died.player())
                                && nearby.position().manhattanDistanceTo(died.position()) <= INTERFERENCE_DISTANCE) {
                            interfered = true;
                        }
                    }
                    (interfered ? trapped : selfKills).add(died.player());
                }
            }
        });

        while (!engine.isRoundOver() && engine.tickCount() < maxTicks) {
            engine.tick();
        }
        Set<PlayerId> survivors = new HashSet<>();
        engine.view().players().stream().filter(p -> p.alive()).forEach(p -> survivors.add(p.id()));
        return new Result(engine.tickCount(), survivors, selfKills, trapped, deaths);
    }
}
