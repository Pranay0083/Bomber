package com.blastarena.core;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CoreIsolationTest {

    @Test
    void libGdxIsNotOnTheCoreClasspath() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("com.badlogic.gdx.Gdx"));
    }
}
