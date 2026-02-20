package at.ac.tuwien.sepr.groupphase.backend.integrationtest;


import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.repository.MerchandiseRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;

import org.springframework.mock.web.MockMultipartFile;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import jakarta.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class MerchandiseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private MerchandiseRepository merchandiseRepository;

    private String userToken;
    private String adminToken;

    private Long normalMerchId;
    private Long rewardMerchId;

    @BeforeEach
    void setup() throws Exception {
        String userEmail = "user+" + UUID.randomUUID() + "@test.com";
        String adminEmail = "admin+" + UUID.randomUUID() + "@test.com";

        registerUser(userEmail, "password123", "User", "Test");
        userToken = login(userEmail, "password123");

        registerUser(adminEmail, "password123", "Admin", "Test");
        var admin = userRepository.findByEmail(adminEmail).orElseThrow();
        promoteToAdmin(admin);
        admin.setLocked(false);
        admin.setLoginFailCount(0);
        userRepository.save(admin);
        adminToken = login(adminEmail, "password123");

        normalMerchId = createMerchandiseAsAdmin(
            Map.of(
                "name", "T-Shirt",
                "description", "Normal merch",
                "unitPrice", 10.00,
                "rewardPointsPerUnit", 50,
                "remainingQuantity", 100,
                "redeemableWithPoints", false
            )
        );

        rewardMerchId = createMerchandiseAsAdmin(
            Map.of(
                "name", "Reward Mug",
                "description", "Redeemable",
                "unitPrice", 5.00,
                "rewardPointsPerUnit", 10,
                "remainingQuantity", 50,
                "redeemableWithPoints", true,
                "pointsPrice", 250
            )
        );
    }

    // ---------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------

    private void registerUser(String email, String pw, String fn, String ln) throws Exception {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }

        mockMvc.perform(
                post("/api/v1/users/registration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        new UserRegisterDto(email, pw, fn, ln)
                    ))
            )
            .andExpect(status().isCreated());
    }

    private String login(String email, String pw) throws Exception {
        return mockMvc.perform(post("/api/v1/authentication")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
                        .withEmail(email)
                        .withPassword(pw)
                        .build()
                )))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    private String bearer(String token) {
        return token;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void promoteToAdmin(Object userEntity) {
        try {
            Method setter = null;
            for (Method m : userEntity.getClass().getMethods()) {
                if (m.getName().equals("setUserRole") && m.getParameterCount() == 1) {
                    setter = m;
                    break;
                }
            }
            if (setter == null) {
                throw new IllegalStateException("User entity has no setUserRole(...) method");
            }

            Class<?> paramType = setter.getParameterTypes()[0];
            Object roleValue;

            if (paramType.equals(String.class)) {
                roleValue = "ROLE_ADMIN";
            } else if (paramType.isEnum()) {
                roleValue = Enum.valueOf((Class<? extends Enum>) paramType, "ROLE_ADMIN");
            } else {
                throw new IllegalStateException("Unsupported userRole parameter type: " + paramType);
            }

            setter.invoke(userEntity, roleValue);
        } catch (Exception e) {
            throw new RuntimeException("Failed to promote test user to admin", e);
        }
    }

    private Long createMerchandiseAsAdmin(Map<String, Object> createPayload) throws Exception {
        String response = mockMvc.perform(post("/api/v1/merchandise")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createPayload)))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isNumber())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    // ---------------------------------------------------------
    // READ endpoints (PermitAll)
    // ---------------------------------------------------------

    @Test
    void findAll_returnsOnlyNonDeleted() throws Exception {
        var toDelete = merchandiseRepository.findById(normalMerchId).orElseThrow();
        toDelete.setDeleted(true);
        merchandiseRepository.save(toDelete);

        mockMvc.perform(get("/api/v1/merchandise"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(rewardMerchId));
    }

    @Test
    void findRewards_returnsOnlyRedeemableNonDeleted() throws Exception {
        mockMvc.perform(get("/api/v1/merchandise/rewards"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(rewardMerchId));
    }

    @Test
    void findById_existingActive_returns200_andIsVisibleInFindAll() throws Exception {
        mockMvc.perform(get("/api/v1/merchandise/{id}", normalMerchId))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(normalMerchId))
            .andExpect(jsonPath("$.name").value("T-Shirt"));

        mockMvc.perform(get("/api/v1/merchandise"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.hasItem(normalMerchId.intValue())));
    }

    // ---------------------------------------------------------
    // ADMIN-only create/delete
    // ---------------------------------------------------------

    @Test
    void create_asUser_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/merchandise")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of(
                        "name", "Should Fail",
                        "description", "No admin",
                        "unitPrice", 1.00,
                        "rewardPointsPerUnit", 1,
                        "remainingQuantity", 1,
                        "redeemableWithPoints", false
                    )
                )))
            .andExpect(status().isForbidden());
    }

    @Test
    void create_asAdmin_returns201_andPersists() throws Exception {
        Long createdId = createMerchandiseAsAdmin(
            Map.of(
                "name", "Cap",
                "description", "New merch",
                "unitPrice", 12.50,
                "rewardPointsPerUnit", 20,
                "remainingQuantity", 10,
                "redeemableWithPoints", false
            )
        );

        assertThat(merchandiseRepository.findById(createdId)).isPresent();
        assertThat(merchandiseRepository.findById(createdId).orElseThrow().getDeleted()).isFalse();
    }

    @Test
    void delete_asAdmin_marksDeleted_andRemovesFromFindAll() throws Exception {
        mockMvc.perform(delete("/api/v1/merchandise/{id}", normalMerchId)
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isNoContent());

        var deleted = merchandiseRepository.findById(normalMerchId).orElseThrow();
        assertThat(deleted.getDeleted()).isTrue();

        mockMvc.perform(get("/api/v1/merchandise"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(normalMerchId.intValue()))));
    }

    // ---------------------------------------------------------
    // IMAGE upload/get
    // ---------------------------------------------------------

    @Test
    void getImage_withoutUpload_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/merchandise/{id}/image", normalMerchId))
            .andExpect(status().isNotFound());
    }

    @Test
    void uploadImage_asUser_returns403() throws Exception {
        MockMultipartFile img = new MockMultipartFile(
            "image",
            "pic.png",
            "image/png",
            "pngdata".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/merchandise/{id}/image", normalMerchId)
                .file(img)
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isForbidden());
    }

    @Test
    void uploadImage_invalidType_returns422() throws Exception {
        MockMultipartFile img = new MockMultipartFile(
            "image",
            "file.txt",
            "text/plain",
            "not-an-image".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/merchandise/{id}/image", normalMerchId)
                .file(img)
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(422)))
            .andExpect(jsonPath("$.message", containsString("Invalid image type")));
    }

    @Test
    void uploadImage_andThenGetImage_returns200_withContentTypeAndBytes() throws Exception {
        byte[] data = "png-bytes".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile img = new MockMultipartFile(
            "image",
            "pic.png",
            "image/png",
            data
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/merchandise/{id}/image", normalMerchId)
                .file(img)
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/merchandise/{id}/image", normalMerchId))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", startsWithMediaType("image/png")))
            .andExpect(header().longValue("Content-Length", data.length))
            .andExpect(content().bytes(data));
    }

    private static org.hamcrest.Matcher<String> startsWithMediaType(String mediaType) {
        return org.hamcrest.Matchers.startsWith(mediaType.toLowerCase(Locale.ROOT));
    }
}
