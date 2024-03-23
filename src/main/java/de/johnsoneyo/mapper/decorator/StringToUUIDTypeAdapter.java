package de.johnsoneyo.mapper.decorator;

import java.util.Objects;
import java.util.UUID;

/**
 * String to UUID adapter class provided
 */
public class StringToUUIDTypeAdapter implements TypeAdapter<String, UUID> {

    @Override
    public UUID convert(String string) {
        Objects.requireNonNull(string);
        return UUID.fromString(string);
    }
}
