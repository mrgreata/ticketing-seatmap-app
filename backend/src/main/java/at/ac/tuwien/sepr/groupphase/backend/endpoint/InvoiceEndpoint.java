package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.CreditInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.DetailedInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.InvoiceCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.SimpleInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.service.InvoiceService;
import at.ac.tuwien.sepr.groupphase.backend.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoint for managing invoices.
 * Provides operations for retrieving, creating, downloading, and crediting invoices.
 * All endpoints require the user to be authenticated.
 */
@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceEndpoint {

    private final InvoiceService invoiceService;
    private final PdfService pdfService;
    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceEndpoint.class);

    /**
     * Creates a new invoice endpoint with the required services.
     *
     * @param invoiceService the service handling invoice business logic
     * @param pdfService the service responsible for PDF generation
     */
    public InvoiceEndpoint(
        InvoiceService invoiceService,
        PdfService pdfService
    ) {
        this.invoiceService = invoiceService;
        this.pdfService = pdfService;
    }

    /**
     * Returns a list of detailed invoices for the authenticated user.
     *
     * @param auth the current authenticated user
     * @return list of detailed invoices
     */
    @Secured("ROLE_USER")
    @GetMapping("/my")
    public ResponseEntity<List<DetailedInvoiceDto>> getMyInvoices(Authentication auth) {
        LOGGER.info("Fetching invoices for user {}", auth.getName());
        List<DetailedInvoiceDto> invoices = invoiceService.getMyInvoices(auth.getName());
        return ResponseEntity.ok(invoices);
    }

    /**
     * Returns a list of credit invoices (e.g., from ticket cancellations) for the authenticated user.
     *
     * @param auth the current authenticated user
     * @return list of credit invoices
     */
    @Secured("ROLE_USER")
    @GetMapping("/my/credits")
    public List<CreditInvoiceDto> getMyCreditInvoices(Authentication auth) {
        LOGGER.info("Fetching credit invoices for user {}", auth.getName());
        return invoiceService.getMyCreditInvoices(auth.getName());
    }

    /**
     * Retrieves detailed information for a specific invoice of the
     * authenticated user.
     *
     * @param id the ID of the invoice
     * @param auth the authentication object of the current user
     * @return the invoice details as a {@link DetailedInvoiceDto}
     * @throws AccessDeniedException
     *         if the invoice does not belong to the user
     */
    @Secured("ROLE_USER")
    @GetMapping("/{id}")
    public DetailedInvoiceDto findById(@PathVariable("id") Long id, Authentication auth) {
        LOGGER.info("Fetching invoice {} for user {}", id, auth.getName());
        return invoiceService.getInvoiceDetailsForUser(id, auth.getName());
    }


    /**
     * Creates a new invoice.
     *
     * @param dto the invoice creation request data
     * @return a {@link SimpleInvoiceDto} representing the created invoice
     */
    @Secured("ROLE_USER")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SimpleInvoiceDto create(@RequestBody InvoiceCreateDto dto) {
        LOGGER.info("Creating invoice with data {}", dto);
        return invoiceService.create(dto);
    }


    /**
     * Downloads the PDF representation of an invoice.
     *
     * @param id the ID of the invoice
     * @param auth the authentication object of the current user
     * @return a {@link ResponseEntity} containing the invoice PDF
     */
    @Secured("ROLE_USER")
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable("id") Long id, Authentication auth) {
        LOGGER.info("Downloading invoice PDF {} for user {}", id, auth.getName());

        Invoice invoice = invoiceService.findById(id, auth.getName());
        byte[] pdf = invoiceService.downloadInvoicePdf(invoice.getId());
        String safeFile = invoice.getInvoiceNumber().replaceAll("[^a-zA-Z0-9-_]", "_");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Rechnung-" + safeFile + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }


    /**
     * Creates a credit invoice (storno invoice) for the given tickets and
     * returns the generated PDF.
     *
     * @param ticketIds the IDs of the tickets to cancel
     * @param auth the authentication object of the current user
     * @return a {@link ResponseEntity} containing the credit invoice PDF
     */
    @Secured("ROLE_USER")
    @PostMapping(value = "/credit", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> createCreditInvoice(@RequestBody List<Long> ticketIds, Authentication auth) {
        LOGGER.info("Creating credit invoice for tickets {} by user {}", ticketIds, auth.getName());

        try {
            Invoice invoice = invoiceService.createCreditInvoice(ticketIds, auth.getName());
            byte[] pdf = pdfService.generateCreditInvoicePdf(invoice);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Storno-Rechnung.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);

        } catch (ValidationException e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage().getBytes());
        } catch (NotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage().getBytes());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage().getBytes());
        }

    }


    /**
     * Downloads the PDF representation of a credit invoice.
     *
     * @param id the ID of the credit invoice
     * @param auth the authentication object of the current user
     * @return a {@link ResponseEntity} containing the credit invoice PDF
     */
    @Secured("ROLE_USER")
    @GetMapping("/credit/{id}/download")
    public ResponseEntity<byte[]> downloadCreditInvoice(@PathVariable("id") Long id, Authentication auth) {
        LOGGER.info("Downloading credit invoice PDF {} for user {}", id, auth.getName());

        Invoice creditInvoice = invoiceService.findCreditInvoiceForUserWithTickets(id, auth.getName());
        byte[] pdf = pdfService.generateCreditInvoicePdf(creditInvoice);
        String safeFile = creditInvoice.getInvoiceNumber().replaceAll("[^a-zA-Z0-9-_]", "_");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Stornorechnung-" + safeFile + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    /**
     * Retrieves all merchandise-related invoices belonging to the
     * authenticated user.
     *
     * @param auth the authentication object of the current user
     * @return a list of {@link SimpleInvoiceDto} representing merchandise invoices
     */
    @Secured("ROLE_USER")
    @GetMapping("/my/merchandise")
    public List<SimpleInvoiceDto> getMyMerchandiseInvoices(Authentication auth) {
        LOGGER.info("Fetching merchandise invoices for user {}", auth.getName());
        return invoiceService.getMyMerchandiseInvoices(auth.getName());
    }
}
