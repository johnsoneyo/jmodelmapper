package de.johnsoneyo.mapper;

import de.johnsoneyo.mapper.decorator.StringToUUIDTypeAdapter;
import de.johnsoneyo.mapper.decorator.TransformToType;
import de.johnsoneyo.mapper.exception.JModelMapperException;
import org.assertj.core.api.Condition;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 *
 */
@ExtendWith(MockitoExtension.class)
class JModelMapperTest {

    @Spy
    JModelMapper modelMapper;

    @Test
    void map_ShouldNotThrowException_WhenMappingNestedObjects() {

        // given
        String name = "test-name";
        Integer age = 1;
        String sex = "test-sex";
        Person expected = new Person(name, age, sex, List.of(new Person.Address("test-street-1", "test-zipcode-1",
                        new Person.Address.ExtraInfo("11000011.000111")),
                new Person.Address("test-street-2", "test-zipcode-2", new Person.Address.ExtraInfo("test-extra-info"))),
                Map.of("key", "value"));

        //when

        PersonDto actual = modelMapper.map(expected, PersonDto.class);

        // then
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", name).hasFieldOrPropertyWithValue("age", age).hasFieldOrPropertyWithValue("sex", sex);

        Condition<PersonDto.AddressDto> addressDtoCondition = new Condition<>(addressDto ->
                addressDto.getExtraInfo() != null && addressDto.getExtraInfo().coordinates == "11000011.000111", "extra info is not null");
        assertThat(actual.getAddresses())
                .isNotEmpty()
                .hasSize(2)
                .satisfies((addressDto -> assertThat(addressDto)
                        .hasFieldOrPropertyWithValue("streetName", "test-street-1").has(addressDtoCondition)), Index.atIndex(0));
    }

    @Test
    void map_ShouldThrowException_WhenMappingSameFieldOfDifferentJavaType() {

        // given
        Extra expected = new Extra(1L);

        // then
        assertThatThrownBy(() -> JModelMapperUtils.map(expected, ExtraDto.class))
                .isInstanceOf(JModelMapperException.class)
                .hasMessage("error occurred while mapping entity")
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void map_ShouldMapWithoutException_WhenStringToUUIDAdapterUsed() {

        // given
        String requesterId = "a0200f66-f5b2-4cc7-accd-9810f1b1471f";
        Request request = new Request(requesterId);

        // when
        RequestDto requestDto = modelMapper.map(request, RequestDto.class);

        // then
        assertThat(requestDto.getRequesterId()).isEqualTo(UUID.fromString(requesterId));
    }


    /**
     *
     */
    static class Person {

        String name;
        Integer age;
        String sex;
        List<Address> addresses;
        Map<String, String> attributes;

        public Person(String name, Integer age, String sex, List<Address> addresses, Map<String, String> attributes) {
            this.name = name;
            this.age = age;
            this.sex = sex;
            this.addresses = addresses;
            this.attributes = attributes;
        }

        static class Address {

            public Address(String streetName, String zipCode, ExtraInfo extraInfo) {
                this.streetName = streetName;
                this.zipCode = zipCode;
                this.extraInfo = extraInfo;
            }

            String streetName;
            String zipCode;

            ExtraInfo extraInfo;

            static class ExtraInfo {

                public ExtraInfo(String coordinates) {
                    this.coordinates = coordinates;
                }

                String coordinates;


            }
        }
    }

    static class PersonDto {

        String name;
        Integer age;
        String sex;
        List<AddressDto> addresses;
        Map<String, String> attributes;

        public PersonDto() {
        }

        public String getName() {
            return name;
        }


        public List<AddressDto> getAddresses() {
            return addresses;
        }


        @Override
        public String toString() {
            return "PersonDto{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    ", sex='" + sex + '\'' +
                    ", addresses=" + addresses +
                    ", attributes=" + attributes +
                    '}';
        }

        static class AddressDto {

            String streetName;
            String zipCode;

            ExtraInfoDto extraInfo;

            public AddressDto() {
            }

            public ExtraInfoDto getExtraInfo() {
                return extraInfo;
            }

            @Override
            public String toString() {
                return "AddressDto{" +
                        "streetName='" + streetName + '\'' +
                        ", zipCode='" + zipCode + '\'' +
                        '}';
            }

            static class ExtraInfoDto {

                String coordinates;

                public ExtraInfoDto() {
                }


            }
        }
    }

    static class Extra {

        Long value;

        public Extra(Long value) {
            this.value = value;
        }
    }

    static class ExtraDto {

        String value;

        public ExtraDto() {
        }
    }

    static class Request {
        String requesterId;

        public Request(String requesterId) {
            this.requesterId = requesterId;
        }
    }

    static class RequestDto {

        @TransformToType(typeAdapter = StringToUUIDTypeAdapter.class)
        UUID requesterId;

        public RequestDto() {
        }

        public UUID getRequesterId() {
            return requesterId;
        }
    }
}