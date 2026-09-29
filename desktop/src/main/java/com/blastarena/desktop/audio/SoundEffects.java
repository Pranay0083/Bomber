package com.blastarena.desktop.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The game's sound effects, loaded once. Can be muted, and is silent when audio is turned off. */
public final class SoundEffects implements Disposable {

    private static final Logger LOG = LoggerFactory.getLogger(SoundEffects.class);

    /** Each effect and the file it is made from (see tools/make_sounds.py). */
    public enum Effect {
        PLACE("place", 0.6f),
        EXPLOSION("explosion", 0.8f),
        PICKUP("pickup", 0.6f),
        DEATH("death", 0.7f),
        WALL("wall", 0.5f),
        WIN("win", 0.7f);

        private final String file;
        private final float volume;

        Effect(String file, float volume) {
            this.file = file;
            this.volume = volume;
        }
    }

    private final Map<Effect, Sound> sounds = new EnumMap<>(Effect.class);
    private boolean muted;

    private SoundEffects() {
    }

    /** Loads every effect, or returns a silent set if audio is off or a file cannot be loaded. */
    public static SoundEffects load(boolean enabled) {
        SoundEffects effects = new SoundEffects();
        if (!enabled || Gdx.audio == null) {
            return effects;
        }
        try {
            for (Effect effect : Effect.values()) {
                effects.sounds.put(effect, Gdx.audio.newSound(Gdx.files.classpath("sounds/" + effect.file + ".wav")));
            }
        } catch (RuntimeException e) {
            LOG.warn("Sound is off: {}", e.getMessage());
            effects.dispose();
            effects.sounds.clear();
        }
        return effects;
    }

    public void play(Effect effect) {
        Sound sound = sounds.get(effect);
        if (sound != null && !muted) {
            sound.play(effect.volume);
        }
    }

    /** Whether the sound files were loaded; false when audio is off. */
    public boolean isLoaded() {
        return !sounds.isEmpty();
    }

    public boolean isMuted() {
        return muted;
    }

    public void toggleMute() {
        muted = !muted;
    }

    @Override
    public void dispose() {
        sounds.values().forEach(Sound::dispose);
    }
}
