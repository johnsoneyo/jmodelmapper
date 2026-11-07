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
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
    private static final Map<Class<?>, Supplier<Collection<Object>>> collectionFactory = ImmutableCollectionFactory
            .collectionFactory();

    /**
     * @param object      source object to be converted
     * @param outputClass destination class instance to map object
     *                    <p>
     *                    It is mandatory that the class has a no arg constructor,
     *                    setters and getters are not necessary
     *                    as it uses reflection to set the fields
     *                    </p>
     * @param <INPUT>     source input param
     * @param <OUTPUT>    destination output param
     * @return mapped object
     * @throws JModelMapperException when matching fields are not of same data type
     * @see JModelMapperException#getCause() when constructor of class is not
     *      defined in destination class
     */
    static <INPUT, OUTPUT> OUTPUT map(final INPUT object, final Class<OUTPUT> outputClass) {

        Objects.requireNonNull(object, "source object is required and cannot be null");

        try {
            final OUTPUT output = outputClass.getDeclaredConstructor().newInstance();
            map(object, output);
            return output;
        } catch (ReflectiveOperationException | RuntimeException e) {
            Throwable rootCause = e.getCause() == null ? e : e.getCause();
            throw new JModelMapperException(GEN_ERROR_MESSAGE, rootCause);
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
        if (object == null || output == null) {
            return;
        }

        if (isJavaType(object.getClass())) {
            return;
        }

        for (Field field : getAllFields(object.getClass())) {
            if (field.isSynthetic()) {
                continue;
            }

            Object sourceValue;
            try {
                field.setAccessible(true);
                sourceValue = field.get(object);
            } catch (IllegalAccessException e) {
                throw new JModelMapperException(GEN_ERROR_MESSAGE, e);
            }

            if (sourceValue == null) {
                Field mappedField = getField(output.getClass(), field.getName());
                if (mappedField != null) {
                    try {
                        mappedField.setAccessible(true);
                        mappedField.set(output, null);
                    } catch (IllegalAccessException e) {
                        throw new JModelMapperException(GEN_ERROR_MESSAGE, e);
                    }
                }
                continue;
            }

            Field outputField = getField(output.getClass(), field.getName());
            if (outputField != null) {
                try {
                    outputField.setAccessible(true);
                    Object mappedValue = mapValue(outputField, sourceValue);
                    outputField.set(output, mappedValue);
                } catch (IllegalAccessException e) {
                    throw new JModelMapperException(GEN_ERROR_MESSAGE, e);
                }
                continue;
            }

            applyCustomFieldMappings(output, object, field.getName(), sourceValue);
        }

        applyCustomFieldMappings(output, object, null, null);
    }

    private static Object mapValue(final Field outputField, final Object sourceValue) {
        if (sourceValue == null) {
            return null;
        }

        if (sourceValue instanceof Collection) {
            return mapCollection(outputField, (Collection<?>) sourceValue);
        }

        if (isJavaType(sourceValue.getClass())) {
            return updateField(outputField, sourceValue);
        }

        if (outputField.getType().isAssignableFrom(sourceValue.getClass())) {
            return sourceValue;
        }

        Object targetValue = instantiateTargetValue(outputField.getType());
        if (targetValue == null) {
            return sourceValue;
        }

        map(sourceValue, targetValue);
        return targetValue;
    }

    private static Collection<Object> mapCollection(final Field outputField, final Collection<?> sourceCollection) {
        if (outputField == null) {
            return null;
        }

        Collection<Object> targetCollection = createCollection(outputField.getType());
        Class<?> elementType = resolveCollectionElementType(outputField);

        for (Object sourceElement : sourceCollection) {
            if (sourceElement == null) {
                continue;
            }

            Object targetElement = mapCollectionElement(elementType, sourceElement);
            if (targetElement != null) {
                targetCollection.add(targetElement);
            }
        }

        return targetCollection;
    }

    private static Object mapCollectionElement(final Class<?> elementType, final Object sourceElement) {
        if (sourceElement == null) {
            return null;
        }

        if (isJavaType(sourceElement.getClass())) {
            return sourceElement;
        }

        if (elementType != null && !elementType.equals(Object.class)
                && !elementType.isAssignableFrom(sourceElement.getClass())) {
            Object targetElement = instantiateTargetValue(elementType);
            if (targetElement != null) {
                map(sourceElement, targetElement);
                return targetElement;
            }
        }

        return sourceElement;
    }

    private static Object instantiateTargetValue(final Class<?> targetType) {
        if (targetType == null || targetType.isPrimitive() || targetType.isInterface()
                || Modifier.isAbstract(targetType.getModifiers())) {
            return null;
        }

        try {
            return targetType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            LOG.warn("unable to instantiate {} for mapping", targetType.getName(), e);
            return null;
        }
    }

    private static Collection<Object> createCollection(final Class<?> outputType) {
        if (Collection.class.isAssignableFrom(outputType)) {
            Supplier<Collection<Object>> supplier = collectionFactory.get(outputType);
            if (supplier != null) {
                return supplier.get();
            }
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }

    private static Class<?> resolveCollectionElementType(final Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            Type[] arguments = ((ParameterizedType) genericType).getActualTypeArguments();
            if (arguments.length > 0) {
                Type argument = arguments[0];
                if (argument instanceof Class) {
                    return (Class<?>) argument;
                }
                if (argument instanceof ParameterizedType) {
                    Type rawType = ((ParameterizedType) argument).getRawType();
                    if (rawType instanceof Class) {
                        return (Class<?>) rawType;
                    }
                }
            }
        }
        return Object.class;
    }

    private static void applyCustomFieldMappings(final Object output, final Object source, final String fieldName,
            final Object sourceValue) {
        for (Field destinationField : getAllFields(output.getClass())) {
            if (destinationField.isSynthetic()) {
                continue;
            }

            Annotation[] annotations = destinationField.getDeclaredAnnotations();
            if (Utils.isEmpty(annotations)) {
                continue;
            }

            for (Annotation annotation : annotations) {
                if (!(annotation instanceof ClassFieldMapping)) {
                    continue;
                }

                for (SourceFieldMapping mapping : ((ClassFieldMapping) annotation).fields()) {
                    if (fieldName != null && !fieldName.equals(mapping.sourceField())) {
                        continue;
                    }

                    Object value = sourceValue != null ? sourceValue : getFieldValue(source, mapping.sourceField());
                    if (value == null && sourceValue == null && !hasField(source, mapping.sourceField())) {
                        continue;
                    }

                    try {
                        destinationField.setAccessible(true);
                        destinationField.set(output, value);
                    } catch (IllegalAccessException e) {
                        throw new JModelMapperException(GEN_ERROR_MESSAGE, e);
                    }
                }
            }
        }
    }

    private static Object getFieldValue(final Object source, final String fieldName) {
        Field field = getField(source.getClass(), fieldName);
        if (field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(source);
        } catch (IllegalAccessException e) {
            throw new JModelMapperException(GEN_ERROR_MESSAGE, e);
        }
    }

    private static boolean hasField(final Object source, final String fieldName) {
        return getField(source.getClass(), fieldName) != null;
    }

    private static Object updateField(final Field outputField, final Object obj) {
        if (outputField == null || outputField.getType().isInstance(obj)) {
            return obj;
        }

        Annotation[] annotations = outputField.getDeclaredAnnotations();
        if (Utils.isNotEmpty(annotations)) {
            for (Annotation annot : annotations) {
                if (annot instanceof TransformToType) {
                    Class<? extends TypeAdapter> transformToType = ((TransformToType) annot).typeAdapter();
                    Method convertMethod = findConvertMethod(transformToType);

                    try {
                        Object typeAdapterInstance = transformToType.getDeclaredConstructor().newInstance();
                        convertMethod.setAccessible(true);
                        return convertMethod.invoke(typeAdapterInstance, obj);
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        LOG.error("updateField: error occurred in updating transforming field object", e);
                        throw new JModelMapperException("error occurred updating object in type adapter", e);
                    }
                }
            }
        }

        return obj;
    }

    private static Method findConvertMethod(final Class<? extends TypeAdapter> typeAdapter) {
        for (Method method : typeAdapter.getDeclaredMethods()) {
            if ("convert".equals(method.getName()) && method.getParameterCount() == 1) {
                return method;
            }
        }

        for (Method method : typeAdapter.getDeclaredMethods()) {
            if (method.getParameterCount() == 1) {
                return method;
            }
        }

        throw new JModelMapperException("type adapter must declare a single-parameter convert method", null);
    }

    private static Field getField(final Class<?> beanClass, final String fieldName) {
        for (Field field : getAllFields(beanClass)) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    private static List<Field> getAllFields(final Class<?> beanClass) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = beanClass;
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private static boolean isJavaType(final Class<?> valueClass) {
        return valueClass == null || valueClass.isPrimitive()
                || (valueClass.getPackageName() != null && valueClass.getPackageName().startsWith("java"));
    }

    /**
     * Defines a class collection instance in the factory method
     */
    static class ImmutableCollectionFactory {
        /**
         * @param <T>
         * @return lazy initialization of new immutable collection to be created
         */
        static <T> Map<Class<?>, Supplier<Collection<T>>> collectionFactory() {
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
