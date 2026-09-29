package com.blastarena.desktop.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.level.EditableLevel;
import com.blastarena.core.level.LevelCodec;
import com.blastarena.core.level.LevelData;
import com.blastarena.core.level.LevelFormatException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileLevelRepositoryTest {

    @TempDir
    Path directory;

    private FileLevelRepository repository() {
        return new FileLevelRepository(directory.resolve("levels"), new LevelCodec());
    }

    private static LevelData level(String name) {
        return EditableLevel.blank(name, 9, 7).snapshot();
    }

    @Test
    void savedLevelsLoadBackExactly() throws IOException, LevelFormatException {
        LevelData level = level("Four Rooms");

        repository().save(level);

        assertThat(repository().load("Four Rooms")).contains(level);
        assertThat(Files.exists(directory.resolve("levels/four-rooms.json"))).isTrue();
    }

    @Test
    void listsLevelsByNameAndSkipsBrokenFiles() throws IOException {
        FileLevelRepository repository = repository();
        repository.save(level("beta"));
        repository.save(level("Alpha"));
        Files.writeString(repository.directory().resolve("broken.json"), "{ not a level");

        assertThat(repository.list()).containsExactly("Alpha", "beta");
    }

    @Test
    void savingAgainReplacesTheLevel() throws IOException, LevelFormatException {
        FileLevelRepository repository = repository();
        repository.save(level("Arena"));
        LevelData bigger = EditableLevel.blank("Arena", 11, 9).snapshot();

        repository.save(bigger);

        assertThat(repository.load("Arena")).contains(bigger);
        assertThat(repository.list()).containsExactly("Arena");
    }

    @Test
    void deleteAndRename() throws IOException, LevelFormatException {
        FileLevelRepository repository = repository();
        repository.save(level("Old"));

        repository.rename("Old", "New");
        assertThat(repository.list()).containsExactly("New");

        repository.rename("New", "NEW");
        assertThat(repository.list()).containsExactly("NEW");

        assertThat(repository.delete("NEW")).isTrue();
        assertThat(repository.delete("NEW")).isFalse();
        assertThat(repository.list()).isEmpty();
    }

    @Test
    void anEmptyOrMissingDirectoryHasNoLevels() throws IOException, LevelFormatException {
        assertThat(repository().list()).isEmpty();
        assertThat(repository().load("Nothing")).isEmpty();
    }

    @Test
    void anyNameMakesASafeFileName() {
        assertThat(FileLevelRepository.fileName("../../etc/passwd")).isEqualTo("etc-passwd.json");
        assertThat(FileLevelRepository.fileName("  ")).isEqualTo("level.json");
    }
}
