package at.ac.tuwien.sepr.groupphase.backend.endpoint;


import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.MerchandiseCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.SimpleMerchandiseDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.DetailedMerchandiseDto;
import at.ac.tuwien.sepr.groupphase.backend.service.MerchandiseService;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.lang.invoke.MethodHandles;
import java.util.List;


/**
 * REST endpoint for managing merchandise items.
 */
@RestController
@RequestMapping("/api/v1/merchandise")
public class MerchandiseEndpoint {

    private final MerchandiseService merchandiseService;

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Creates a new merchandise endpoint with the required service.
     *
     * @param merchandiseService the service handling merchandise business logic
     */
    public MerchandiseEndpoint(MerchandiseService merchandiseService) {
        this.merchandiseService = merchandiseService;
    }

    /**
     * Retrieves all available merchandise items.
     *
     * @return a list of {@link SimpleMerchandiseDto} representing all merchandise
     */
    @PermitAll
    @GetMapping
    public List<SimpleMerchandiseDto> findAll() {
        LOGGER.info("GET /api/v1/merchandise requested");
        return merchandiseService.findAll();
    }


    /**
     * Retrieves detailed information for a specific merchandise item.
     *
     * @param id the ID of the merchandise item
     * @return a {@link DetailedMerchandiseDto} containing merchandise details
     */
    @PermitAll
    @GetMapping("/{id}")
    public DetailedMerchandiseDto findById(@PathVariable("id") Long id) {
        LOGGER.info("GET /api/v1/merchandise/{} requested", id);
        return merchandiseService.findById(id);
    }


    /**
     * Creates a new merchandise item.
     * This operation is restricted to administrators.
     *
     * @param dto the merchandise creation request data
     * @return a {@link SimpleMerchandiseDto} representing the created merchandise
     */
    @Secured("ROLE_ADMIN")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SimpleMerchandiseDto create(@RequestBody @Valid MerchandiseCreateDto dto) {
        LOGGER.info("POST /api/v1/merchandise requested (name='{}', unitPrice={}, remainingQuantity={}, redeemableWithPoints={})",
            dto != null ? dto.name() : null,
            dto != null ? dto.unitPrice() : null,
            dto != null ? dto.remainingQuantity() : null,
            dto != null ? dto.redeemableWithPoints() : null
        );
        return merchandiseService.create(dto);
    }


    /**
     * Retrieves all merchandise items that can be redeemed using reward points.
     *
     * @return a list of reward-eligible {@link SimpleMerchandiseDto}
     */
    @PermitAll
    @GetMapping("/rewards")
    public List<SimpleMerchandiseDto> findRewards() {
        LOGGER.info("GET /api/v1/merchandise/rewards requested");
        return merchandiseService.findRewards();
    }

    /**
     * Deletes a merchandise item.
     * This operation is restricted to administrators.
     *
     * @param id the ID of the merchandise item to delete
     */
    @Secured("ROLE_ADMIN")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        LOGGER.info("DELETE /api/v1/merchandise/{} requested", id);
        merchandiseService.delete(id);
    }

    /**
     * Uploads or replaces the image associated with a merchandise item.
     * The image is provided as a multipart file.
     * This operation is restricted to administrators.
     *
     * @param id the ID of the merchandise item
     * @param image the image file to upload
     * @return an empty {@link ResponseEntity} with HTTP 200 status
     */
    @Secured("ROLE_ADMIN")
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadImage(
        @PathVariable("id") Long id,
        @RequestParam("image") MultipartFile image
    ) {
        LOGGER.info("POST /api/v1/merchandise/{}/image image upload requested (filename='{}', contentType='{}', size={} bytes)",
            id,
            image != null ? image.getOriginalFilename() : null,
            image != null ? image.getContentType() : null,
            image != null ? image.getSize() : null
        );

        merchandiseService.uploadImage(id, image);
        return ResponseEntity.ok().build();
    }


    /**
     * Retrieves the image associated with a merchandise item.
     *
     * @param id the ID of the merchandise item
     * @return a {@link ResponseEntity} containing the image bytes
     */
    @PermitAll
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable("id") Long id) {
        LOGGER.info("GET /api/v1/merchandise/{}/image requested", id);
        return merchandiseService.getImageResponse(id);
    }
}
