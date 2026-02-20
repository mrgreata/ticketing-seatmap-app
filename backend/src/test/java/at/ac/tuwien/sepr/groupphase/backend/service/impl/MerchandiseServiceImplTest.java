package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.DetailedMerchandiseDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.MerchandiseCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.SimpleMerchandiseDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.MerchandiseMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.Merchandise;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.MerchandiseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MerchandiseServiceImplTest {

    @Mock private MerchandiseRepository merchandiseRepository;
    @Mock private MerchandiseMapper merchandiseMapper;

    private MerchandiseServiceImpl merchandiseService;

    @BeforeEach
    void setup() {
        merchandiseService = new MerchandiseServiceImpl(merchandiseRepository, merchandiseMapper);
    }

    private Merchandise merch(
        long id,
        boolean deleted,
        boolean redeemable,
        Integer pointsPrice,
        byte[] image,
        String imageContentType
    ) {
        Merchandise m = new Merchandise();
        m.setId(id);
        m.setName("M-" + id);
        m.setDescription("Desc-" + id);
        m.setUnitPrice(new BigDecimal("9.99"));
        m.setRewardPointsPerUnit(10);
        m.setRemainingQuantity(10);
        m.setRedeemableWithPoints(redeemable);
        m.setPointsPrice(pointsPrice);
        m.setDeleted(deleted);
        m.setImage(image);
        m.setImageContentType(imageContentType);
        return m;
    }

    private SimpleMerchandiseDto simpleDto(long id) {
        return new SimpleMerchandiseDto(
            id,
            "Desc-" + id,
            "M-" + id,
            new BigDecimal("9.99"),
            10,
            10,
            false,
            false,
            null
        );
    }

    // -------------------------
    // findAll
    // -------------------------

    @Test
    void findAll_mapsAndReturnsSimpleDtos() {
        Merchandise m1 = merch(1L, false, false, null, null, null);
        Merchandise m2 = merch(2L, false, true, 100, null, null);

        when(merchandiseRepository.findAllByDeletedFalse()).thenReturn(List.of(m1, m2));
        when(merchandiseMapper.toSimple(m1)).thenReturn(simpleDto(1L));
        when(merchandiseMapper.toSimple(m2)).thenReturn(simpleDto(2L));

        List<SimpleMerchandiseDto> res = merchandiseService.findAll();

        assertAll(
            () -> assertThat(res).hasSize(2),
            () -> assertThat(res.get(0).id()).isEqualTo(1L),
            () -> assertThat(res.get(1).id()).isEqualTo(2L)
        );

        verify(merchandiseRepository).findAllByDeletedFalse();
        verify(merchandiseMapper, times(2)).toSimple(any(Merchandise.class));
    }

    // -------------------------
    // findById
    // -------------------------

    @Test
    void findById_active_merchandise_returnsDetailedDto() {
        Merchandise m = merch(5L, false, true, 100, null, null);

        DetailedMerchandiseDto detailed = new DetailedMerchandiseDto(
            5L,
            "Desc-5",
            "M-5",
            new BigDecimal("9.99"),
            10,
            10,
            true,
            false,
            100
        );

        when(merchandiseRepository.findById(5L)).thenReturn(Optional.of(m));
        when(merchandiseMapper.toDetailed(m)).thenReturn(detailed);

        DetailedMerchandiseDto res = merchandiseService.findById(5L);

        assertThat(res).isEqualTo(detailed);
        verify(merchandiseMapper).toDetailed(m);
    }

    @Test
    void findById_missing_throwsNotFound() {
        when(merchandiseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchandiseService.findById(99L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Merchandise not found");

        verify(merchandiseMapper, never()).toDetailed(any());
    }

    @Test
    void findById_deleted_throwsNotFound() {
        Merchandise deleted = merch(10L, true, false, null, null, null);
        when(merchandiseRepository.findById(10L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> merchandiseService.findById(10L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("has been deleted");

        verify(merchandiseMapper, never()).toDetailed(any());
    }

    // -------------------------
    // create
    // -------------------------

    @Test
    void create_setsDeletedFalse_saves_andMapsToSimple() {
        MerchandiseCreateDto dto = new MerchandiseCreateDto(
            "NewDesc",
            "NewName",
            new BigDecimal("19.90"),
            5,
            7,
            true,
            123
        );

        Merchandise mapped = merch(0L, true, true, 123, null, null);
        Merchandise saved = merch(77L, false, true, 123, null, null);

        SimpleMerchandiseDto simple = new SimpleMerchandiseDto(
            77L,
            "Desc-77",
            "M-77",
            new BigDecimal("9.99"),
            10,
            10,
            true,
            false,
            123
        );

        when(merchandiseMapper.fromCreateDto(dto)).thenReturn(mapped);
        when(merchandiseRepository.save(any(Merchandise.class))).thenReturn(saved);
        when(merchandiseMapper.toSimple(saved)).thenReturn(simple);

        SimpleMerchandiseDto res = merchandiseService.create(dto);

        assertThat(res).isEqualTo(simple);

        ArgumentCaptor<Merchandise> captor = ArgumentCaptor.forClass(Merchandise.class);
        verify(merchandiseRepository).save(captor.capture());
        assertThat(captor.getValue().getDeleted()).isFalse();

        verify(merchandiseMapper).fromCreateDto(dto);
        verify(merchandiseMapper).toSimple(saved);
    }

    // -------------------------
    // findRewards
    // -------------------------

    @Test
    void findRewards_mapsAndReturnsSimpleDtos() {
        Merchandise r1 = merch(1L, false, true, 100, null, null);
        Merchandise r2 = merch(2L, false, true, 200, null, null);

        when(merchandiseRepository.findByRedeemableWithPointsTrueAndDeletedFalse()).thenReturn(List.of(r1, r2));
        when(merchandiseMapper.toSimple(r1)).thenReturn(simpleDto(1L));
        when(merchandiseMapper.toSimple(r2)).thenReturn(simpleDto(2L));

        List<SimpleMerchandiseDto> res = merchandiseService.findRewards();

        assertThat(res).hasSize(2);
        verify(merchandiseRepository).findByRedeemableWithPointsTrueAndDeletedFalse();
        verify(merchandiseMapper, times(2)).toSimple(any(Merchandise.class));
    }

    // -------------------------
    // delete
    // -------------------------

    @Test
    void delete_existingNotDeleted_setsDeletedTrue_andSaves() {
        Merchandise m = merch(5L, false, false, null, null, null);
        when(merchandiseRepository.findById(5L)).thenReturn(Optional.of(m));
        when(merchandiseRepository.save(m)).thenReturn(m);

        merchandiseService.delete(5L);

        assertThat(m.getDeleted()).isTrue();
        verify(merchandiseRepository).save(m);
    }

    @Test
    void delete_alreadyDeleted_returnsWithoutSaving() {
        Merchandise m = merch(5L, true, false, null, null, null);
        when(merchandiseRepository.findById(5L)).thenReturn(Optional.of(m));

        merchandiseService.delete(5L);

        verify(merchandiseRepository, never()).save(any());
    }

    @Test
    void delete_missing_throwsNotFound() {
        when(merchandiseRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchandiseService.delete(123L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Merchandise not found");

        verify(merchandiseRepository, never()).save(any());
    }

    // -------------------------
    // uploadImage
    // -------------------------

    @Test
    void uploadImage_validPng_storesBytes_andNormalizedContentType_andSaves() {
        Merchandise m = merch(5L, false, false, null, null, null);
        when(merchandiseRepository.findById(5L)).thenReturn(Optional.of(m));
        when(merchandiseRepository.save(m)).thenReturn(m);

        MockMultipartFile img = new MockMultipartFile(
            "image",
            "test.png",
            " image/PNG ",
            "abc".getBytes(StandardCharsets.UTF_8)
        );

        merchandiseService.uploadImage(5L, img);

        assertAll(
            () -> assertThat(m.getImage()).isEqualTo("abc".getBytes(StandardCharsets.UTF_8)),
            () -> assertThat(m.getImageContentType()).isEqualTo("image/png")
        );
        verify(merchandiseRepository).save(m);
    }

    @Test
    void uploadImage_merchandiseMissing_throwsNotFound() {
        when(merchandiseRepository.findById(5L)).thenReturn(Optional.empty());

        MockMultipartFile img = new MockMultipartFile("image", "x.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> merchandiseService.uploadImage(5L, img))
            .isInstanceOf(NotFoundException.class);

        verify(merchandiseRepository, never()).save(any());
    }

    @Test
    void uploadImage_nullOrEmpty_throwsValidation() {
        Merchandise m = merch(5L, false, false, null, null, null);
        when(merchandiseRepository.findById(5L)).thenReturn(Optional.of(m));

        MockMultipartFile empty = new MockMultipartFile("image", "x.png", "image/png", new byte[0]);

        assertAll(
            () -> assertThatThrownBy(() -> merchandiseService.uploadImage(5L, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("leer"),
            () -> assertThatThrownBy(() -> merchandiseService.uploadImage(5L, empty))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("leer")
        );

        verify(merchandiseRepository, never()).save(any());
    }

    @Test
    void uploadImage_tooLarge_throwsValidation() {
        Merchandise m = merch(5L, false, false, null, null, null);
        when(merchandiseRepository.findById(5L)).thenReturn(Optional.of(m));

        byte[] big = new byte[3 * 1024 * 1024 + 1];
        MockMultipartFile img = new MockMultipartFile("image", "x.png", "image/png", big);

        assertThatThrownBy(() -> merchandiseService.uploadImage(5L, img))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("too large");

        verify(merchandiseRepository, never()).save(any());
    }

    @Test
    void uploadImage_invalidType_throwsValidation() {
        Merchandise m = merch(5L, false, false, null, null, null);
        when(merchandiseRepository.findById(5L)).thenReturn(Optional.of(m));

        MockMultipartFile img = new MockMultipartFile("image", "x.gif", "image/gif", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> merchandiseService.uploadImage(5L, img))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Invalid image type");

        verify(merchandiseRepository, never()).save(any());
    }

    @Test
    void uploadImage_getBytesThrowsIOException_wrapsAsValidationException() throws Exception {
        Merchandise m = merch(5L, false, false, null, null, null);
        when(merchandiseRepository.findById(5L)).thenReturn(Optional.of(m));

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> merchandiseService.uploadImage(5L, file))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("could not be processed");

        verify(merchandiseRepository, never()).save(any());
    }

    // -------------------------
    // getImageResponse
    // -------------------------

    @Test
    void getImageResponse_noImage_returns404() {
        Merchandise m = merch(5L, false, false, null, null, null);
        when(merchandiseRepository.findById(5L)).thenReturn(Optional.of(m));

        ResponseEntity<byte[]> res = merchandiseService.getImageResponse(5L);

        assertAll(
            () -> assertThat(res.getStatusCode().value()).isEqualTo(404),
            () -> assertThat(res.getBody()).isNull()
        );
    }

    @Test
    void getImageResponse_withImage_andContentType_returns200WithHeaders() {
        byte[] bytes = "img".getBytes(StandardCharsets.UTF_8);
        Merchandise m = merch(5L, false, false, null, bytes, "image/webp");

        when(merchandiseRepository.findById(5L)).thenReturn(Optional.of(m));

        ResponseEntity<byte[]> res = merchandiseService.getImageResponse(5L);

        assertAll(
            () -> assertThat(res.getStatusCode().value()).isEqualTo(200),
            () -> assertThat(res.getBody()).isEqualTo(bytes),
            () -> assertThat(res.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("image/webp")),
            () -> assertThat(res.getHeaders().getContentLength()).isEqualTo(bytes.length)
        );
    }

    @Test
    void getImageResponse_withImage_butBlankContentType_fallsBackToOctetStream() {
        byte[] bytes = "img".getBytes(StandardCharsets.UTF_8);
        Merchandise m = merch(5L, false, false, null, bytes, "   ");

        when(merchandiseRepository.findById(5L)).thenReturn(Optional.of(m));

        ResponseEntity<byte[]> res = merchandiseService.getImageResponse(5L);

        assertAll(
            () -> assertThat(res.getStatusCode().value()).isEqualTo(200),
            () -> assertThat(res.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM)
        );
    }

    @Test
    void getImageResponse_merchandiseMissing_throwsNotFound() {
        when(merchandiseRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchandiseService.getImageResponse(5L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Merchandise not found");

        verifyNoInteractions(merchandiseMapper);
    }
}
