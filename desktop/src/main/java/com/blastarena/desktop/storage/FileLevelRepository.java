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

/** Keeps each level as a JSON file named after it, by default in {@code ~/.blastarena/levels/}. */
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
        return new FileLevelRepository(
                Path.of(System.getProperty("user.home"), ".blastarena", "levels"), new LevelCodec());
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
        Path file = fileFor(name);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(codec.decode(Files.readString(file, StandardCharsets.UTF_8)));
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
        return Files.deleteIfExists(fileFor(name));
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
