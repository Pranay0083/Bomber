package com.blastarena.desktop.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.bot.BotController;
import com.blastarena.core.bot.Difficulty;
import com.blastarena.core.control.Controller;
import com.blastarena.core.engine.GameEngine;
import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.event.SynchronousEventPublisher;
import com.blastarena.core.level.Cell;
import com.blastarena.core.level.CommandHistory;
import com.blastarena.core.level.CustomLevelSource;
import com.blastarena.core.level.EditableLevel;
import com.blastarena.core.level.LevelChangeCommand;
import com.blastarena.core.level.LevelCodec;
import com.blastarena.core.level.LevelData;
import com.blastarena.core.level.LevelValidator;
import com.blastarena.core.level.MirrorTool;
import com.blastarena.core.level.Paint;
import com.blastarena.core.level.PaintCommand;
import com.blastarena.core.level.RectangleTool;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Build a level with the editor's tools, save it, load it as if after a restart, and play a round on it. */
class LevelRoundTripTest {

    @TempDir
    Path directory;

    @Test
    void aLevelBuiltInTheEditorCanBeSavedLoadedAndPlayed() throws Exception {
        EditableLevel level = EditableLevel.blank("Crossroads", 13, 11);
        CommandHistory history = new CommandHistory(level);
        history.execute(LevelChangeCommand.crateDensity(0.8));
        MirrorTool mirroredRectangle = new MirrorTool(new RectangleTool());
        history.execute(new PaintCommand(
                mirroredRectangle.cells(level, new Position(3, 1), new Position(5, 3)), new Paint.OfCell(Cell.CRATE_ZONE)));
        history.execute(new PaintCommand(
                List.of(new Position(6, 5)), new Paint.OfPowerUp(PowerUpType.BLAST_RANGE)));
        LevelData built = level.snapshot();
        assertThat(LevelValidator.standard().validate(built)).isEmpty();

        new FileLevelRepository(directory, new LevelCodec()).save(built);
        FileLevelRepository afterRestart = new FileLevelRepository(directory, new LevelCodec());
        LevelData loaded = afterRestart.load("Crossroads").orElseThrow();
        assertThat(loaded).isEqualTo(built);

        GameConfig config = GameConfig.builder().countdownTicks(0).seed(4L).build();
        GameWorld world = GameWorld.forArena(config, new CustomLevelSource(loaded).createArena(config), 4);
        Map<PlayerId, Controller> controllers = new HashMap<>();
        controllers.put(new PlayerId(1), BotController.of(Difficulty.HARD, new PlayerId(1), 1L));
        for (int id = 2; id <= 4; id++) {
            controllers.put(new PlayerId(id), BotController.of(Difficulty.MEDIUM, new PlayerId(id), id));
        }
        GameEngine engine = new GameEngine(world, controllers, new SynchronousEventPublisher());
        assertThat(engine.view().powerUpAt(new Position(6, 5))).contains(PowerUpType.BLAST_RANGE);
        while (!engine.isRoundOver() && engine.tickCount() < 6000) {
            engine.tick();
        }

        assertThat(engine.isRoundOver()).as("the round finished with a result").isTrue();
    }
}
