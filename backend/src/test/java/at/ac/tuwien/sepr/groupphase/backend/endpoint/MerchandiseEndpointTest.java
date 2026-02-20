package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.DetailedMerchandiseDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.MerchandiseCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.SimpleMerchandiseDto;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtAuthorizationFilter;
import at.ac.tuwien.sepr.groupphase.backend.service.MerchandiseService;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = MerchandiseEndpoint.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthorizationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
public class MerchandiseEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MerchandiseService merchandiseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void findAll_returnsOk_andReturnsList() throws Exception {
        SimpleMerchandiseDto d1 = new SimpleMerchandiseDto(
            1L, "desc-1", "name-1", new BigDecimal("9.99"), 10, 100, false, false, null
        );
        SimpleMerchandiseDto d2 = new SimpleMerchandiseDto(
            2L, "desc-2", "name-2", new BigDecimal("19.99"), 20, 200, true, false, 500
        );

        when(merchandiseService.findAll()).thenReturn(List.of(d1, d2));

        mockMvc.perform(get("/api/v1/merchandise").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("name-1"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("name-2"));

        verify(merchandiseService, times(1)).findAll();
    }

    @Test
    void findById_returnsOk_andReturnsDetailed() throws Exception {
        DetailedMerchandiseDto detailed = new DetailedMerchandiseDto(
            7L,
            "detail-desc",
            "detail-name",
            new BigDecimal("29.99"),
            99,
            3,
            true,
            false,
            123
        );

        when(merchandiseService.findById(7L)).thenReturn(detailed);

        mockMvc.perform(get("/api/v1/merchandise/7").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.name").value("detail-name"));

        verify(merchandiseService, times(1)).findById(7L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returnsCreated_andDelegatesToService() throws Exception {
        MerchandiseCreateDto dto = new MerchandiseCreateDto(
            "desc",
            "name",
            new BigDecimal("9.99"),
            5,
            10,
            false,
            null
        );

        SimpleMerchandiseDto out = new SimpleMerchandiseDto(
            99L,
            "desc",
            "name",
            new BigDecimal("9.99"),
            5,
            10,
            false,
            false,
            null
        );

        when(merchandiseService.create(dto)).thenReturn(out);

        mockMvc.perform(
                post("/api/v1/merchandise")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(99))
            .andExpect(jsonPath("$.name").value("name"));

        verify(merchandiseService, times(1)).create(dto);
    }

    @Test
    void findRewards_returnsOk_andReturnsList() throws Exception {
        SimpleMerchandiseDto d1 = new SimpleMerchandiseDto(
            5L, "reward-desc", "reward-name", new BigDecimal("0.00"), 0, 7, true, false, 200
        );

        when(merchandiseService.findRewards()).thenReturn(List.of(d1));

        mockMvc.perform(get("/api/v1/merchandise/rewards").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(5))
            .andExpect(jsonPath("$[0].redeemableWithPoints").value(true));

        verify(merchandiseService, times(1)).findRewards();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returnsNoContent_andCallsService() throws Exception {
        mockMvc.perform(delete("/api/v1/merchandise/10"))
            .andExpect(status().isNoContent());

        verify(merchandiseService, times(1)).delete(10L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadImage_returnsOk_andCallsService() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "image",
            "img.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake-image-bytes".getBytes()
        );

        mockMvc.perform(
                multipart("/api/v1/merchandise/10/image")
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
            )
            .andExpect(status().isOk());

        verify(merchandiseService, times(1)).uploadImage(10L, file);
    }

    @Test
    void getImage_returnsOk_andDelegatesToService() throws Exception {
        when(merchandiseService.getImageResponse(10L))
            .thenReturn(org.springframework.http.ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body("bytes".getBytes()));

        mockMvc.perform(get("/api/v1/merchandise/10/image"))
            .andExpect(status().isOk());

        verify(merchandiseService, times(1)).getImageResponse(10L);
    }
}
