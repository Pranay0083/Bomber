package com.blastarena.core.testing;

import com.blastarena.core.control.Controller;
import com.blastarena.core.control.IdleController;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.engine.GameEngine;
import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.entity.Player;
import com.blastarena.core.event.SynchronousEventPublisher;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import java.util.HashMap;
import java.util.Map;

/** Live views of small ASCII worlds, for testing the bot helpers. Everyone stands still. */
public final class BotWorlds {

    /** Short fuse and fire so timings in tests stay readable. */
    public static final GameConfig CONFIG =
            GameConfig.builder().countdownTicks(0).fuseTicks(10).fireTicks(3).build();

    private BotWorlds() {
    }

    public static WorldView view(String... rows) {
        return engine(rows).view();
    }

    public static GameEngine engine(String... rows) {
        return engine(CONFIG, rows);
    }

    public static GameEngine engine(GameConfig config, String... rows) {
        GameWorld world = AsciiWorld.parse(config, rows);
        Map<PlayerId, Controller> controllers = new HashMap<>();
        for (Player player : world.players()) {
            controllers.put(player.id(), new IdleController(player.id()));
        }
        return new GameEngine(world, controllers, new SynchronousEventPublisher());
    }
}
