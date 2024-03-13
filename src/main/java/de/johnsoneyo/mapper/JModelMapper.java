package de.johnsoneyo.mapper;

/**
 * Bean class used to create model mapper
 */
public class JModelMapper {

    public JModelMapper() {
    }

    /**
     * @param input
     * @param outputClass
     * @param <INPUT>
     * @param <OUTPUT>
     * @return
     */
    public <INPUT, OUTPUT> OUTPUT map(INPUT input, Class<OUTPUT> outputClass) {
        return JModelMapperUtils.map(input, outputClass);
    }
}
