package com.blastarena.core.engine;

import com.blastarena.core.command.Command;
import com.blastarena.core.command.CommandContext;
import com.blastarena.core.command.IdleCommand;
import com.blastarena.core.control.Controller;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.entity.Fire;
import com.blastarena.core.entity.Player;
import com.blastarena.core.entity.PowerUpDrop;
import com.blastarena.core.event.BombExploded;
import com.blastarena.core.event.CrateDestroyed;
import com.blastarena.core.event.EventPublisher;
import com.blastarena.core.event.GameEvent;
import com.blastarena.core.event.PlayerDied;
import com.blastarena.core.event.PowerUpBurned;
import com.blastarena.core.event.PowerUpCollected;
import com.blastarena.core.event.PowerUpDropped;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.rules.Explosion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a round one fixed tick at a time. This is the facade the client uses: it calls {@link #tick()}
 * and reads {@link #view()}, and never touches the rule classes or the world directly.
 *
 * <p>Each running tick follows the same order, which keeps rounds deterministic:
 * <ol>
 *   <li>Count down move cooldowns, then ask every living player's controller for one command.</li>
 *   <li>Run the commands in player-id order: all moves first, then all bomb placements.</li>
 *   <li>Pick up any power-up on a tile a player now stands on.</li>
 *   <li>Count down every bomb's fuse.</li>
 *   <li>Explode due bombs and bombs sitting in fire, then everything they trigger.</li>
 *   <li>Burn power-ups the blasts reached, then destroy the crates they hit and roll a drop for each.</li>
 *   <li>Count down the fire that existed before this tick and remove what burned out.</li>
 *   <li>Kill players standing in fire.</li>
 *   <li>Let the current phase move on (round timer, win check).</li>
 *   <li>Publish every event collected during the tick.</li>
 * </ol>
 */
public final class GameEngine {

    private static final Logger LOG = LoggerFactory.getLogger(GameEngine.class);

    private final GameWorld world;
    private final Map<PlayerId, Controller> controllers;
    private final EventPublisher publisher;
    private final Rules rules;
    private final WorldView view;
    private GamePhase phase;
    private long tickCount;

    public GameEngine(GameWorld world, Map<PlayerId, Controller> controllers, EventPublisher publisher) {
        this(world, controllers, publisher, Rules.standard(world.config()));
    }

    public GameEngine(GameWorld world, Map<PlayerId, Controller> controllers, EventPublisher publisher, Rules rules) {
        this.world = Objects.requireNonNull(world, "world");
        this.controllers = Map.copyOf(controllers);
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.rules = Objects.requireNonNull(rules, "rules");
        for (Player player : world.players()) {
            if (!this.controllers.containsKey(player.id())) {
                throw new IllegalArgumentException("No controller for player " + player.id().id());
            }
        }
        this.view = new EngineWorldView(world, this);
        this.phase = GamePhase.initial(world.config());
    }

    public WorldView view() {
        return view;
    }

    public GamePhase phase() {
        return phase;
    }

    public long tickCount() {
        return tickCount;
    }

    public boolean isRoundOver() {
        return phase instanceof RoundOver;
    }

    /** Advances the round by one tick. Does nothing once the round is over. */
    public void tick() {
        if (isRoundOver()) {
            return;
        }
        tickCount++;
        List<GameEvent> events = new ArrayList<>();

        if (phase.isRunning()) {
            runCommands(collectCommands(), events);
            collectPowerUps(events);
            List<Fire> existingFire = List.copyOf(world.fires());
            world.tickBombs();
            explodeBombs(events);
            world.tickFires(existingFire);
            killPlayersInFire(events);
        }

        GamePhase next = phase.next(new PhaseContext(world, rules.winConditionChecker()));
        if (next instanceof RoundOver over) {
            events.add(over.result());
            LOG.debug("Round over after {} ticks: {}", tickCount, over.result());
        }
        phase = next;

        events.forEach(publisher::publish);
    }

    private List<Command> collectCommands() {
        world.tickCooldowns();
        List<Command> commands = new ArrayList<>();
        for (Player player : world.livingPlayers()) {
            Command command = controllers.get(player.id()).nextCommand(view);
            if (command == null || !command.player().equals(player.id())) {
                LOG.warn("Controller for player {} sent {}; treating it as idle", player.id().id(), command);
                command = new IdleCommand(player.id());
            }
            commands.add(command);
        }
        return commands;
    }

    private void runCommands(List<Command> commands, List<GameEvent> events) {
        CommandContext context = new CommandContext(world, rules.movementValidator(), rules.bombPlacer(), events::add);
        commands.stream()
                .sorted(Comparator.comparing(Command::stage).thenComparing(Command::player))
                .forEach(command -> command.execute(context));
    }

    private void collectPowerUps(List<GameEvent> events) {
        for (Player player : world.livingPlayers()) {
            world.dropAt(player.position()).ifPresent(drop -> {
                world.collect(player, drop);
                events.add(new PowerUpCollected(player.id(), drop.powerUp().type(), drop.position()));
            });
        }
    }

    private void explodeBombs(List<GameEvent> events) {
        var resolver = rules.explosionResolver();
        List<Explosion> chain = resolver.resolveChain(world, resolver.bombsToExplode(world));
        for (Explosion explosion : chain) {
            world.explode(explosion.bomb(), new Fire(explosion.fireTiles(), world.config().fireTicks()));
            events.add(new BombExploded(explosion.bomb().owner(), explosion.bomb().position(), explosion.fireTiles()));
        }
        burnPowerUps(chain, events);
        List<Position> crates = chain.stream()
                .flatMap(explosion -> explosion.cratesHit().stream())
                .distinct()
                .toList();
        for (Position crate : crates) {
            world.destroyTile(crate);
            events.add(new CrateDestroyed(crate));
            rules.powerUpFactory().rollDrop().ifPresent(powerUp -> {
                world.addDrop(new PowerUpDrop(crate, powerUp));
                events.add(new PowerUpDropped(crate, powerUp.type()));
            });
        }
    }

    /** Only power-ups already on the floor burn; one revealed by this tick's blast survives it. */
    private void burnPowerUps(List<Explosion> chain, List<GameEvent> events) {
        Set<Position> blasted = new HashSet<>();
        chain.forEach(explosion -> blasted.addAll(explosion.fireTiles()));
        List<PowerUpDrop> burned = world.drops().stream()
                .filter(drop -> blasted.contains(drop.position()))
                .toList();
        for (PowerUpDrop drop : burned) {
            world.removeDrop(drop);
            events.add(new PowerUpBurned(drop.position(), drop.powerUp().type()));
        }
    }

    private void killPlayersInFire(List<GameEvent> events) {
        for (Player player : rules.deathChecker().playersInFire(world)) {
            world.killPlayer(player);
            events.add(new PlayerDied(player.id(), player.position()));
        }
    }
}
