package at.ac.tuwien.sepr.groupphase.backend.service.impl.validators;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.EventCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.EventUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class EventValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final long MAX_IMAGE_SIZE = 3 * 1024 * 1024;
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg",
        "image/png",
        "image/webp"
    );

    public EventValidator() {
    }

    /**
     * Validates an event before creation.
     */
    public void validateForCreate(EventCreateDto dto) {
        LOGGER.debug("Validating event creation for title={}", dto.title());
        List<String> validationErrors = new ArrayList<>();

        if (dto.title() == null || dto.title().isBlank()) {
            validationErrors.add("Titel darf nicht leer sein");
        } else if (dto.title().length() > 255) {
            validationErrors.add("Titel darf maximal 255 Zeichen lang sein");
        }

        if (dto.type() != null && dto.type().length() > 100) {
            validationErrors.add("Typ darf maximal 100 Zeichen lang sein");
        }

        if (dto.durationMinutes() != null) {
            if (dto.durationMinutes() < 1) {
                validationErrors.add("Dauer muss mindestens 1 Minute betragen");
            }
            if (dto.durationMinutes() > 9999) {
                validationErrors.add("Dauer darf maximal 9999 Minuten betragen");
            }
        }

        if (dto.description() != null && dto.description().length() > 800) {
            validationErrors.add("Beschreibung darf maximal 800 Zeichen lang sein");
        }

        if (dto.dateTime() == null) {
            validationErrors.add("Datum und Uhrzeit sind erforderlich");
        } else if (dto.dateTime().isBefore(LocalDateTime.now())) {
            validationErrors.add("Das Datum muss in der Zukunft liegen");
        }

        if (dto.locationId() == null) {
            validationErrors.add("Ort ist erforderlich");
        }

        if (!validationErrors.isEmpty()) {
            throw new ValidationException("Event validation failed", validationErrors);
        }
    }

    /**
     * Validates an event before update.
     */
    public void validateForUpdate(EventUpdateDto dto) {
        LOGGER.debug("Validating event update for id={}", dto.id());
        List<String> validationErrors = new ArrayList<>();

        if (dto.id() == null) {
            validationErrors.add("Event ID ist erforderlich");
        }

        if (dto.title() == null || dto.title().isBlank()) {
            validationErrors.add("Titel darf nicht leer sein");
        } else if (dto.title().length() > 255) {
            validationErrors.add("Titel darf maximal 255 Zeichen lang sein");
        }

        if (dto.type() != null && dto.type().length() > 100) {
            validationErrors.add("Typ darf maximal 100 Zeichen lang sein");
        }

        if (dto.durationMinutes() != null) {
            if (dto.durationMinutes() < 1) {
                validationErrors.add("Dauer muss mindestens 1 Minute betragen");
            }
            if (dto.durationMinutes() > 9999) {
                validationErrors.add("Dauer darf maximal 9999 Minuten betragen");
            }
        }

        if (dto.description() != null && dto.description().length() > 1000) {
            validationErrors.add("Beschreibung darf maximal 1000 Zeichen lang sein");
        }

        if (dto.dateTime() == null) {
            validationErrors.add("Datum und Uhrzeit sind erforderlich");
        }

        if (dto.locationId() == null) {
            validationErrors.add("Ort ist erforderlich");
        }

        if (!validationErrors.isEmpty()) {
            throw new ValidationException("Event validation failed", validationErrors);
        }
    }

    /**
     * Validates an event entity.
     */
    public void validateEvent(Event event) {
        LOGGER.debug("Validating event entity id={}", event.getId());
        List<String> validationErrors = new ArrayList<>();

        if (event.getTitle() == null || event.getTitle().isBlank()) {
            validationErrors.add("Titel darf nicht leer sein");
        }

        if (event.getDateTime() != null && event.getDateTime().isBefore(LocalDateTime.now())) {
            validationErrors.add("Das Datum muss in der Zukunft liegen");
        }

        if (event.getLocation() == null) {
            validationErrors.add("Ort ist erforderlich");
        }

        if (!validationErrors.isEmpty()) {
            throw new ValidationException("Event validation failed", validationErrors);
        }
    }

    /**
     * Validates an image file for event upload.
     */
    public void validateImage(MultipartFile image) {
        LOGGER.debug("Validating image file={}", image.getOriginalFilename());
        List<String> validationErrors = new ArrayList<>();

        if (image == null || image.isEmpty()) {
            validationErrors.add("Bild darf nicht leer sein");
        } else {
            if (image.getSize() > MAX_IMAGE_SIZE) {
                validationErrors.add("Bild darf maximal 3 MB groß sein");
            }

            String contentType = image.getContentType();
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                validationErrors.add("Nur JPG, PNG und WebP Bilder sind erlaubt");
            }

            String originalFilename = image.getOriginalFilename();
            if (originalFilename != null && contentType != null) {
                String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                boolean validExtension = false;

                if (contentType.equals("image/jpeg") && (extension.equals("jpg") || extension.equals("jpeg"))) {
                    validExtension = true;
                } else if (contentType.equals("image/png") && extension.equals("png")) {
                    validExtension = true;
                } else if (contentType.equals("image/webp") && extension.equals("webp")) {
                    validExtension = true;
                }

                if (!validExtension) {
                    validationErrors.add("Dateiendung entspricht nicht dem Bildformat");
                }
            }
        }

        if (!validationErrors.isEmpty()) {
            throw new ValidationException("Image validation failed", validationErrors);
        }
    }
}