package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.DetailedNewsItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.NewsItemCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.NewsItemUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.SimpleNewsItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for managing news items.
 * Provides business logic for creating, retrieving, updating, and deleting news items,
 * as well as managing read status and images.
 */
public interface NewsItemService {

    /**
     * Retrieves all unread news items for a specific user.
     * News items are filtered to exclude those the user has already read
     * and are ordered by publication date (newest first).
     *
     * @param userEmail the email of the user
     * @return a list of unread {@link SimpleNewsItemDto} objects
     */
    Page<SimpleNewsItemDto> getUnreadNews(String userEmail, Pageable pageable);

    /**
     * Retrieves all read news items for a specific user.
     * News items are filtered to include only those the user has marked as read
     * and are ordered by publication date (newest first).
     *
     * @param userEmail the email of the user
     * @return a list of read {@link SimpleNewsItemDto} objects
     */
    Page<SimpleNewsItemDto> getReadNews(String userEmail, Pageable pageable);

    /**
     * Retrieves all news items regardless of read status.
     * News items are ordered by publication date (newest first).
     *
     * @return a list of all {@link SimpleNewsItemDto} objects
     */
    Page<SimpleNewsItemDto> findAll(Pageable pageable);

    /**
     * Retrieves all news items which their published date is set in the future.
     * News items are ordered by publication date.
     *
     * @return a list of all{@link  SimpleNewsItemDto} objects
     */
    Page<SimpleNewsItemDto> getUnpublished(Pageable pageable);

    /**
     * Retrieves detailed information about a specific news item by its ID.
     *
     * @param id the ID of the news item to retrieve
     * @return the {@link DetailedNewsItemDto} containing detailed news information
     */
    DetailedNewsItemDto findById(Long id);

    /**
     * Marks a specific news item as read by a user.
     * This operation creates a {@link at.ac.tuwien.sepr.groupphase.backend.entity.SeenNews} record.
     *
     * @param newsId the ID of the news item to mark as read
     * @param userEmail the email of the user
     */
    void markAsRead(Long newsId, String userEmail);

    /**
     * Creates a new news item.
     *
     * @param newsItemCreateDto the DTO containing data for the new news item
     * @return the created {@link DetailedNewsItemDto}
     */
    DetailedNewsItemDto createNews(NewsItemCreateDto newsItemCreateDto);


    /**
     * Updates an existing news item.
     *
     * @param newsItemUpdateDto the DTO containing updated data for the news item
     * @return the updated {@link DetailedNewsItemDto}
     */
    DetailedNewsItemDto update(NewsItemUpdateDto newsItemUpdateDto);

    /**
     * Uploads an image for a specific news item.
     * Replaces any existing image for the news item.
     *
     * @param newsId the ID of the news item
     * @param image the image file to upload
     */
    void uploadImage(Long newsId, MultipartFile image);

    /**
     * Retrieves the image data for a specific news item.
     *
     * @param newsId the ID of the news item
     * @return the image data as byte array, or null if no image exists
     */
    byte[] getImageData(Long newsId);

    /**
     * Retrieves the image data for a specific news item.
     *
     * @param newsId the ID of the news item
     * @return the image data as byte array, or null if no image exists
     */
    String getImageContentType(Long newsId);

    /**
     * Deletes a news item by its ID.
     * Also removes any associated read records and images.
     *
     * @param id the ID of the news item to delete
     */
    void delete(Long id);

    /**
     * Delete the image of an event.
     *
     * @param newsId the event ID
     */
    void deleteImage(Long newsId);
}