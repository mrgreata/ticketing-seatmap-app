package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.DetailedMerchandiseDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.MerchandiseCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.SimpleMerchandiseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for managing merchandise items.
 */
public interface MerchandiseService {


    /**
     * Retrieves all available merchandise items.
     *
     * @return a list of {@link SimpleMerchandiseDto} representing all merchandise
     */
    List<SimpleMerchandiseDto> findAll();

    /**
     * Retrieves a single merchandise item by its unique identifier.
     *
     * @param id the ID of the merchandise item
     * @return a {@link DetailedMerchandiseDto} containing full merchandise details
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException
     *         if no merchandise with the given ID exists
     */
    DetailedMerchandiseDto findById(Long id);

    /**
     * Creates a new merchandise item.
     *
     * @param dto the data transfer object containing the merchandise creation data
     * @return a {@link SimpleMerchandiseDto} representing the created merchandise
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException
     *         if the provided data is invalid
     */
    SimpleMerchandiseDto create(MerchandiseCreateDto dto);

    /**
     * Retrieves all merchandise items that are available as rewards.
     * Reward merchandise items can typically be obtained using reward points
     * instead of monetary payment.
     *
     * @return a list of {@link SimpleMerchandiseDto} representing reward merchandise
     */
    List<SimpleMerchandiseDto> findRewards();

    /**
     * Deletes a merchandise item by its unique identifier.
     *
     * @param id the ID of the merchandise item to delete
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException
     *         if no merchandise with the given ID exists
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ConflictException
     *         if the merchandise cannot be deleted due to existing references
     */
    void delete(Long id);

    /**
     * Uploads or replaces the image associated with a merchandise item.
     *
     * @param id the ID of the merchandise item
     * @param image the image file to associate with the merchandise
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException
     *         if no merchandise with the given ID exists
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException
     *         if the image file is invalid or unsupported
     */
    void uploadImage(Long id, MultipartFile image);

    /**
     * Retrieves the image associated with a merchandise item as an HTTP response.
     * The returned response entity contains the image bytes and appropriate
     * HTTP headers such as content type.
     *
     * @param id the ID of the merchandise item
     * @return a {@link ResponseEntity} containing the image bytes
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException
     *         if no merchandise or image exists for the given ID
     */
    ResponseEntity<byte[]> getImageResponse(Long id);
}