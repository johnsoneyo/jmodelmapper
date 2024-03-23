package de.johnsoneyo.mapper.decorator;

import java.util.Objects;

/**
 * Integer to String type adapter provided
 */
public class IntegerToStringTypeAdapter implements TypeAdapter<Integer, String> {

    @Override
    public String convert(final Integer integer) {
        Objects.requireNonNull(integer);
        return String.valueOf(integer);
    }
}
