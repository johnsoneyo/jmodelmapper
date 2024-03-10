## JModel Mapper

JModel Mapper is an open source library used to map simple to deep object nesting with similar object data structure without the need of accessors or mutators

## Maven


### Assumptions
- Data fields in the both objects are homogeneous by name and data type
- Destination classes
  - Has a non argument constructor and doesn't require immutable creation, required for no argument class object construction.
  - Has getter fields for returning the values atleast ( no mandatory )

### Limitations
- Has no provision for custom field mapping using a supplier

### Code Examples 
To use simply call the class `JModelMapper#map` and supply the source and destination class
```java

        // given
        String name = "test-name";
        Integer age = 1;
        String sex = "test-sex";
        Person expected = new Person(name, age, sex, List.of(new Person.Address("test-street-1", "test-zipcode-1",
                        new Person.Address.ExtraInfo("trrr")),
                new Person.Address("test-street-2", "test-zipcode-2", new Person.Address.ExtraInfo("test-extra-info"))),
                Map.of("key", "value"));

        //when
        PersonDto actual = JModelMapper.map(expected, PersonDto.class);

        // then
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", name).hasFieldOrPropertyWithValue("age", age).hasFieldOrPropertyWithValue("sex", sex);

        assertThat(actual.getAddresses())
                .isNotEmpty()
                .hasSize(2)
                .satisfies((addressDto -> assertThat(addressDto)
                        .hasFieldOrPropertyWithValue("streetName", "test-street-1")), Index.atIndex(0));
```

