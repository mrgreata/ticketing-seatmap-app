package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;

/**
 * Service for generating PDFs of invoices.
 */
public interface PdfService {

    /**
     * Generates a PDF for a normal invoice.
     *
     * @param invoice The invoice entity to generate the PDF for.
     * @return PDF as a byte array.
     */
    byte[] generateInvoicePdf(Invoice invoice);

    /**
     * Generates a PDF for a credit invoice.
     *
     * @param creditInvoice The credit invoice entity.
     * @return PDF as a byte array.
     */
    byte[] generateCreditInvoicePdf(Invoice creditInvoice);

}
