## JModel Mapper

JModel Mapper is an open source library used to map simple to deep object nesting with similar object data structure without the need of accessors or mutators

## Maven
```xml

```

## Kotlin
- Under Development


### Assumptions
- Data fields in the both objects are homogeneous by name and data type
- Destination classes
  - Has a non argument constructor and doesn't require immutable creation, required for no argument class object construction.
  - Has getter fields for returning the values atleast ( no mandatory )

### Limitations
- Has no provision for custom field mapping using a supplier

### Usage

- Create bean instance or a singleton
```java
JModelMapper modelMapper = new JModelMapper();
```
- Then supply source object and destination class
```java
  Person expected = new Person(name, age, sex, List.of(new Person.Address("test-street-1", "test-zipcode-1",
        new Person.Address.ExtraInfo("100010000.0000")),
        new Person.Address("test-street-2", "test-zipcode-2", new Person.Address.ExtraInfo("test-extra-info"))),
        Map.of("key", "value"));

PersonDto actual = modelMapper.map(expected, PersonDto.class);
```
- Use Type Transformers if you want to map same name field to different data type

```java

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Type Adapter Example
 */
public class LocalDateTimeToOffsetDateTimeTypeAdapter implements TypeAdapter<LocalDate, OffsetDateTime> {

  /**
   * 
    * @param localDateTime input parameter to be converted
   * @return
   */  
  @Override
  public OffsetDateTime convert(LocalDateTime localDateTime) {
    Objects.requireNonNull(localDateTime);
    ZoneOffset offset = OffsetDateTime.now().getOffset();
    OffsetDateTime offsetDateTime = localDateTime.atOffset(offset);
    return offsetDateTime;
  }
}

Then decorate class field

        @TransformToType(typeAdapter = LocalDateTimeToOffsetDateTimeTypeAdapter.class)
        OffsetDateTime localDateTime;
```
