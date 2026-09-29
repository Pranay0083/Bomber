package com.blastarena.core.level;

/** A level file that cannot be read: bad JSON, an unknown version, or data that does not fit together. */
public class LevelFormatException extends Exception {

    public LevelFormatException(String message) {
        super(message);
    }

    public LevelFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
