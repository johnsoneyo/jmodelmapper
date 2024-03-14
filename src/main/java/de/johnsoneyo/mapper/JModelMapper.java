package de.johnsoneyo.mapper;

/**
 * Bean class used to create model mapper
 */
public class JModelMapper {
    /**
     *
     * @param input
     * @param outputClass
     * @return
     * @param <INPUT>
     * @param <OUTPUT>
     */
    public <INPUT, OUTPUT> OUTPUT map(INPUT input, Class<OUTPUT> outputClass) {
        return JModelMapperUtils.map(input, outputClass);
    }
}
