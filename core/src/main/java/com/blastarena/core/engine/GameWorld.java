package com.blastarena.core.engine;

import com.blastarena.core.board.Board;
import com.blastarena.core.board.Destructible;
import com.blastarena.core.board.SolidWall;
import com.blastarena.core.board.Tile;
import com.blastarena.core.entity.Bomb;
import com.blastarena.core.entity.Fire;
import com.blastarena.core.entity.Player;
import com.blastarena.core.entity.PowerUpDrop;
import com.blastarena.core.level.Arena;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Everything that changes during a round: the board, players, bombs, fire and power-ups on the floor.
 * This is the only mutable game state. Rule classes read it and ask it to change;
 * it keeps the pieces consistent with each other (for example, a bomb's owner is told when it explodes).
 */
public final class GameWorld {

    private final GameConfig config;
    private final Board board;
    private final List<Player> players;
    private final List<Bomb> bombs = new ArrayList<>();
    private final List<Fire> fires = new ArrayList<>();
    private final List<PowerUpDrop> drops = new ArrayList<>();

    public GameWorld(GameConfig config, Board board, List<Player> players) {
        this.config = Objects.requireNonNull(config, "config");
        this.board = Objects.requireNonNull(board, "board");
        List<Player> sorted = new ArrayList<>(players);
        sorted.sort(Comparator.comparing(Player::id));
        this.players = Collections.unmodifiableList(sorted);
    }

    /** Creates players 1 to {@code playerCount} on the board's spawn points. */
    public static GameWorld withPlayersOnSpawns(GameConfig config, Board board, int playerCount) {
        List<Position> spawns = board.spawns();
        if (playerCount < 1 || playerCount > spawns.size()) {
            throw new IllegalArgumentException(
                    "Player count must be from 1 to " + spawns.size() + ", was " + playerCount);
        }
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            players.add(new Player(new PlayerId(i + 1), spawns.get(i)));
        }
        return new GameWorld(config, board, players);
    }

    /** Creates players 1 to {@code playerCount} on the arena's spawns, with its starting power-ups on the floor. */
    public static GameWorld forArena(GameConfig config, Arena arena, int playerCount) {
        GameWorld world = withPlayersOnSpawns(config, arena.board(), playerCount);
        arena.powerUps().forEach((position, type) -> world.addDrop(new PowerUpDrop(position, type.create())));
        return world;
    }

    public GameConfig config() {
        return config;
    }

    public Board board() {
        return board;
    }

    /** All players, alive or dead, in player-id order. */
    public List<Player> players() {
        return players;
    }

    public List<Player> livingPlayers() {
        return players.stream().filter(Player::isAlive).toList();
    }

    public Player player(PlayerId id) {
        return players.stream()
                .filter(player -> player.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No player " + id.id()));
    }

    /** Bombs in the order they were placed. */
    public List<Bomb> bombs() {
        return Collections.unmodifiableList(bombs);
    }

    public Optional<Bomb> bombAt(Position position) {
        return bombs.stream().filter(bomb -> bomb.position().equals(position)).findFirst();
    }

    public List<Fire> fires() {
        return Collections.unmodifiableList(fires);
    }

    public boolean isBurning(Position position) {
        return fires.stream().anyMatch(fire -> fire.covers(position));
    }

    /** Power-ups on the floor, in the order they appeared. */
    public List<PowerUpDrop> drops() {
        return Collections.unmodifiableList(drops);
    }

    public Optional<PowerUpDrop> dropAt(Position position) {
        return drops.stream().filter(drop -> drop.position().equals(position)).findFirst();
    }

    /** Puts a power-up on a walkable tile that has none yet. */
    public void addDrop(PowerUpDrop drop) {
        if (!board.tileAt(drop.position()).isWalkable()) {
            throw new IllegalStateException("A power-up needs a walkable tile, not " + board.tileAt(drop.position()));
        }
        if (dropAt(drop.position()).isPresent()) {
            throw new IllegalStateException("There is already a power-up at " + drop.position());
        }
        drops.add(drop);
    }

    public void removeDrop(PowerUpDrop drop) {
        if (!drops.remove(drop)) {
            throw new IllegalStateException(drop + " is not on the board");
        }
    }

    /** Hands the power-up to the player and takes it off the floor. */
    public void collect(Player player, PowerUpDrop drop) {
        removeDrop(drop);
        drop.powerUp().apply(player);
    }

    /** Moves a player and releases any bomb they were allowed to stand on. */
    public void movePlayer(Player player, Position target) {
        Position from = player.position();
        player.moveTo(target, player.stats().moveDelayTicks());
        bombAt(from).ifPresent(bomb -> bomb.playerLeft(player.id()));
    }

    public void addBomb(Bomb bomb) {
        if (bombAt(bomb.position()).isPresent()) {
            throw new IllegalStateException("There is already a bomb at " + bomb.position());
        }
        player(bomb.owner()).bombPlaced();
        bombs.add(bomb);
    }

    /** Removes an exploded bomb, hands the bomb back to its owner and lights its fire. */
    public void explode(Bomb bomb, Fire fire) {
        if (!bombs.remove(bomb)) {
            throw new IllegalStateException(bomb + " is not on the board");
        }
        player(bomb.owner()).bombExploded();
        fires.add(fire);
    }

    public void destroyTile(Position position) {
        Tile tile = board.tileAt(position);
        if (!(tile instanceof Destructible destructible)) {
            throw new IllegalStateException(tile + " at " + position + " cannot be destroyed");
        }
        board.setTile(position, destructible.destroy());
    }

    /**
     * Sudden death: turns the tile into solid wall. A bomb there is gone and its owner gets it back, a power-up
     * there is gone, and every living player standing there is crushed. Returns the players crushed.
     */
    public List<Player> dropWall(Position position) {
        bombAt(position).ifPresent(bomb -> {
            bombs.remove(bomb);
            player(bomb.owner()).bombExploded();
        });
        dropAt(position).ifPresent(drops::remove);
        board.setTile(position, new SolidWall());
        List<Player> crushed = livingPlayers().stream()
                .filter(player -> player.position().equals(position))
                .toList();
        crushed.forEach(Player::kill);
        return crushed;
    }

    public void tickCooldowns() {
        livingPlayers().forEach(Player::tickCooldown);
    }

    public void tickBombs() {
        bombs.forEach(Bomb::tick);
    }

    /**
     * Counts down the given fires and removes any that burned out. The engine passes the fires that existed
     * before this tick's explosions, so new fire burns for its full duration.
     */
    public void tickFires(Collection<Fire> existing) {
        existing.forEach(Fire::tick);
        fires.removeIf(Fire::isExpired);
    }

    public void killPlayer(Player player) {
        player.kill();
    }
}
