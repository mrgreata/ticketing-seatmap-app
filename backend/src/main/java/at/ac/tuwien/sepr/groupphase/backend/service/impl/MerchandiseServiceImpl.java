package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.DetailedMerchandiseDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.MerchandiseCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.SimpleMerchandiseDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.MerchandiseMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.Merchandise;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.MerchandiseRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.MerchandiseService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MerchandiseServiceImpl implements MerchandiseService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final MerchandiseRepository merchandiseRepository;
    private final MerchandiseMapper merchandiseMapper;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/png",
        "image/jpeg",
        "image/webp"
    );
    private static final long MAX_IMAGE_SIZE_BYTES = (3 * 1024 * 1024);

    public MerchandiseServiceImpl(MerchandiseRepository merchandiseRepository, MerchandiseMapper merchandiseMapper) {
        this.merchandiseRepository = merchandiseRepository;
        this.merchandiseMapper = merchandiseMapper;
    }

    @Override
    public List<SimpleMerchandiseDto> findAll() {
        LOGGER.debug("Find all merchandise items");
        return merchandiseRepository.findAllByDeletedFalse().stream()
            .map(merchandiseMapper::toSimple)
            .toList();
    }

    @Override
    public DetailedMerchandiseDto findById(Long id) {
        LOGGER.debug("Find merchandise by id {}", id);
        Merchandise m = getActiveOrThrow(id);
        return merchandiseMapper.toDetailed(m);
    }

    @Override
    public SimpleMerchandiseDto create(MerchandiseCreateDto dto) {
        LOGGER.debug("Create merchandise");
        Merchandise entity = merchandiseMapper.fromCreateDto(dto);
        entity.setDeleted(false);
        Merchandise saved = merchandiseRepository.save(entity);
        return merchandiseMapper.toSimple(saved);
    }

    @Override
    public List<SimpleMerchandiseDto> findRewards() {
        LOGGER.debug("Find all reward-eligible merchandise items");
        return merchandiseRepository.findByRedeemableWithPointsTrueAndDeletedFalse().stream()
            .map(merchandiseMapper::toSimple)
            .toList();
    }

    @Override
    public void delete(Long id) {
        LOGGER.debug("Delete merchandise by id {}", id);

        var merchandise = merchandiseRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Merchandise not found: " + id));
        if (Boolean.TRUE.equals(merchandise.getDeleted())) {
            return;
        }
        merchandise.setDeleted(true);
        merchandiseRepository.save(merchandise);
    }

    @Override
    @Transactional
    public void uploadImage(Long id, MultipartFile image) {
        LOGGER.debug("Upload image for merchandise id={} (originalFilename='{}', contentType='{}', size={} bytes)",
            id,
            image != null ? image.getOriginalFilename() : null,
            image != null ? image.getContentType() : null,
            image != null ? image.getSize() : null
        );
        Merchandise m = getExisting(id);
        validateImage(image);

        try {
            m.setImage(image.getBytes());
            m.setImageContentType(normalizeContentType(image.getContentType()));
            merchandiseRepository.save(m);

        } catch (IOException e) {
            throw new ValidationException("Image could not be processed!");
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> getImageResponse(Long id) {
        LOGGER.debug("Get image response for merchandise id={}", id);
        Merchandise m = getExisting(id);

        byte[] data = m.getImage();
        if (data == null || data.length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String contentType = m.getImageContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(data.length);

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    private Merchandise getExisting(Long id) {
        LOGGER.debug("Get existing merchandise (including deleted) id={}", id);
        return merchandiseRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Merchandise not found: " + id));
    }

    private void validateImage(MultipartFile image) {
        LOGGER.debug("Validate image (present={}, empty={}, contentType='{}', size={} bytes)",
            image != null,
            image != null && image.isEmpty(),
            image != null ? image.getContentType() : null,
            image != null ? image.getSize() : null
        );
        if (image == null || image.isEmpty()) {
            throw new ValidationException("Image ist leer");
        }
        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new ValidationException("Image is too large (max. 3MB)");
        }

        String ct = normalizeContentType(image.getContentType());
        if (ct == null || !ALLOWED_IMAGE_TYPES.contains(ct)) {
            throw new ValidationException("Invalid image type (allowed: PNG, JPEG, WEBP)");
        }
    }

    private String normalizeContentType(String contentType) {
        String normalized = contentType == null ? null : contentType.trim().toLowerCase(Locale.ROOT);
        LOGGER.debug("Normalize content type '{}' -> '{}'", contentType, normalized);
        return normalized;
    }

    private Merchandise getActiveOrThrow(Long id) {
        LOGGER.debug("Get active merchandise (not deleted) id={}", id);
        Merchandise m = merchandiseRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Merchandise not found: " + id));

        if (Boolean.TRUE.equals(m.getDeleted())) {
            throw new NotFoundException(
                "Merchandise has been deleted and does no longer exist: " + id
            );
        }

        return m;
    }

}