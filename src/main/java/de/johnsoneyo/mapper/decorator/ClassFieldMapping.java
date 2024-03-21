package de.johnsoneyo.mapper.decorator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Class field decarator used for custom field mapping
 * ignores fields where the class is a standard pojo type
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ClassFieldMapping {

     SourceFieldMapping[] fields () default {};
}
