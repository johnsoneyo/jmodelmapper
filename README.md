# JModel Mapper

JModel Mapper is a lightweight Java 11 library for copying data between similar bean structures without requiring a heavy configuration model.

It is best suited for straightforward mapping scenarios where source and destination classes share field names or where a small amount of annotation-based customization is needed.

## Supported Java version

- Java 11+

## Installation

The library is published to GitHub Packages. Use the following dependency in your Maven project:

```xml
<dependency>
  <groupId>de.johnsoneyo</groupId>
  <artifactId>jmodel-mapper</artifactId>
  <version>1.0.0</version>
</dependency>
```

If you are pulling from GitHub Packages, add the repository as well:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/johnsoneyo/jmodelmapper</url>
  </repository>
</repositories>
```

For local development and testing from a checked-out copy, run:

```bash
./mvnw install
```

> The version shown above is the published release example. Local snapshot builds may use `1.0.0-SNAPSHOT` during development.

## Basic usage

```java
import de.johnsoneyo.mapper.JModelMapper;

public class Example {
    public static void main(String[] args) {
        JModelMapper mapper = new JModelMapper();

        Person person = new Person("Ada", 36, "female");
        PersonDto dto = mapper.map(person, PersonDto.class);

        System.out.println(dto.name); // Ada
    }
}

class Person {
    String name;
    Integer age;
    String sex;

    Person(String name, Integer age, String sex) {
        this.name = name;
        this.age = age;
        this.sex = sex;
    }
}

class PersonDto {
    String name;
    Integer age;
    String sex;
}
```

## Nested object mapping

```java
class Address {
    String street;
    String city;
}

class User {
    String username;
    Address address;
}

class AddressDto {
    String street;
    String city;
}

class UserDto {
    String username;
    AddressDto address;
}

JModelMapper mapper = new JModelMapper();
UserDto dto = mapper.map(user, UserDto.class);
```

## Collection mapping

```java
import java.util.List;

class Member {
    String name;
    List<Address> addresses;
}

class MemberDto {
    String name;
    List<AddressDto> addresses;
}

Member member = new Member();
member.name = "Ada";
member.addresses = List.of(new Address("Main Street", "London"));

MemberDto dto = mapper.map(member, MemberDto.class);
```

Null collection elements are skipped. Null collection fields remain null unless the destination field is explicitly initialized before or after mapping.

## Custom field mapping

Use `@ClassFieldMapping` when the destination field name differs from the source field name:

```java
import de.johnsoneyo.mapper.decorator.ClassFieldMapping;
import de.johnsoneyo.mapper.decorator.SourceFieldMapping;

class Request {
    String id;
    String requesterId;
}

class RequestDto {
    @ClassFieldMapping(fields = { @SourceFieldMapping(sourceField = "id") })
    String identifier;

    String requesterId;
}

Request request = new Request();
request.id = "abc";
request.requesterId = "uuid-123";

RequestDto dto = mapper.map(request, RequestDto.class);
```

## Type adapter mapping

Use `@TransformToType` when a source field must be converted to a different Java type:

```java
import de.johnsoneyo.mapper.decorator.TransformToType;
import de.johnsoneyo.mapper.decorator.TypeAdapter;

import java.util.Objects;
import java.util.UUID;

class StringToUuidAdapter implements TypeAdapter<String, UUID> {
    @Override
    public UUID convert(String value) {
        Objects.requireNonNull(value);
        return UUID.fromString(value);
    }
}

class Request {
    String requesterId;
}

class RequestDto {
    @TransformToType(typeAdapter = StringToUuidAdapter.class)
    UUID requesterId;
}
```

## Supported behavior

This library is intended for simple, reliable mapping use cases:

- same-name field matching between compatible bean properties
- nested bean mapping
- list/set collection mapping for simple nested elements
- annotation-based custom field resolution
- annotation-based type conversion via `TypeAdapter`
- Java 11 compatibility

## Explicit limitations

The mapper is intentionally small and conservative. It does not attempt to cover every Java mapping pattern.

- destination types must be instantiable with a no-arg constructor
- immutable objects or constructor-based mapping are not supported by default
- deep generic mapping is intentionally limited to common bean and collection patterns
- custom collection implementations beyond standard JDK collection types are not fully guaranteed
- it is not a full bean-validation or framework configuration system

## Local build and test

Run the project tests with:

```bash
./mvnw clean test
```

To install the project into your local Maven cache:

```bash
./mvnw install
```

## Notes

- The library is intentionally focused on backward compatibility and simplicity.
- Null values are handled safely instead of failing with unexpected `NullPointerException`s.
- Mapping failures still raise `JModelMapperException` with a useful cause chain for debugging.
