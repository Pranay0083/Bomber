package com.blastarena.core.engine;

import com.blastarena.core.model.Position;
import java.util.ArrayList;
import java.util.List;

/** The order sudden-death walls fill the arena: inside the border, clockwise, ring by ring towards the middle. */
public final class WallSpiral {

    private WallSpiral() {
    }

    public static List<Position> order(int width, int height) {
        List<Position> order = new ArrayList<>();
        int left = 1;
        int top = 1;
        int right = width - 2;
        int bottom = height - 2;
        while (left <= right && top <= bottom) {
            for (int x = left; x <= right; x++) {
                order.add(new Position(x, top));
            }
            for (int y = top + 1; y <= bottom; y++) {
                order.add(new Position(right, y));
            }
            if (top < bottom) {
                for (int x = right - 1; x >= left; x--) {
                    order.add(new Position(x, bottom));
                }
            }
            if (left < right) {
                for (int y = bottom - 1; y > top; y--) {
                    order.add(new Position(left, y));
                }
            }
            left++;
            top++;
            right--;
            bottom--;
        }
        return order;
    }
}
