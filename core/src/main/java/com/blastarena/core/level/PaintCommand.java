package com.blastarena.core.level;

import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Paints a set of cells in one undoable step. Painting a cell that cannot hold a power-up clears the power-up there;
 * a power-up is only placed where it may lie (floor or crate zone).
 */
public final class PaintCommand implements EditorCommand {

    private record Before(Cell cell, Optional<PowerUpType> powerUp) {
    }

    private final List<Position> positions;
    private final Paint paint;
    private final Map<Position, Before> before = new LinkedHashMap<>();

    public PaintCommand(List<Position> positions, Paint paint) {
        this.positions = List.copyOf(positions);
        this.paint = paint;
    }

    @Override
    public boolean execute(EditableLevel level) {
        before.clear();
        boolean changed = false;
        for (Position position : positions) {
            if (!level.isInside(position) || before.containsKey(position)) {
                continue;
            }
            Before was = new Before(level.cellAt(position), level.powerUpAt(position));
            before.put(position, was);
            apply(level, position);
            changed |= !was.equals(new Before(level.cellAt(position), level.powerUpAt(position)));
        }
        return changed;
    }

    private void apply(EditableLevel level, Position position) {
        switch (paint) {
            case Paint.OfCell(Cell cell) -> {
                level.setCell(position, cell);
                if (!canHoldPowerUp(cell)) {
                    level.setPowerUp(position, null);
                }
            }
            case Paint.OfPowerUp(PowerUpType type) -> {
                if (canHoldPowerUp(level.cellAt(position))) {
                    level.setPowerUp(position, type);
                }
            }
            case Paint.RemovePowerUp() -> level.setPowerUp(position, null);
        }
    }

    private static boolean canHoldPowerUp(Cell cell) {
        return cell == Cell.FLOOR || cell == Cell.CRATE_ZONE;
    }

    @Override
    public void undo(EditableLevel level) {
        before.forEach((position, was) -> {
            level.setCell(position, was.cell());
            level.setPowerUp(position, was.powerUp().orElse(null));
        });
    }
}
