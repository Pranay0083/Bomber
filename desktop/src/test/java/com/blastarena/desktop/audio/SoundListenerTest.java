package com.blastarena.desktop.audio;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.event.BombExploded;
import com.blastarena.core.event.CrateDestroyed;
import com.blastarena.core.event.PlayerDied;
import com.blastarena.core.event.RoundEnded;
import com.blastarena.core.event.WallDropped;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.desktop.audio.SoundEffects.Effect;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SoundListenerTest {

    @Test
    void eachKindOfEventHasItsSoundAndQuietOnesHaveNone() {
        Position somewhere = new Position(1, 1);

        assertThat(SoundListener.effectFor(new BombExploded(new PlayerId(1), somewhere, Set.of(somewhere))))
                .contains(Effect.EXPLOSION);
        assertThat(SoundListener.effectFor(new PlayerDied(new PlayerId(2), somewhere))).contains(Effect.DEATH);
        assertThat(SoundListener.effectFor(new WallDropped(somewhere))).contains(Effect.WALL);
        assertThat(SoundListener.effectFor(RoundEnded.draw())).contains(Effect.WIN);
        assertThat(SoundListener.effectFor(new CrateDestroyed(somewhere))).isEmpty();
    }

    @Test
    void soundsAreSilentWithoutAudio() {
        SoundEffects silent = SoundEffects.load(false);

        silent.play(Effect.EXPLOSION);
        silent.toggleMute();

        assertThat(silent.isMuted()).isTrue();
    }
}
