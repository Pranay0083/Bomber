package com.blastarena.core.debug;

import com.blastarena.core.board.Crate;
import com.blastarena.core.board.Floor;
import com.blastarena.core.board.SolidWall;
import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.model.Position;

/**
 * Draws the world as text, using the same symbols as the ASCII test boards:
 * {@code #} wall, {@code x} crate, {@code .} floor, {@code B} bomb, {@code *} fire, and a digit per living player.
 * When things share a tile, players show over fire, fire over bombs, and bombs over the floor.
 */
public final class TextRenderer {

    public String render(WorldView view) {
        StringBuilder text = new StringBuilder();
        for (int y = 0; y < view.height(); y++) {
            for (int x = 0; x < view.width(); x++) {
                text.append(symbolAt(view, new Position(x, y)));
            }
            text.append('\n');
        }
        return text.toString();
    }

    private static char symbolAt(WorldView view, Position position) {
        for (PlayerSnapshot player : view.players()) {
            if (player.alive() && player.position().equals(position)) {
                return Character.forDigit(player.id().id(), 10);
            }
        }
        if (view.isBurning(position)) {
            return '*';
        }
        if (view.bombAt(position).isPresent()) {
            return 'B';
        }
        return switch (view.tileAt(position)) {
            case SolidWall wall -> '#';
            case Crate crate -> 'x';
            case Floor floor -> '.';
        };
    }
}
