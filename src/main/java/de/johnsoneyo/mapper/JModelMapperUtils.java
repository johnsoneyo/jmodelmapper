package de.johnsoneyo.mapper;

import de.johnsoneyo.mapper.decorator.ClassFieldMapping;
import de.johnsoneyo.mapper.decorator.SourceFieldMapping;
import de.johnsoneyo.mapper.decorator.TransformToType;
import de.johnsoneyo.mapper.decorator.TypeAdapter;
import de.johnsoneyo.mapper.exception.JModelMapperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 *
 */
final class JModelMapperUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JModelMapperUtils.class);
    private static final String GEN_ERROR_MESSAGE = "error occurred while mapping entity";

    /**
     * Holds class to supplier collection
     */
    private static final Map<Class, Supplier<Collection<Object>>> collectionFactory = ImmutableCollectionFactory.collectionFactory();

    /**
     * @param object      source object to be converted
     * @param outputClass destination class instance to map object
     *                    <p>
     *                    It is mandatory that the class has a no arg constructor, setters and getters are not necessary
     *                    as it uses reflection to set the fields
     *                    </p>
     * @param <INPUT>     source input param
     * @param <OUTPUT>    destination output param
     * @return mapped object
     * @throws JModelMapperException when matching fields are not of same data type
     * @see JModelMapperException#getCause() when constructor of class is not defined in destination class
     */
    static <INPUT, OUTPUT> OUTPUT map(final INPUT object, final Class<OUTPUT> outputClass) {

        Objects.requireNonNull(object, "source object is required and cannot be null");

        try {
            final OUTPUT output = outputClass.getDeclaredConstructor().newInstance();
            map(object, output);
            return output;
        } catch (Throwable throwable) {
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
    static <INPUT, OUTPUT> void map(final INPUT object, final OUTPUT output) {

        try {

            // break chain when object is null
            if (object == null) {
                return;
            }

            // break chain when class is a java runtime class
            if (object.getClass().getPackageName().startsWith("java")) {
                return;
            }

            Field[] fields = object.getClass().getDeclaredFields();
            if (fields.length == 0) {
                // break chain if there are no more fields in the class
                return;
            }

            // iterate through fields in the bean class
            for (Field field : fields) {
                Object obj = field.get(object);

                // check if field is a collection type
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

                            ParameterizedType collectionType = (ParameterizedType) outputField.getGenericType();

                            oput_ = Class.forName(collectionType.getActualTypeArguments()[0].getTypeName()).getDeclaredConstructor().newInstance();
                            Collection<Object> clctn = (Collection<Object>) outputField.get(output);
                            if (clctn != null) {
                                // set newly created object in list which is further updated by reference
                                /**
                                 * @see #map(Object, Object)
                                 */
                                clctn.add(oput_);
                            } else {  // initialize an empty collection when mapping null field
                                outputField.setAccessible(true);
                                Collection<Object> objectList = collectionFactory.get(outputField.getType()).get();
                                objectList.add(oput_);
                                outputField.set(output, objectList);
                            }
                        }

                        if (oput_ == null) continue;
                        map(o, oput_);
                    }
                } else {

                    Field outputField = getField(output, field.getName());

                    if (outputField == null && obj != null) {
                        if (obj.getClass().getPackageName().startsWith("java")) {

                            for (Field destinationField : output.getClass().getDeclaredFields()) {
                                Annotation[] annotations = destinationField.getDeclaredAnnotations();

                                if (Utils.isNotEmpty(annotations)) {

                                    for (Annotation annotation : annotations) {
                                        if (annotation instanceof ClassFieldMapping) {

                                            ClassFieldMapping classFieldMapping = (ClassFieldMapping) annotation;
                                            SourceFieldMapping[] sourceFieldMapping = classFieldMapping.fields();
                                            for (SourceFieldMapping sfm : sourceFieldMapping) {
                                                if (sfm.sourceField().equals(field.getName())) {
                                                    destinationField.set(output, obj);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Object customJavaObject = null;
                    if (outputField != null) {

                        outputField.setAccessible(true);
                        if (obj.getClass().getPackageName().startsWith("java")) {
                            Object updatedObj = updateField(outputField, obj);
                            // sets a java field in the object
                            outputField.set(output, updatedObj);
                        } else {
                            customJavaObject = outputField.getType().getDeclaredConstructor().newInstance();
                            // set a bean field in the object
                            outputField.set(output, customJavaObject);
                        }
                    }

                    // send a custom java object for further division
                    if (customJavaObject != null) {
                        map(obj, customJavaObject);
                    } else  // end iteration of jre object
                        map(obj, output);
                }
            }

        } catch (Exception exception) {
            LOG.error("error occurred mapping entity", exception);
            throw new JModelMapperException(GEN_ERROR_MESSAGE, exception);
        }
    }

    private static Object updateField(final Field outputField, final Object obj) {

        if (outputField.getType().equals(obj.getClass())) return obj;

        Annotation[] annotations = outputField.getDeclaredAnnotations();

        if (Utils.isNotEmpty(annotations)) {
            for (Annotation annot : annotations) {
                if (annot instanceof TransformToType) {
                    Class<? extends TypeAdapter> transformToType = ((TransformToType) annot).typeAdapter();
                    Method convertMethod = transformToType.getDeclaredMethods()[0];

                    try {
                        Object typeAdapterInstance = transformToType.getDeclaredConstructor().newInstance();
                        convertMethod.setAccessible(true);
                        return convertMethod.invoke(typeAdapterInstance, obj);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                             NoSuchMethodException e) {
                        LOG.error("updateField: error occurred in updating transforming field object", e);
                        throw new JModelMapperException("error occurred updating object in type adapter", e);
                    }
                }
            }
        }

        return obj;
    }


    private static Field getField(Object object, String fieldName) {
        try {
            return object.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            LOG.warn("field {} not found", fieldName);
            return null;
        }
    }


    /**
     * Defines a class collection instance in the factory method
     */
    static class ImmutableCollectionFactory {
        /**
         * @param <T>
         * @return lazy initialization of new immutable collection to be created
         */
        static <T> Map<Class, Supplier<Collection<T>>> collectionFactory() {
            return Map.of(List.class, () -> new ArrayList<>(),
                    Set.class, () -> new HashSet<>(),
                    LinkedList.class, () -> new LinkedList<>());
        }
    }

    /**
     * Helper class for commons logic
     */
    static class Utils {


        static <T> boolean isEmpty(T... t) {
            return t == null || t.length == 0;
        }

        static <T> boolean isNotEmpty(T... t) {
            return t != null && t.length > 0;
        }

    }

}
