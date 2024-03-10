package de.johnsoneyo.mapper;

import de.johnsoneyo.mapper.exception.JModelMapperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public final class JModelMapper {

    private static final Logger LOG = LoggerFactory.getLogger(JModelMapper.class);
    private static final String GEN_ERROR_MESSAGE = "error occurred while mapping entity";

    /**
     *
     */
    private JModelMapper() {
    }


    /**
     * @param object
     * @param outputClass
     * @param <INPUT>
     * @param <OUTPUT>
     * @return
     * @throws JModelMapperException
     */
    public static <INPUT, OUTPUT> OUTPUT map(final INPUT object, final Class<OUTPUT> outputClass) {

        Objects.requireNonNull(object, "source object is required and cannot be null");

        try {
            final OUTPUT output = outputClass.getDeclaredConstructor().newInstance();
            map(object, output);
            return output;
        }catch (Throwable throwable) {
            throw new JModelMapperException(GEN_ERROR_MESSAGE, throwable.getCause());
        }
    }

    /**
     * @param object
     * @param output
     * @param <INPUT>
     * @param <OUTPUT>
     * @throws Exception
     */
    private static <INPUT, OUTPUT> void map(final INPUT object, final OUTPUT output) {

        try {

            if (object == null) {
                return;
            }

            if (object.getClass().getPackageName().startsWith("java")) {
                return;
            }

            Field[] fields = object.getClass().getDeclaredFields();
            if (fields.length == 0) {
                return;
            }

            for (Field field : fields) {
                Object obj = field.get(object);

                if (obj instanceof Collection) {
                    Collection<Object> collection = (Collection<Object>) obj;
                    for (Object o : collection) {
                        if (o == null) {
                            continue;
                        }
                        if (object.getClass().getPackageName().startsWith("java")) {
                            continue;
                        }

                        Field[] flds = object.getClass().getDeclaredFields();
                        if (flds.length == 0) {
                            continue;
                        }

                        Field outputField = getField(output, field.getName());

                        Object oput_ = null;
                        if (outputField != null && outputField.getType() == List.class) {

                            ParameterizedType listType = (ParameterizedType) outputField.getGenericType();

                            oput_ = Class.forName(listType.getActualTypeArguments()[0].getTypeName()).getDeclaredConstructor().newInstance();
                            List<Object> list = (List<Object>) outputField.get(output);
                            if (list != null) {
                                list.add(oput_);
                            } else {
                                outputField.setAccessible(true);
                                List<Object> objectList = new ArrayList<>();
                                objectList.add(oput_);
                                outputField.set(output, objectList);
                            }
                        }

                        if (oput_ == null)
                            continue;
                        map(o, oput_);
                    }
                } else {

                    Field outputField = getField(output, field.getName());
                    if (outputField != null) {

                        if(obj.getClass().getPackageName().startsWith("java")) {

                            outputField.setAccessible(true);
                            outputField.set(output, obj);
                        }
                    }
                    map(obj, output);
                }

            }

        } catch (Exception exception) {
            LOG.error("error occurred mapping entity", exception);
            throw  new JModelMapperException(GEN_ERROR_MESSAGE, exception);
        }
    }

    private static Field getField(Object object, String fieldName) {
        try {
            return object.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            LOG.warn("field {} not found", fieldName);
            return null;
        }
    }
}
