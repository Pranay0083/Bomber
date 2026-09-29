package com.blastarena.core.level;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/** Where saved levels live (the Repository pattern). Core only knows this interface, never files. */
public interface LevelRepository {

    /** Saves the level under its name, replacing any level with the same name. */
    void save(LevelData level) throws IOException;

    Optional<LevelData> load(String name) throws IOException, LevelFormatException;

    /** Names of every readable saved level, sorted. */
    List<String> list() throws IOException;

    /** Returns whether there was such a level to delete. */
    boolean delete(String name) throws IOException;

    /** Saves the level under a new name and removes the old one. */
    default void rename(String oldName, String newName) throws IOException, LevelFormatException {
        LevelData level = load(oldName).orElseThrow(() -> new IOException("No level named " + oldName));
        save(level.withName(newName));
        if (!sameFile(oldName, newName)) {
            delete(oldName);
        }
    }

    /** Whether two names would be stored in the same place; by default only if they are equal. */
    default boolean sameFile(String firstName, String secondName) {
        return firstName.equals(secondName);
    }
}
