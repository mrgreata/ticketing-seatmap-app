package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.CreditInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.DetailedInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.InvoiceCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.SimpleInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.InvoiceMerchandiseItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.InvoiceTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.InvoiceMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtAuthorizationFilter;
import at.ac.tuwien.sepr.groupphase.backend.service.InvoiceService;
import at.ac.tuwien.sepr.groupphase.backend.service.PdfService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = InvoiceEndpoint.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthorizationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class InvoiceEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InvoiceService invoiceService;


    @MockitoBean
    private InvoiceMapper invoiceMapper;


    @MockitoBean
    private PdfService pdfService;

    // ---------------- CREATE ----------------

    @Test
    void createInvoice_returns201() throws Exception {
        InvoiceCreateDto createDto = new InvoiceCreateDto(
            1L,
            "John",
            "Doe",
            "Street 1, City",
            LocalDateTime.now().plusDays(1),
            List.of(1L, 2L)
        );

        SimpleInvoiceDto simpleDto = new SimpleInvoiceDto(
            1L,
            "INV-1",
            1L
        );

        when(invoiceService.create(any(InvoiceCreateDto.class))).thenReturn(simpleDto);

        mockMvc.perform(post("/api/v1/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.invoiceNumber").value("INV-1"))
            .andExpect(jsonPath("$.userId").value(1L));
    }

    // ---------------- GET MY INVOICES ----------------

    @Test
    void getMyInvoices_returns200() throws Exception {
        DetailedInvoiceDto detailedDto = new DetailedInvoiceDto(
            1L,
            "INV-1",
            LocalDate.now(),
            1L,
            List.of(),
            List.of()
        );

        when(invoiceService.getMyInvoices("user@test.com"))
            .thenReturn(List.of(detailedDto));

        mockMvc.perform(get("/api/v1/invoices/my")
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].invoiceNumber").value("INV-1"))
            .andExpect(jsonPath("$[0].userId").value(1L));
    }
    @Test
    void getMyInvoices_notMyInvoices_returns403() throws Exception {
        // Mock: Service wirft AccessDeniedException, weil der User keine Rechte hat
        when(invoiceService.getMyInvoices("user@test.com"))
            .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/v1/invoices/my")
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied"));
    }



    // ---------------- GET BY ID ----------------
    @Test
    void findInvoiceById_returns200() throws Exception {
        // Mock Tickets
        InvoiceTicketDto ticket1 = new InvoiceTicketDto(
            1000L,
            "testevent",
            "2026-01-17T19:30:00"
        );

        InvoiceTicketDto ticket2 = new InvoiceTicketDto(
            1001L,
            "testevent",
            "2026-01-17T19:30:00"
        );

        // Mock Merchandise
        InvoiceMerchandiseItemDto merch1 = new InvoiceMerchandiseItemDto(
            1L,                        // merchandiseId
            "T-Shirt",                 // name
            BigDecimal.valueOf(15.0),  // unitPrice
            2,                         // quantity
            10                         // rewardPointsPerUnit
        );

        DetailedInvoiceDto detailedDto = new DetailedInvoiceDto(
            1L,
            "INV-1",
            LocalDate.now(),
            1L,
            List.of(ticket1, ticket2),
            List.of(merch1)
        );

        // Dummy User für Invoice
        User dummyUser = new User();
        dummyUser.setId(1L);
        dummyUser.setEmail("user@test.com");

        // Dummy Invoice-Entity
        Invoice dummyInvoice = new Invoice();
        dummyInvoice.setId(1L);
        dummyInvoice.setUser(dummyUser);

        // Mockito korrekt mocken
        when(invoiceService.getInvoiceDetailsForUser(eq(1L), eq("user@test.com")))
            .thenReturn(detailedDto);
        when(invoiceMapper.toDetailed(any(Invoice.class))).thenReturn(detailedDto);

        mockMvc.perform(get("/api/v1/invoices/1")
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.invoiceNumber").value("INV-1"))
            .andExpect(jsonPath("$.userId").value(1L))
            .andExpect(jsonPath("$.tickets", hasSize(2)))
            .andExpect(jsonPath("$.tickets[0].id").value(1000L))
            .andExpect(jsonPath("$.merchandiseItems", hasSize(1)))
            .andExpect(jsonPath("$.merchandiseItems[0].name").value("T-Shirt"))
            .andExpect(jsonPath("$.merchandiseItems[0].quantity").value(2));
    }

    @Test
    void findInvoiceById_notMyInvoice_returns403() throws Exception {
        when(invoiceService.getInvoiceDetailsForUser(eq(1L), eq("user@test.com")))
            .thenThrow(new AccessDeniedException("Access denied for invoice: 1"));

        mockMvc.perform(get("/api/v1/invoices/1")
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied for invoice: 1"));
    }

    @Test
    void createCreditInvoice_returnsPdf() throws Exception {
        List<Long> ticketIds = List.of(1L, 2L);

        Invoice creditInvoice = new Invoice();
        creditInvoice.setId(10L);
        creditInvoice.setInvoiceNumber("CR-1");

        byte[] pdfBytes = "PDF_CONTENT".getBytes();

        when(invoiceService.createCreditInvoice(ticketIds, "user@test.com"))
            .thenReturn(creditInvoice);
        when(pdfService.generateCreditInvoicePdf(creditInvoice)).thenReturn(pdfBytes);

        mockMvc.perform(post("/api/v1/invoices/credit")
                .principal(new TestingAuthenticationToken("user@test.com", "pw"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketIds)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=Storno-Rechnung.pdf"))
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void createCreditInvoice_handlesValidationException() throws Exception {
        List<Long> ticketIds = List.of(1L);

        when(invoiceService.createCreditInvoice(ticketIds, "user@test.com"))
            .thenThrow(new ValidationException("Invalid"));

        mockMvc.perform(post("/api/v1/invoices/credit")
                .principal(new TestingAuthenticationToken("user@test.com", "pw"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketIds)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().string("Invalid"));
    }

    @Test
    void createCreditInvoice_handlesNotFoundException() throws Exception {
        List<Long> ticketIds = List.of(1L);

        when(invoiceService.createCreditInvoice(ticketIds, "user@test.com"))
            .thenThrow(new NotFoundException("Not found"));

        mockMvc.perform(post("/api/v1/invoices/credit")
                .principal(new TestingAuthenticationToken("user@test.com", "pw"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketIds)))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Not found"));
    }

    @Test
    void downloadCreditInvoice_returnsPdf() throws Exception {
        Invoice creditInvoice = new Invoice();
        creditInvoice.setId(20L);
        creditInvoice.setInvoiceNumber("CR-123");

        byte[] pdfBytes = "PDF".getBytes();

        when(invoiceService.findCreditInvoiceForUserWithTickets(20L, "user@test.com"))
            .thenReturn(creditInvoice);
        when(pdfService.generateCreditInvoicePdf(creditInvoice))
            .thenReturn(pdfBytes);

        mockMvc.perform(get("/api/v1/invoices/credit/20/download")
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=Stornorechnung-CR-123.pdf"))
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void getMyCreditInvoices_returnsList() throws Exception {
        CreditInvoiceDto dto = new CreditInvoiceDto(
            1L,
            "CR-1",
            LocalDate.now(),
            List.of()
        );

        when(invoiceService.getMyCreditInvoices("user@test.com"))
            .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/invoices/my/credits")
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].invoiceNumber").value("CR-1"));
    }

    @Test
    void getMyMerchandiseInvoices_returnsList() throws Exception {
        SimpleInvoiceDto dto = new SimpleInvoiceDto(1L, "INV-M1", 1L);

        when(invoiceService.getMyMerchandiseInvoices("user@test.com"))
            .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/invoices/my/merchandise")
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].invoiceNumber").value("INV-M1"));
    }

}




