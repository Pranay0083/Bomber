package com.blastarena.core.level;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

/** Turns levels into JSON and back. Newer files are refused rather than half-read. */
public final class LevelCodec {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public String encode(LevelData level) {
        try {
            return mapper.writeValueAsString(level);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not write level " + level.name(), e);
        }
    }

    public LevelData decode(String json) throws LevelFormatException {
        LevelData level;
        try {
            level = mapper.readValue(json, LevelData.class);
        } catch (ValueInstantiationException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new LevelFormatException("Level data does not fit together: " + cause.getMessage(), e);
        } catch (JsonProcessingException e) {
            throw new LevelFormatException("Not a level file: " + e.getOriginalMessage(), e);
        }
        if (level.version() < 1 || level.version() > LevelData.CURRENT_VERSION) {
            throw new LevelFormatException("Level format version " + level.version() + " is not supported");
        }
        return level;
    }
}
