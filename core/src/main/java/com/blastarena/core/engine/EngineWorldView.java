package com.blastarena.core.engine;

import com.blastarena.core.board.Tile;
import com.blastarena.core.control.BombSnapshot;
import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.entity.Bomb;
import com.blastarena.core.entity.Fire;
import com.blastarena.core.entity.Player;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Answers view questions from the live world, copying everything it hands out. */
final class EngineWorldView implements WorldView {

    private final GameWorld world;
    private final GameEngine engine;

    EngineWorldView(GameWorld world, GameEngine engine) {
        this.world = world;
        this.engine = engine;
    }

    @Override
    public GameConfig config() {
        return world.config();
    }

    @Override
    public long tick() {
        return engine.tickCount();
    }

    @Override
    public GamePhase phase() {
        return engine.phase();
    }

    @Override
    public int width() {
        return world.board().width();
    }

    @Override
    public int height() {
        return world.board().height();
    }

    @Override
    public boolean isInside(Position position) {
        return world.board().isInside(position);
    }

    @Override
    public Tile tileAt(Position position) {
        return world.board().tileAt(position);
    }

    @Override
    public List<PlayerSnapshot> players() {
        return world.players().stream().map(EngineWorldView::snapshot).toList();
    }

    @Override
    public Optional<PlayerSnapshot> player(PlayerId id) {
        return world.players().stream()
                .filter(player -> player.id().equals(id))
                .findFirst()
                .map(EngineWorldView::snapshot);
    }

    @Override
    public List<BombSnapshot> bombs() {
        return world.bombs().stream().map(EngineWorldView::snapshot).toList();
    }

    @Override
    public Optional<BombSnapshot> bombAt(Position position) {
        return world.bombAt(position).map(EngineWorldView::snapshot);
    }

    @Override
    public boolean isBurning(Position position) {
        return world.isBurning(position);
    }

    @Override
    public Set<Position> burningTiles() {
        Set<Position> burning = new LinkedHashSet<>();
        for (Fire fire : world.fires()) {
            burning.addAll(fire.tiles());
        }
        return Set.copyOf(burning);
    }

    private static PlayerSnapshot snapshot(Player player) {
        return new PlayerSnapshot(
                player.id(),
                player.position(),
                player.isAlive(),
                player.stats(),
                player.moveCooldown(),
                player.activeBombs());
    }

    private static BombSnapshot snapshot(Bomb bomb) {
        return new BombSnapshot(bomb.owner(), bomb.position(), bomb.range(), bomb.remainingFuse());
    }
}
