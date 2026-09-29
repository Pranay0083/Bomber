package com.blastarena.desktop.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import java.util.EnumMap;
import java.util.Map;

/** The pixel-art sprites, cut out of sprites.png. Drawn with nearest filtering so pixels stay crisp when scaled. */
public final class SpriteSheet implements Disposable {

    private final Texture texture;
    private final Map<Sprite, TextureRegion> regions = new EnumMap<>(Sprite.class);

    public SpriteSheet() {
        texture = new Texture(Gdx.files.classpath("sprites.png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        for (Sprite sprite : Sprite.values()) {
            regions.put(sprite, new TextureRegion(texture,
                    sprite.column() * Sprite.SIZE, sprite.row() * Sprite.SIZE, Sprite.SIZE, Sprite.SIZE));
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
