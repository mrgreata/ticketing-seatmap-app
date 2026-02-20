package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.DetailedNewsItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.NewsItemCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.NewsItemUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.SimpleNewsItemDto;
import at.ac.tuwien.sepr.groupphase.backend.service.NewsItemService;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.lang.invoke.MethodHandles;

/**
 * REST Endpoint for NewsItem management.
 * Provides endpoints for retrieving, creating, updating, and deleting news items,
 * as well as marking them as read/unread and managing images.
 */
@RestController
@RequestMapping("/api/v1/news")
public class NewsItemEndpoint {

    private final NewsItemService newsItemService;

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Constructs a new NewsItemEndpoint.
     *
     * @param newsItemService the service handling news item operations
     */
    public NewsItemEndpoint(NewsItemService newsItemService) {
        this.newsItemService = newsItemService;
    }

    /**
     * Retrieves all unread news items for the authenticated user, ordered by publication date (newest first).
     *
     * @param authentication the authentication object containing the user's email
     * @param page the page number (default: 0)
     * @param size the page size (default: 12)
     * @return a page of unread {@link SimpleNewsItemDto} objects
     */
    @Secured("ROLE_USER")
    @GetMapping("/unread")
    public Page<SimpleNewsItemDto> getUnreadNews(
        Authentication authentication,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "12") int size
    ) {
        String userEmail = authentication.getName();
        Pageable pageable = PageRequest.of(page, size);
        LOGGER.info("GET /api/v1/news/unread - User: {}", userEmail);
        Page<SimpleNewsItemDto> result = newsItemService.getUnreadNews(userEmail, pageable);
        LOGGER.info("GET /api/v1/news/unread successful - User: {}, Count: {}", userEmail, result.getSize());
        return result;
    }

    /**
     * Retrieves all read news items for the authenticated user, ordered by publication date (newest first).
     *
     * @param authentication the authentication object containing the user's email
     * @param page the page number (default: 0)
     * @param size the page size (default: 12)
     * @return a page of read {@link SimpleNewsItemDto} objects
     */
    @Secured("ROLE_USER")
    @GetMapping("/read")
    public Page<SimpleNewsItemDto> getReadNews(
        Authentication authentication,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "12") int size
    ) {
        String userEmail = authentication.getName();
        Pageable pageable = PageRequest.of(page, size);
        LOGGER.info("GET /api/v1/news/unread - User: {}", userEmail);
        Page<SimpleNewsItemDto> result = newsItemService.getReadNews(userEmail, pageable);
        LOGGER.info("GET /api/v1/news/read successful - User: {}, Count: {}", userEmail, result.getSize());
        return result;
    }

    /**
     * Retrieves all news items, ordered by publication date (newest first).
     * This endpoint is publicly accessible.
     *
     * @param page the page number (default: 0)
     * @param size the page size (default: 12)
     * @return a page of all {@link SimpleNewsItemDto} objects
     */
    @PermitAll
    @GetMapping
    public Page<SimpleNewsItemDto> findAll(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "12") int size
    ) {
        LOGGER.info("GET api/v1/news");
        Pageable pageable = PageRequest.of(page, size);
        Page<SimpleNewsItemDto> result = newsItemService.findAll(pageable);
        LOGGER.info("GET /api/v1/news successful - Count: {}", result.getSize());
        return result;
    }

    /**
     * Retrieves all news items that are not visible to users do to their publication date being in the future.
     * This Endpoint is only accessible by admins.
     *
     * @param page the page number (default: 0)
     * @param size the page size (default: 12)
     * @return a page of all {@link SimpleNewsItemDto} objects
     */
    @Secured("ROLE_ADMIN")
    @GetMapping("/unpublished")
    public Page<SimpleNewsItemDto> getUnpublished(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "12") int size
    ) {
        LOGGER.info("GET api/v1/news/unpublished");
        Pageable pageable = PageRequest.of(page, size);
        Page<SimpleNewsItemDto> result = newsItemService.getUnpublished(pageable);
        LOGGER.info("GET api/v1/news/unpublished successful - COUNT: {}", result.getSize());
        return result;
    }

    /**
     * Retrieves detailed information about a specific news item by its ID.
     * This endpoint is publicly accessible.
     *
     * @param id the ID of the news item to retrieve
     * @return the {@link DetailedNewsItemDto} containing detailed news information
     */
    @PermitAll
    @GetMapping("/{id}")
    public DetailedNewsItemDto findById(@PathVariable(name = "id") Long id) {
        LOGGER.info("GET api/v1/news/{}", id);
        DetailedNewsItemDto result = newsItemService.findById(id);
        LOGGER.info("GET /api/v1/news/{} successful - Title: {}", id, result.title());
        return result;
    }

    /**
     * Marks a specific news item as read by the authenticated user.
     *
     * @param id the ID of the news item to mark as read
     * @param authentication the authentication object containing the user's email
     */
    @Secured("ROLE_USER")
    @PostMapping("/{id}/mark-read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable(name = "id") Long id, Authentication authentication) {
        String userEmail = authentication.getName();
        LOGGER.info("POST /news/{}/mark-read - User: {}", id, userEmail);
        newsItemService.markAsRead(id, userEmail);
        LOGGER.info("POST /api/v1/news/{}/mark-read successful - User: {}", id, userEmail);
    }

    /**
     * Creates a new news item (admin only).
     *
     * @param dto the {@link NewsItemCreateDto} containing the news item data
     * @return the created {@link DetailedNewsItemDto}
     */
    @Secured("ROLE_ADMIN")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DetailedNewsItemDto createNews(@RequestBody NewsItemCreateDto dto) {
        LOGGER.info("POST /api/v1/news - Title: {}", dto.title());
        DetailedNewsItemDto result = newsItemService.createNews(dto);
        LOGGER.info("POST /api/v1/news successful - Created ID: {}, Title: {}", result.id(), result.title());
        return result;
    }

    /**
     * Updates an existing news item (admin only).
     *
     * @param id the ID of the news item to update
     * @param dto the {@link NewsItemUpdateDto} containing the updated news item data
     * @return the updated {@link DetailedNewsItemDto}
     * @throws IllegalArgumentException if the ID in the path does not match the ID in the request body
     */
    @Secured("ROLE_ADMIN")
    @PutMapping("/{id}")
    public DetailedNewsItemDto updateNews(@PathVariable(name = "id") Long id, @RequestBody NewsItemUpdateDto dto) {
        LOGGER.info("PUT /api/v1/news/{} - Title: {}", id, dto.title());
        if (!id.equals(dto.id())) {
            LOGGER.error("PUT /api/v1/news/{} failed - ID mismatch: path ID={}, body ID={}", id, id, dto.id());
            throw new IllegalArgumentException("ID in path and body must match");
        }

        DetailedNewsItemDto result = newsItemService.update(dto);
        LOGGER.info("PUT /api/v1/news/{} successful - Title: {}", id, result.title());
        return result;
    }

    /**
     * Deletes a news item by its ID (admin only).
     *
     * @param id the ID of the news item to delete
     */
    @Secured("ROLE_ADMIN")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNews(@PathVariable(name = "id") Long id) {
        LOGGER.info("DELETE /api/v1/news/{}", id);
        newsItemService.delete(id);
        LOGGER.info("DELETE /api/v1/news/{} successful", id);
    }

    /**
     * Uploads an image for a specific news item (admin only).
     *
     * @param id the ID of the news item
     * @param image the image file to upload
     * @return HTTP 200 OK on success
     */
    @Secured("ROLE_ADMIN")
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadImage(@PathVariable(name = "id") Long id, @RequestParam("image")MultipartFile image) {
        LOGGER.info("POST /api/v1/news/{}/image - File: {} ({} bytes, Content-Type: {})", id, image.getOriginalFilename(), image.getSize(), image.getContentType());
        newsItemService.uploadImage(id, image);
        LOGGER.info("POST /api/v1/news/{}/image successful", id);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves the image data for a specific news item.
     * This endpoint is publicly accessible.
     *
     * @param id the ID of the news item
     * @return the image data with appropriate content headers, or 404 Not Found if no image exists
     */
    @PermitAll
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable("id") Long id) {
        LOGGER.info("GET /api/v1/news/{}/image", id);

        byte[] data = newsItemService.getImageData(id);
        String contentType = newsItemService.getImageContentType(id);

        if (data == null || data.length == 0) {
            LOGGER.info("GET /api/v1/news/{}/image - No image found", id);
            return ResponseEntity.notFound().build();
        }

        LOGGER.info("GET /api/v1/news/{}/image successful - Size: {} bytes, Content-Type: {}", id, data.length, contentType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(data.length);
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    /**
     * Delete image for an event (admin only).
     *
     * @param id the event ID
     */
    @Secured("ROLE_ADMIN")
    @DeleteMapping("/{id}/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable("id") Long id) {
        newsItemService.deleteImage(id);
    }
}