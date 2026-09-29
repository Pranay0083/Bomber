package com.blastarena.desktop.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import java.util.EnumMap;
import java.util.Map;

/**
 * The pixel-art sprites, cut out of sprites.png using the positions in sprites.txt.
 * Drawn with nearest filtering so pixels stay crisp when scaled up.
 */
public final class SpriteSheet implements Disposable {

    private final Texture texture;
    private final Map<Sprite, TextureRegion> regions = new EnumMap<>(Sprite.class);

    public SpriteSheet() {
        texture = new Texture(Gdx.files.classpath("sprites.png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        for (String line : Gdx.files.classpath("sprites.txt").readString("UTF-8").split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.trim().split("\\s+");
            regions.put(Sprite.valueOf(parts[0]), new TextureRegion(texture,
                    Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]), Integer.parseInt(parts[4])));
        }
        for (Sprite sprite : Sprite.values()) {
            if (!regions.containsKey(sprite)) {
                throw new IllegalStateException("sprites.png has no " + sprite + "; run tools/make_sprites.py");
            }
        }
    }

    public TextureRegion get(Sprite sprite) {
        return regions.get(sprite);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
