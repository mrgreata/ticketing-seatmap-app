package at.ac.tuwien.sepr.groupphase.backend.service.impl.validators;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.EventCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.EventUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Location;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventValidatorTest {

    private EventValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EventValidator();
    }

    @Test
    void validateForCreate_shouldPass_whenAllFieldsValid() {
        EventCreateDto dto = new EventCreateDto(
            "Valid Event",
            "Concert",
            120,
            "Description",
            LocalDateTime.now().plusDays(1),
            1L,
            null
        );

        assertDoesNotThrow(() -> validator.validateForCreate(dto));
    }

    @Test
    void validateForCreate_shouldThrow_whenTitleIsNull() {
        EventCreateDto dto = new EventCreateDto(
            null,
            "Concert",
            120,
            "Description",
            LocalDateTime.now().plusDays(1),
            1L,
            null
        );

        assertThrows(ValidationException.class,
            () -> validator.validateForCreate(dto));
    }

    @Test
    void validateForCreate_shouldThrow_whenTitleIsBlank() {
        EventCreateDto dto = new EventCreateDto(
            "   ",
            "Concert",
            120,
            "Description",
            LocalDateTime.now().plusDays(1),
            1L,
            null
        );

        assertThrows(ValidationException.class,
            () -> validator.validateForCreate(dto));
    }

    @Test
    void validateForCreate_shouldThrow_whenTitleTooLong() {
        String longTitle = "a".repeat(256);
        EventCreateDto dto = new EventCreateDto(
            longTitle,
            "Concert",
            120,
            "Description",
            LocalDateTime.now().plusDays(1),
            1L,
            null
        );

        assertThrows(ValidationException.class,
            () -> validator.validateForCreate(dto));
    }

    @Test
    void validateForCreate_shouldThrow_whenDateTimeIsNull() {
        EventCreateDto dto = new EventCreateDto(
            "Valid Event",
            "Concert",
            120,
            "Description",
            null,
            1L,
            null
        );

        assertThrows(ValidationException.class,
            () -> validator.validateForCreate(dto));
    }

    @Test
    void validateForCreate_shouldThrow_whenDateTimeInPast() {
        EventCreateDto dto = new EventCreateDto(
            "Valid Event",
            "Concert",
            120,
            "Description",
            LocalDateTime.now().minusDays(1),
            1L,
            null
        );

        assertThrows(ValidationException.class,
            () -> validator.validateForCreate(dto));
    }

    @Test
    void validateForCreate_shouldThrow_whenLocationIdIsNull() {
        EventCreateDto dto = new EventCreateDto(
            "Valid Event",
            "Concert",
            120,
            "Description",
            LocalDateTime.now().plusDays(1),
            null,
            null
        );

        assertThrows(ValidationException.class,
            () -> validator.validateForCreate(dto));
    }

    @Test
    void validateForCreate_shouldThrow_whenDurationTooSmall() {
        EventCreateDto dto = new EventCreateDto(
            "Valid Event",
            "Concert",
            0,
            "Description",
            LocalDateTime.now().plusDays(1),
            1L,
            null
        );

        assertThrows(ValidationException.class,
            () -> validator.validateForCreate(dto));
    }

    @Test
    void validateForCreate_shouldThrow_whenDurationTooLarge() {
        EventCreateDto dto = new EventCreateDto(
            "Valid Event",
            "Concert",
            10000,
            "Description",
            LocalDateTime.now().plusDays(1),
            1L,
            null
        );

        assertThrows(ValidationException.class,
            () -> validator.validateForCreate(dto));
    }

    @Test
    void validateForCreate_shouldThrow_whenDescriptionTooLong() {
        String longDesc = "a".repeat(801);
        EventCreateDto dto = new EventCreateDto(
            "Valid Event",
            "Concert",
            120,
            longDesc,
            LocalDateTime.now().plusDays(1),
            1L,
            null
        );

        assertThrows(ValidationException.class,
            () -> validator.validateForCreate(dto));
    }

    @Test
    void validateForCreate_shouldThrow_whenTypeTooLong() {
        String longType = "a".repeat(101);
        EventCreateDto dto = new EventCreateDto(
            "Valid Event",
            longType,
            120,
            "Description",
            LocalDateTime.now().plusDays(1),
            1L,
            null
        );

        assertThrows(ValidationException.class,
            () -> validator.validateForCreate(dto));
    }


    @Test
    void validateForUpdate_shouldPass_whenAllFieldsValid() {
        EventUpdateDto dto = new EventUpdateDto(
            1L,
            "Updated Event",
            "Concert",
            120,
            "Description",
            LocalDateTime.now().plusDays(1),
            1L,
            null
        );

        assertDoesNotThrow(() -> validator.validateForUpdate(dto));
    }

    @Test
    void validateForUpdate_shouldThrow_whenIdIsNull() {
        EventUpdateDto dto = new EventUpdateDto(
            null,
            "Updated Event",
            "Concert",
            120,
            "Description",
            LocalDateTime.now().plusDays(1),
            1L,
            null
        );

        assertThrows(ValidationException.class,
            () -> validator.validateForUpdate(dto));
    }

    @Test
    void validateForUpdate_shouldThrow_whenTitleIsNull() {
        EventUpdateDto dto = new EventUpdateDto(
            1L,
            null,
            "Concert",
            120,
            "Description",
            LocalDateTime.now().plusDays(1),
            1L,
            null
        );

        assertThrows(ValidationException.class,
            () -> validator.validateForUpdate(dto));
    }

    @Test
    void validateEvent_shouldPass_whenEventIsValid() {
        Event event = new Event();
        event.setId(1L);
        event.setTitle("Valid Event");
        event.setDateTime(LocalDateTime.now().plusDays(1));
        Location location = new Location();
        location.setId(1L);
        event.setLocation(location);

        assertDoesNotThrow(() -> validator.validateEvent(event));
    }

    @Test
    void validateEvent_shouldThrow_whenTitleIsNull() {
        Event event = new Event();
        event.setId(1L);
        event.setTitle(null);
        event.setDateTime(LocalDateTime.now().plusDays(1));
        Location location = new Location();
        event.setLocation(location);

        assertThrows(ValidationException.class,
            () -> validator.validateEvent(event));
    }

    @Test
    void validateEvent_shouldThrow_whenDateTimeInPast() {
        Event event = new Event();
        event.setId(1L);
        event.setTitle("Valid Event");
        event.setDateTime(LocalDateTime.now().minusDays(1));
        Location location = new Location();
        event.setLocation(location);

        assertThrows(ValidationException.class,
            () -> validator.validateEvent(event));
    }

    @Test
    void validateEvent_shouldThrow_whenLocationIsNull() {
        Event event = new Event();
        event.setId(1L);
        event.setTitle("Valid Event");
        event.setDateTime(LocalDateTime.now().plusDays(1));
        event.setLocation(null);

        assertThrows(ValidationException.class,
            () -> validator.validateEvent(event));
    }

    @Test
    void validateImage_shouldPass_whenImageIsValid() {
        MockMultipartFile validImage = new MockMultipartFile(
            "image",
            "test.jpg",
            "image/jpeg",
            new byte[1024]
        );

        assertDoesNotThrow(() -> validator.validateImage(validImage));
    }

    @Test
    void validateImage_shouldThrow_whenImageIsEmpty() {
        MockMultipartFile emptyImage = new MockMultipartFile(
            "image",
            "test.jpg",
            "image/jpeg",
            new byte[0]
        );

        assertThrows(ValidationException.class,
            () -> validator.validateImage(emptyImage));
    }

    @Test
    void validateImage_shouldThrow_whenImageTooLarge() {
        byte[] largeImageData = new byte[4 * 1024 * 1024];
        MockMultipartFile largeImage = new MockMultipartFile(
            "image",
            "test.jpg",
            "image/jpeg",
            largeImageData
        );

        assertThrows(ValidationException.class,
            () -> validator.validateImage(largeImage));
    }

    @Test
    void validateImage_shouldThrow_whenContentTypeInvalid() {
        MockMultipartFile invalidImage = new MockMultipartFile(
            "image",
            "test.pdf",
            "application/pdf",
            new byte[1024]
        );

        assertThrows(ValidationException.class,
            () -> validator.validateImage(invalidImage));
    }

    @Test
    void validateImage_shouldThrow_whenExtensionMismatch() {
        MockMultipartFile mismatchImage = new MockMultipartFile(
            "image",
            "test.png",
            "image/jpeg",
            new byte[1024]
        );

        assertThrows(ValidationException.class,
            () -> validator.validateImage(mismatchImage));
    }

    @Test
    void validateImage_shouldPass_whenPngImage() {
        MockMultipartFile pngImage = new MockMultipartFile(
            "image",
            "test.png",
            "image/png",
            new byte[1024]
        );

        assertDoesNotThrow(() -> validator.validateImage(pngImage));
    }

    @Test
    void validateImage_shouldPass_whenWebpImage() {
        MockMultipartFile webpImage = new MockMultipartFile(
            "image",
            "test.webp",
            "image/webp",
            new byte[1024]
        );

        assertDoesNotThrow(() -> validator.validateImage(webpImage));
    }
}
