package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.SimpleInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.InvoiceCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.DetailedInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.CreditInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase.MerchandisePurchaseItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase.PaymentDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.type.PaymentMethod;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;


/**
 * Service interface for managing invoices and invoice-related business logic.
 */
public interface InvoiceService {

    /**
     * Retrieves all invoices in the system.
     * This method is typically restricted to administrative use.
     *
     * @return a list of all {@link Invoice} entities
     */
    List<Invoice> findAll();


    /**
     * Finds a specific invoice for a user by ID.
     *
     * @param id        Invoice ID.
     * @param userEmail Email of the user requesting the invoice.
     * @return The invoice entity.
     */
    Invoice findById(Long id, String userEmail);

    /**
     * Creates a new invoice based on provided DTO.
     *
     * @param invoice DTO containing invoice creation data.
     * @return SimpleInvoiceDto representing the saved invoice.
     */
    SimpleInvoiceDto create(InvoiceCreateDto invoice);

    /**
     * Retrieves all invoices belonging to a specific user.
     *
     * @param userEmail the email address identifying the user
     * @return a list of {@link DetailedInvoiceDto} for the user
     */
    List<DetailedInvoiceDto> getMyInvoices(String userEmail);

    /**
     * Retrieves all merchandise-related invoices belonging to a specific user.
     *
     * @param userEmail the email address identifying the user
     * @return a list of {@link SimpleInvoiceDto} representing merchandise invoices
     */
    List<SimpleInvoiceDto> getMyMerchandiseInvoices(String userEmail);

    /**
     * Retrieves detailed invoice information for a user-authorized invoice.
     *
     * @param invoiceId the ID of the invoice
     * @param userEmail the email address identifying the user
     * @return a {@link DetailedInvoiceDto} containing full invoice details
     * @throws AccessDeniedException if the invoice does not belong to the given user
     */
    DetailedInvoiceDto getInvoiceDetailsForUser(Long invoiceId, String userEmail);

    /**
     * Retrieves the invoice entity for a user-authorized invoice.
     *
     * @param invoiceId the ID of the invoice
     * @param userEmail the email address identifying the user
     * @return the {@link Invoice} entity
     * @throws AccessDeniedException if the invoice does not belong to the given user
     */
    Invoice getInvoiceEntityForUser(Long invoiceId, String userEmail);

    /**
     * Creates a credit invoice (storno invoice) for the given tickets.
     * Credit invoices are used to reverse previously issued invoices,
     * typically in the context of ticket cancellations.
     *
     * @param ticketIds the IDs of the tickets to be credited
     * @param userEmail the email address identifying the user
     * @return the created credit {@link Invoice} entity
     * @throws AccessDeniedException if the user is not authorized to cancel the given tickets
     */
    Invoice createCreditInvoice(List<Long> ticketIds, String userEmail) throws AccessDeniedException;


    /**
     * Retrieves all credit invoices of a user.
     *
     * @param userEmail Email of the user.
     * @return List of CreditInvoiceDto.
     */
    List<CreditInvoiceDto> getMyCreditInvoices(String userEmail);

    /**
     * Generates and returns the PDF representation of an invoice.
     *
     * @param invoiceId the ID of the invoice
     * @return a byte array containing the PDF data
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException
     *         if no invoice with the given ID exists
     */
    byte[] downloadInvoicePdf(Long invoiceId);

    /**
     * Creates an invoice for a merchandise purchase, optionally including
     * reward-based items.
     * This operation validates reward point usage, processes payment,
     * updates inventory and reward balances, and persists the resulting invoice.
     *
     * @param user          the purchasing user
     * @param merchItems    the list of merchandise items purchased normally
     * @param rewardItems   the list of merchandise items redeemed with reward points
     * @param paymentMethod the selected payment method
     * @param paymentDetail additional payment details, if required
     * @return the created {@link Invoice} entity
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException if reward points or payment data are invalid
     */
    Invoice purchaseMerchandiseWithRewards(
        User user,
        List<MerchandisePurchaseItemDto> merchItems,
        List<MerchandisePurchaseItemDto> rewardItems,
        PaymentMethod paymentMethod,
        PaymentDetailDto paymentDetail
    );


    /**
     * Validates the provided payment data for the given payment method.
     * This method performs syntactic and semantic validation of the supplied
     * {@link PaymentDetailDto} according to the selected {@link PaymentMethod}.
     * Checks include format validation (e.g. credit card number, expiry
     * date) and basic consistency rules.
     *
     * @param paymentMethod the payment method to validate against
     * @param paymentDetail the payment details to validate
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException if the payment data is invalid or incomplete
     */
    void validatePaymentData(PaymentMethod paymentMethod, PaymentDetailDto paymentDetail);

    /**
     * Finds a credit invoice of a user with its cancelled tickets.
     *
     * @param id    Invoice ID.
     * @param email Email of the user.
     * @return Invoice entity with cancelled tickets loaded.
     */
    Invoice findCreditInvoiceForUserWithTickets(Long id, String email);

}