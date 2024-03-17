package de.johnsoneyo.mapper.decorator;

@FunctionalInterface
/**
 * This class is public and available for extension when decorating a field to transform {@link TransformToType} <INPUT> to be mapped to an <OUTPUT>
 * @see IntegerToStringTypeAdapter
 * @see StringToUUIDTypeAdapter
 */
public interface TypeAdapter <INPUT, OUTPUT>{

    /**
     *
     * @param input source
     * @return destination
     */
    OUTPUT convert(INPUT input);
}
