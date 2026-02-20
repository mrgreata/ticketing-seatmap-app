package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartAddMerchandiseItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartCheckoutRequestDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartCheckoutResultDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartUpdateItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase.PaymentDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtAuthorizationFilter;
import at.ac.tuwien.sepr.groupphase.backend.service.CartService;
import at.ac.tuwien.sepr.groupphase.backend.type.PaymentMethod;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = CartEndpoint.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthorizationFilter.class
    )
)
@AutoConfigureMockMvc
public class CartEndpointTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @Test
    void getCart_returnsOk_andMapsDto() throws Exception {
        CartDto cart = new CartDto(
            1L,
            List.of(),
            new BigDecimal("12.50")
        );

        when(cartService.getMyCart(eq("user@email.com"))).thenReturn(cart);

        mockMvc.perform(get("/api/v1/cart")
                .with(user("user@email.com").roles("USER"))
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.total").value(12.50));
    }

    @Test
    void addItem_returnsOk_andMapsDto() throws Exception {
        CartAddMerchandiseItemDto addDto = new CartAddMerchandiseItemDto(
            5L,
            2,
            false
        );

        CartDto updated = new CartDto(
            7L,
            List.of(),
            new BigDecimal("39.90")
        );

        when(cartService.addMerchandiseItem(eq("user@email.com"), eq(5L), eq(2), eq(false)))
            .thenReturn(updated);

        mockMvc.perform(post("/api/v1/cart/items")
                .with(user("user@email.com").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addDto))
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.total").value(39.90));
    }

    @Test
    void updateQuantity_returnsOk_andMapsDto() throws Exception {
        Long cartItemId = 10L;
        CartUpdateItemDto updateDto = new CartUpdateItemDto(3);

        CartDto updated = new CartDto(
            7L,
            List.of(),
            new BigDecimal("59.90")
        );

        when(cartService.updateMerchandiseItemQuantity(eq("user@email.com"), eq(cartItemId), eq(3)))
            .thenReturn(updated);

        mockMvc.perform(patch("/api/v1/cart/items/{id}", cartItemId)
                .with(user("user@email.com").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.total").value(59.90));
    }

    @Test
    void deleteItem_returnsNoContent() throws Exception {
        Long cartItemId = 10L;

        doNothing().when(cartService).removeItem(eq("user@email.com"), eq(cartItemId));

        mockMvc.perform(delete("/api/v1/cart/items/{id}", cartItemId)
                .with(user("user@email.com").roles("USER"))
                .with(csrf()))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));
    }

    @Test
    void checkout_returnsCreated_andMapsResult() throws Exception {
        CartCheckoutRequestDto req = new CartCheckoutRequestDto(
            PaymentMethod.PAYPAL,
            new PaymentDetailDto(
                null,
                null,
                null,
                "buyer@example.com"
            )
        );

        CartCheckoutResultDto result = new CartCheckoutResultDto(100L, 200L);

        when(cartService.checkout(eq("user@email.com"), eq(PaymentMethod.PAYPAL), any(PaymentDetailDto.class)))
            .thenReturn(result);

        mockMvc.perform(post("/api/v1/cart/checkout")
                .with(user("user@email.com").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.merchandiseInvoiceId").value(100))
            .andExpect(jsonPath("$.ticketInvoiceId").value(200));
    }

    @Test
    void addTickets_returnsOk_andMapsListOfCarts() throws Exception {
        List<Long> ticketIds = List.of(1L, 2L, 3L);

        CartDto c1 = new CartDto(1L, List.of(), new BigDecimal("10.00"));
        CartDto c2 = new CartDto(2L, List.of(), new BigDecimal("20.00"));

        when(cartService.addTickets(eq("user@email.com"), eq(ticketIds)))
            .thenReturn(List.of(c1, c2));

        mockMvc.perform(post("/api/v1/cart/tickets")
                .with(user("user@email.com").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketIds))
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].items").isArray())
            .andExpect(jsonPath("$[0].total").value(10.00))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].items").isArray())
            .andExpect(jsonPath("$[1].total").value(20.00));
    }

    @Test
    void removeTicket_returnsNoContent() throws Exception {
        Long ticketId = 99L;

        doNothing().when(cartService).removeTicket(eq("user@email.com"), eq(ticketId));

        mockMvc.perform(delete("/api/v1/cart/tickets/{ticketId}", ticketId)
                .with(user("user@email.com").roles("USER"))
                .with(csrf()))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));
    }


    @Test
    void checkout_whenAccessDenied_returnsForbidden() throws Exception {
        CartCheckoutRequestDto req = new CartCheckoutRequestDto(
            PaymentMethod.PAYPAL,
            new PaymentDetailDto(
                null,
                null,
                null,
                "buyer@example.com"
            )
        );

        when(cartService.checkout(eq("user@email.com"), eq(PaymentMethod.PAYPAL), any(PaymentDetailDto.class)))
            .thenThrow(new AccessDeniedException("not allowed"));

        mockMvc.perform(post("/api/v1/cart/checkout")
                .with(user("user@email.com").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }


    @Test
    void addTickets_withEmptyList_throwsIllegalArgumentException() throws Exception {
        List<Long> ticketIds = List.of();

        mockMvc.perform(post("/api/v1/cart/tickets")
                .with(user("user@email.com").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketIds))
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Ticket IDs dürfen nicht leer sein"));
    }

    @Test
    void addTickets_withNullList_throwsIllegalArgumentException_unit() {
        CartEndpoint endpoint = new CartEndpoint(cartService);

        var auth = new UsernamePasswordAuthenticationToken("user@email.com", "pw");

        assertThrows(
            IllegalArgumentException.class,
            () -> endpoint.addTickets(null, auth)
        );
    }
}
