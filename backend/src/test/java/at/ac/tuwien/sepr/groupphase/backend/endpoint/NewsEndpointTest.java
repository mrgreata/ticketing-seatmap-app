package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.DetailedNewsItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.NewsItemCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.NewsItemUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.SimpleNewsItemDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtAuthorizationFilter;
import at.ac.tuwien.sepr.groupphase.backend.service.NewsItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = NewsItemEndpoint.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthorizationFilter.class
    )
)
@Import(NewsEndpointTest.TestSecurityConfig.class)
class NewsEndpointTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(securedEnabled = true)
    static class TestSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NewsItemService newsItemService;



    // ---------------------------------------------------------
    // GET UNREAD NEWS (AUTHENTICATED USER)
    // ---------------------------------------------------------

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void getUnreadNews_returnsOk_forAuthenticatedUser() throws Exception {
        SimpleNewsItemDto dto1 = new SimpleNewsItemDto(
            1L,
            "Unread News",
            LocalDate.of(2024, 1, 15),
            "Unread news summary"
        );

        Pageable pageable = PageRequest.of(0, 12);
        Page<SimpleNewsItemDto> page = new PageImpl<>(List.of(dto1), pageable, 1);

        when(newsItemService.getUnreadNews(eq("user@example.com"), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/api/v1/news/unread")
                .param("page", "0")
                .param("size", "12")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Unread News"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithAnonymousUser
    void getUnreadNews_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/news/unread"))
            .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------
    // GET READ NEWS (AUTHENTICATED USER)
    // ---------------------------------------------------------

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void getReadNews_returnsOk_forAuthenticatedUser() throws Exception {
        SimpleNewsItemDto dto1 = new SimpleNewsItemDto(
            1L,
            "Read News",
            LocalDate.of(2024, 1, 14),
            "Read news summary"
        );

        Pageable pageable = PageRequest.of(0, 12);
        Page<SimpleNewsItemDto> page = new PageImpl<>(List.of(dto1), pageable, 1);

        when(newsItemService.getReadNews(eq("user@example.com"), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/api/v1/news/read")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Read News"));
    }

    @Test
    @WithAnonymousUser
    void getReadNews_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/news/read"))
            .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------
    // GET UNPUBLISHED NEWS (ADMIN ONLY)
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUnpublished_returnsOk_forAdmin() throws Exception {
        SimpleNewsItemDto dto1 = new SimpleNewsItemDto(
            1L,
            "Future News",
            LocalDate.of(2025, 1, 1),
            "Future news summary"
        );

        Pageable pageable = PageRequest.of(0, 12);
        Page<SimpleNewsItemDto> page = new PageImpl<>(List.of(dto1), pageable, 1);

        when(newsItemService.getUnpublished(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/news/unpublished")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Future News"))
            .andExpect(jsonPath("$.content[0].publishedAt").value("2025-01-01"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUnpublished_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/news/unpublished"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void getUnpublished_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/news/unpublished"))
            .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------
    // MARK AS READ (AUTHENTICATED USER)
    // ---------------------------------------------------------

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void markAsRead_returnsNoContent() throws Exception {
        doNothing().when(newsItemService).markAsRead(1L, "user@example.com");

        mockMvc.perform(post("/api/v1/news/1/mark-read")
                .with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithAnonymousUser
    void markAsRead_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/news/1/mark-read")
                .with(csrf()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void markAsRead_notFound_returnsNotFound() throws Exception {
        doThrow(new NotFoundException("News item with id 99 not found"))
            .when(newsItemService).markAsRead(99L, "user@example.com");

        mockMvc.perform(post("/api/v1/news/99/mark-read")
                .with(csrf()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("News item with id 99 not found"));
    }

    // ---------------------------------------------------------
    // CREATE NEWS (ADMIN ONLY)
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void createNews_returnsCreated() throws Exception {
        NewsItemCreateDto createDto = new NewsItemCreateDto(
            "New News Item",
            "Summary of new news",
            "Full text of the new news item",
            LocalDate.of(2024, 1, 20)
        );

        DetailedNewsItemDto resultDto = new DetailedNewsItemDto(
            10L,
            "New News Item",
            LocalDate.of(2024, 1, 20),
            "Summary of new news",
            "Full text of the new news item"
        );

        when(newsItemService.createNews(any(NewsItemCreateDto.class))).thenReturn(resultDto);

        mockMvc.perform(post("/api/v1/news")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.title").value("New News Item"))
            .andExpect(jsonPath("$.publishedAt").value("2024-01-20"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createNews_asUser_returnsForbidden() throws Exception {
        NewsItemCreateDto createDto = new NewsItemCreateDto(
            "New News Item",
            "Summary",
            "Text",
            LocalDate.now()
        );

        mockMvc.perform(post("/api/v1/news")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void createNews_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/news")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------
    // UPDATE NEWS (ADMIN ONLY)
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateNews_returnsOk() throws Exception {
        NewsItemUpdateDto updateDto = new NewsItemUpdateDto(
            1L,
            "Updated News",
            "Updated summary",
            "Updated full text",
            LocalDate.of(2024, 1, 25)
        );

        DetailedNewsItemDto resultDto = new DetailedNewsItemDto(
            1L,
            "Updated News",
            LocalDate.of(2024, 1, 25),
            "Updated summary",
            "Updated full text"
        );

        when(newsItemService.update(any(NewsItemUpdateDto.class))).thenReturn(resultDto);

        mockMvc.perform(put("/api/v1/news/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Updated News"))
            .andExpect(jsonPath("$.publishedAt").value("2024-01-25"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateNews_asUser_returnsForbidden() throws Exception {
        NewsItemUpdateDto updateDto = new NewsItemUpdateDto(
            1L,
            "Updated News",
            "Summary",
            "Text",
            LocalDate.now()
        );

        mockMvc.perform(put("/api/v1/news/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------
    // DELETE NEWS (ADMIN ONLY)
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteNews_returnsNoContent() throws Exception {
        doNothing().when(newsItemService).delete(1L);

        mockMvc.perform(delete("/api/v1/news/1")
                .with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteNews_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/news/1")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteNews_notFound_returnsNotFound() throws Exception {
        doThrow(new NotFoundException("News item with id 99 not found"))
            .when(newsItemService).delete(99L);

        mockMvc.perform(delete("/api/v1/news/99")
                .with(csrf()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("News item with id 99 not found"));
    }

    // ---------------------------------------------------------
    // UPLOAD IMAGE (ADMIN ONLY)
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadImage_returnsOk() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
            "image",
            "test.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test image content".getBytes()
        );

        doNothing().when(newsItemService).uploadImage(eq(1L), any(MultipartFile.class));

        mockMvc.perform(multipart("/api/v1/news/1/image")
                .file(imageFile)
                .with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void uploadImage_asUser_returnsForbidden() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
            "image",
            "test.jpg",
            "image/jpeg",
            "test".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/news/1/image")
                .file(imageFile)
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------
    // DELETE IMAGE (ADMIN ONLY)
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteImage_returnsNoContent() throws Exception {
        doNothing().when(newsItemService).deleteImage(1L);

        mockMvc.perform(delete("/api/v1/news/1/image")
                .with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteImage_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/news/1/image")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }
}