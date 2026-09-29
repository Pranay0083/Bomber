package com.blastarena.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class PlayerIdTest {

    @Test
    void rejectsIdsBelowOne() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PlayerId(0));
    }

    @Test
    void ordersById() {
        assertThat(new PlayerId(1)).isLessThan(new PlayerId(2));
    }
}
