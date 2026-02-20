package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for accessing {@link Invoice} entities.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Retrieves all invoices for a user, ordered by invoice date in descending order.
     *
     * @param email the email address identifying the user
     * @return a list of {@link Invoice} entities ordered by invoice date (newest first)
     */
    List<Invoice> findAllByUserEmailOrderByInvoiceDateDesc(String email);

    /**
     * Retrieves a specific invoice for a given user.
     * This method is typically used to enforce ownership-based access control.
     *
     * @param id the ID of the invoice
     * @param email the email address identifying the user
     * @return an {@link Optional} containing the invoice if it belongs to the user
     */
    Optional<Invoice> findByIdAndUserEmail(Long id, String email);

    /**
     * Retrieves all credit invoices for a user by user ID.
     *
     * @param userId the ID of the user
     * @return a list of credit {@link Invoice} entities
     */
    List<Invoice> findByUserIdAndOriginalInvoiceNumberIsNotNull(Long userId);

    /**
     * Retrieves all credit invoices for a user by email address.
     *
     * @param email the email address identifying the user
     * @return a list of credit {@link Invoice} entities
     */
    List<Invoice> findByUserEmailAndOriginalInvoiceNumberIsNotNull(String email);


    @Query("""
        select distinct i
        from Invoice i
        join i.tickets t
        join fetch i.user
        where i.user.email = :email
        order by i.invoiceDate desc
        """)
    List<Invoice> findAllActiveByUserEmailOrderByInvoiceDateDesc(@Param("email") String email);

}