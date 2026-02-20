package at.ac.tuwien.sepr.groupphase.backend.service.impl.validators;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.NewsItemCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.NewsItemUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.NewsItem;
import at.ac.tuwien.sepr.groupphase.backend.exception.ConflictException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.NewsItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class NewsValidator {
    private final NewsItemRepository newsItemRepository;
    private List<String> validationExceptionList = new ArrayList<>();
    private List<String> conflictExceptionList = new ArrayList<>();

    public NewsValidator(NewsItemRepository newsItemRepository) {
        this.newsItemRepository = newsItemRepository;
    }

    public void validateForCreate(NewsItemCreateDto createDto) {
        validationExceptionList = new ArrayList<>();
        conflictExceptionList = new ArrayList<>();

        if (createDto.title().isBlank()) {
            validationExceptionList.add("Title is blank");
        }
        if (!newsItemRepository.findByTitle(createDto.title()).isEmpty()) {
            conflictExceptionList.add("Title already in use: " + createDto.title());
        }

        if (createDto.summary().isBlank()) {
            validationExceptionList.add("Summary is blank");
        }

        if (createDto.text().isBlank()) {
            validationExceptionList.add("Text is blank");
        }

        if (!validationExceptionList.isEmpty()) {
            throw new ValidationException("Validation error during news create", validationExceptionList);
        }
        if (!conflictExceptionList.isEmpty()) {
            throw new ConflictException("Conflict during news create", conflictExceptionList);
        }
    }

    public void validateForUpdate(NewsItemUpdateDto updateDto) {
        validationExceptionList = new ArrayList<>();
        conflictExceptionList = new ArrayList<>();

        if (updateDto.title().isBlank()) {
            validationExceptionList.add("Title is blank");
        }
        List<NewsItem> sameTitle = newsItemRepository.findByTitle(updateDto.title());
        if (sameTitle.size() > 1) {
            conflictExceptionList.add("Title already in use: " + updateDto.title());
        } else if (sameTitle.size() == 1 && !sameTitle.get(0).getId().equals(updateDto.id())) {
            conflictExceptionList.add("Title already in use: " + updateDto.title());
        }

        if (updateDto.summary().isBlank()) {
            validationExceptionList.add("Summary is blank");
        }

        if (updateDto.text().isBlank()) {
            validationExceptionList.add("Text is blank");
        }

        if (!validationExceptionList.isEmpty()) {
            throw new ValidationException("Validation error during news update", validationExceptionList);
        }
        if (!conflictExceptionList.isEmpty()) {
            throw new ConflictException("Conflict during news update", conflictExceptionList);
        }
    }

    public void validateImage(MultipartFile image) {
        validationExceptionList = new ArrayList<>();
        if (!Arrays.asList("image/jpeg", "image/png", "image/gif").contains(image.getContentType())) {
            validationExceptionList.add("Invalid image type. Only JPEG, PNG, GIF allowed");
        }

        if (image.getSize() > 3 * 1024 * 1024) { // 5MB
            validationExceptionList.add("Image size cannot exceed 5MB");
        }

        if (!validationExceptionList.isEmpty()) {
            throw new ValidationException("Validation error during news image upload", validationExceptionList);
        }
    }
}
