package com.blastarena.desktop.storage;

import com.blastarena.core.level.LevelCodec;
import com.blastarena.core.level.LevelData;
import com.blastarena.core.level.LevelFormatException;
import com.blastarena.core.level.LevelRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps each level as a JSON file named after it, by default in {@code ~/.blastarena/levels/}.
 * Set the {@code BLASTARENA_LEVELS} environment variable to use another folder, for example while testing.
 */
public final class FileLevelRepository implements LevelRepository {

    private static final Logger LOG = LoggerFactory.getLogger(FileLevelRepository.class);
    private static final String EXTENSION = ".json";

    private final Path directory;
    private final LevelCodec codec;

    public FileLevelRepository(Path directory, LevelCodec codec) {
        this.directory = directory;
        this.codec = codec;
    }

    public static FileLevelRepository inHomeDirectory() {
        String override = System.getenv("BLASTARENA_LEVELS");
        Path directory = override != null && !override.isBlank()
                ? Path.of(override)
                : Path.of(System.getProperty("user.home"), ".blastarena", "levels");
        return new FileLevelRepository(directory, new LevelCodec());
    }

    public Path directory() {
        return directory;
    }

    @Override
    public void save(LevelData level) throws IOException {
        Files.createDirectories(directory);
        Path target = fileFor(level.name());
        // Write to a temporary file first so a crash never leaves a half-written level behind.
        Path temporary = Files.createTempFile(directory, "saving-", ".tmp");
        try {
            Files.writeString(temporary, codec.encode(level), StandardCharsets.UTF_8);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public Optional<LevelData> load(String name) throws IOException, LevelFormatException {
        Optional<Path> file = find(name);
        if (file.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(codec.decode(Files.readString(file.get(), StandardCharsets.UTF_8)));
    }

    @Override
    public List<String> list() throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.toString().endsWith(EXTENSION)).toList()) {
                try {
                    names.add(codec.decode(Files.readString(file, StandardCharsets.UTF_8)).name());
                } catch (LevelFormatException | IOException e) {
                    LOG.warn("Skipping unreadable level file {}: {}", file, e.getMessage());
                }
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    @Override
    public boolean delete(String name) throws IOException {
        Optional<Path> file = find(name);
        return file.isPresent() && Files.deleteIfExists(file.get());
    }

    /**
     * The file holding the level with this name: normally the one named after it, but a file copied in under
     * another name is found by reading the level names inside.
     */
    private Optional<Path> find(String name) throws IOException {
        Path expected = fileFor(name);
        if (Files.exists(expected)) {
            return Optional.of(expected);
        }
        if (!Files.isDirectory(directory)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.toString().endsWith(EXTENSION)).toList()) {
                try {
                    if (codec.decode(Files.readString(file, StandardCharsets.UTF_8)).name().equals(name)) {
                        return Optional.of(file);
                    }
                } catch (LevelFormatException | IOException e) {
                    // Unreadable files cannot hold the level we want.
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean sameFile(String firstName, String secondName) {
        return fileName(firstName).equals(fileName(secondName));
    }

    private Path fileFor(String name) {
        return directory.resolve(fileName(name));
    }

    /** Lower case letters, digits and dashes only, so any name makes a safe file name. */
    static String fileName(String name) {
        String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        return (slug.isEmpty() ? "level" : slug) + EXTENSION;
    }
}
