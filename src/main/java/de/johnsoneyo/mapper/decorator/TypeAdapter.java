package de.johnsoneyo.mapper.decorator;

@FunctionalInterface
/**
 * This class should be extended for field transformation {@link TransformToType} <INPUT> to be mapped to an <OUTPUT>
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
