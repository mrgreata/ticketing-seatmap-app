package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.entity.CancelledTicket;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.service.PdfService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class PdfServiceImpl implements PdfService {

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm");

    // =========================
    // PDF Rendering
    // =========================
    public byte[] generateInvoicePdfFromHtml(String htmlContent) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Generieren der PDF aus HTML", e);
        }
    }

    // =========================
    // Invoice HTML Routing
    // =========================
    public String buildInvoiceHtml(Invoice invoice) {
        if (!invoice.getTickets().isEmpty()) {
            return buildTicketInvoiceHtml(invoice);
        }

        if (!invoice.getMerchandiseItems().isEmpty()) {
            return buildMerchInvoiceHtml(invoice);
        }

        throw new IllegalStateException("Invoice has no items");
    }

    // =========================
    // Shared HTML Helpers (CHANGED)
    // =========================
    private String companyBlockHtml() {
        return """
            <div class="company">
                <p><strong>Firma:</strong> Ticketline 4.0</p>
                <p><strong>Anschrift:</strong> Karlsplatz 13, 1040 Wien, Österreich</p>
            </div>
            """;
    }

    private String addressHtml(Invoice invoice) {
        if (invoice.getUser().getAddress() != null && !invoice.getUser().getAddress().isBlank()) {
            return "<p><strong>Adresse:</strong> " + esc(invoice.getUser().getAddress()) + "</p>";
        }
        return "";
    }

    private String commonCss(boolean isCredit) {
        String headingColor = isCredit ? "red" : "inherit";

        return """
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; color: #111; }
                h1, h2, h3 { text-align: center; color: %s; margin: 10px 0; }

                .invoice-title {
                    font-size: 22px;
                    font-weight: 700;
                    line-height: 1.25;
                    white-space: normal;
                    overflow-wrap: anywhere;
                    word-break: break-word;
                }

                .company { margin-bottom: 24px; }

                table {
                    width: 100%%;
                    border-collapse: collapse;
                    margin-top: 20px;
                    table-layout: fixed;
                }

                th, td {
                    border: 1px solid #ccc;
                    padding: 8px;
                    text-align: left;
                    vertical-align: top;
                    white-space: normal;
                    overflow-wrap: anywhere;
                    word-wrap: break-word;
                    word-break: break-word;
                }

                th { background: #f5f5f5; }

                .wrap { word-break: break-all; }

                .col-service { width: 28%%; }
                .col-seats   { width: 24%%; }
                .col-qty     { width: 8%%;  }
                .col-date    { width: 12%%; }
                .col-time    { width: 10%%; }
                .col-price   { width: 18%%; }

                .col-article { width: 30%%; }
                .col-desc    { width: 45%%; }
                .col-qty-m   { width: 10%%; }
                .col-price-m { width: 15%%; }
            </style>
            """.formatted(headingColor);
    }

    // =========================
    // Ticket Invoice
    // =========================
    public String buildTicketInvoiceHtml(Invoice invoice) {

        String today = LocalDate.now().format(DATE_FORMAT);

        Map<Double, Double> taxByRate = invoice.getTickets().stream()
            .collect(Collectors.groupingBy(
                Ticket::getTaxRate,
                Collectors.summingDouble(t -> t.getGrossPrice() - t.getNetPrice())
            ));

        String taxRowsHtml = taxByRate.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> "<h3>Steuer (" + String.format("%.0f", e.getKey() * 100) + " % MwSt): "
                + String.format("%.2f €", e.getValue()) + "</h3>")
            .collect(Collectors.joining());

        Map<String, List<Ticket>> ticketsByEvent = invoice.getTickets().stream()
            .collect(Collectors.groupingBy(t ->
                t.getEvent().getTitle() + "|" + t.getEvent().getDateTime()
            ));

        String ticketsHtml = ticketsByEvent.values().stream()
            .flatMap(eventTickets -> {
                Ticket eventTicket = eventTickets.get(0);
                Map<Double, List<Ticket>> byPrice = eventTickets.stream()
                    .collect(Collectors.groupingBy(Ticket::getGrossPrice));

                return byPrice.entrySet().stream().map(priceEntry -> {
                    List<Ticket> tickets = priceEntry.getValue();

                    String seatInfo = tickets.stream()
                        .map(t -> esc(t.getSeat().getRowNumber() + "/" + t.getSeat().getSeatNumber()))
                        .collect(Collectors.joining(", "));

                    double unitPrice = priceEntry.getKey();
                    double totalPrice = tickets.stream().mapToDouble(Ticket::getGrossPrice).sum();

                    String eventDate = eventTicket.getEvent().getDateTime().toLocalDate().format(DATE_FORMAT);
                    String eventTime = eventTicket.getEvent().getDateTime().toLocalTime().format(TIME_FORMAT);

                    String priceDisplay = tickets.size() + " x "
                        + String.format("%.2f €", unitPrice)
                        + " = " + String.format("%.2f €", totalPrice);

                    return "<tr>"
                        + "<td class=\"wrap col-service\">" + esc(eventTicket.getEvent().getTitle()) + "</td>"
                        + "<td class=\"wrap col-seats\">" + esc(seatInfo) + "</td>"
                        + "<td class=\"col-qty\">" + tickets.size() + "</td>"
                        + "<td class=\"col-date\">" + eventDate + "</td>"
                        + "<td class=\"col-time\">" + eventTime + "</td>"
                        + "<td class=\"wrap col-price\">" + priceDisplay + "</td>"
                        + "</tr>";
                });
            })
            .collect(Collectors.joining());

        double netTotal = invoice.getTickets().stream().mapToDouble(Ticket::getNetPrice).sum();
        double grossTotal = invoice.getTickets().stream().mapToDouble(Ticket::getGrossPrice).sum();

        return """
            <html>
            <head>
                %s
            </head>
            <body>

            %s

            <h1 class="invoice-title">Rechnung %s</h1>

            <p><strong>Kunde:</strong> %s %s</p>
            %s
            <p><strong>Rechnungsdatum:</strong> %s</p>

            <table>
                <tr>
                    <th class="wrap col-service">Art der Dienstleistung</th>
                    <th class="wrap col-seats">Sitzplätze</th>
                    <th class="col-qty">Menge</th>
                    <th class="col-date">Datum</th>
                    <th class="col-time">Uhrzeit</th>
                    <th class="wrap col-price">Preis inkl. Steuer</th>
                </tr>
                %s
            </table>

            <h3>Netto: %.2f €</h3>
            %s
            <h2>Gesamt: %.2f €</h2>

            </body>
            </html>
            """.formatted(
            commonCss(false),
            companyBlockHtml(),
            esc(invoice.getInvoiceNumber()),
            esc(invoice.getUser().getFirstName()),
            esc(invoice.getUser().getLastName()),
            addressHtml(invoice),
            today,
            ticketsHtml,
            netTotal,
            taxRowsHtml,
            grossTotal
        );
    }

    // =========================
    // Merchandise Invoice
    // =========================
    private String buildMerchInvoiceHtml(Invoice invoice) {

        String today = LocalDate.now().format(DATE_FORMAT);

        AtomicReference<Double> grossSum = new AtomicReference<>(0.0);
        AtomicReference<Double> netSum = new AtomicReference<>(0.0);
        AtomicReference<Double> taxSum = new AtomicReference<>(0.0);

        String rows = invoice.getMerchandiseItems().stream()
            .map(item -> {
                var merch = item.getMerchandise();
                int qty = item.getQuantity();

                boolean redeemed = Boolean.TRUE.equals(item.getRedeemedWithPoints());

                double grossUnit;
                double grossTotal;
                double netTotal;
                double taxTotal;

                if (redeemed) {
                    grossUnit = 0.0;
                    grossTotal = 0.0;
                    netTotal = 0.0;
                    taxTotal = 0.0;
                } else {
                    grossUnit = merch.getUnitPrice().doubleValue();
                    grossTotal = grossUnit * qty;

                    netTotal = grossTotal / 1.10;
                    taxTotal = grossTotal - netTotal;

                    grossSum.updateAndGet(v -> v + grossTotal);
                    netSum.updateAndGet(v -> v + netTotal);
                    taxSum.updateAndGet(v -> v + taxTotal);
                }

                String name = esc(merch.getName()) + (redeemed ? " (Prämie)" : "");
                String desc = esc(merch.getDescription());

                String unitDisplay = String.format("%.2f €", grossUnit);
                String totalDisplay = String.format("%.2f €", grossTotal);

                String priceDisplay = qty + " x " + unitDisplay + " = " + totalDisplay;

                return "<tr>"
                    + "<td class=\"wrap col-article\">" + name + "</td>"
                    + "<td class=\"wrap col-desc\">" + desc + "</td>"
                    + "<td class=\"col-qty-m\">" + qty + "</td>"
                    + "<td class=\"wrap col-price-m\">" + priceDisplay + "</td>"
                    + "</tr>";
            })
            .collect(Collectors.joining());

        return """
            <html>
            <head>
                %s
            </head>
            <body>

            %s

            <h1 class="invoice-title">Rechnung %s</h1>

            <p><strong>Kunde:</strong> %s %s</p>
            %s
            <p><strong>Rechnungsdatum:</strong> %s</p>

            <table>
                <tr>
                    <th class="wrap col-article">Artikel</th>
                    <th class="wrap col-desc">Beschreibung</th>
                    <th class="col-qty-m">Menge</th>
                    <th class="wrap col-price-m">Preis inkl. Steuer</th>
                </tr>
                %s
            </table>

            <h3>Netto: %.2f €</h3>
            <h3>Steuer (10%% MwSt): %.2f €</h3>
            <h2>Gesamt: %.2f €</h2>

            </body>
            </html>
            """.formatted(
            commonCss(false),
            companyBlockHtml(),
            esc(invoice.getInvoiceNumber()),
            esc(invoice.getUser().getFirstName()),
            esc(invoice.getUser().getLastName()),
            addressHtml(invoice),
            today,
            rows,
            netSum.get(),
            taxSum.get(),
            grossSum.get()
        );
    }

    // =========================
    // Credit Invoice (Ticket Cancellation)
    // =========================
    public String buildCreditInvoiceHtml(Invoice creditInvoice) {

        String today = LocalDate.now().format(DATE_FORMAT);
        String originalDate = creditInvoice.getInvoiceDate().format(DATE_FORMAT);

        Map<Double, Double> taxByRate = creditInvoice.getCancelledTickets().stream()
            .collect(Collectors.groupingBy(
                CancelledTicket::getTaxRate,
                Collectors.summingDouble(t -> t.getGrossPrice() - t.getNetPrice())
            ));

        String taxRowsHtml = taxByRate.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> "<h3>Steuer (" + String.format("%.0f", e.getKey() * 100) + " % MwSt): -"
                + String.format("%.2f €", e.getValue()) + "</h3>")
            .collect(Collectors.joining());

        Map<String, List<CancelledTicket>> groupedTickets = creditInvoice.getCancelledTickets().stream()
            .collect(Collectors.groupingBy(t -> t.getEventName() + "|" + t.getEventDate()));

        String ticketsHtml = groupedTickets.entrySet().stream()
            .map(entry -> {
                List<CancelledTicket> tickets = entry.getValue();
                CancelledTicket firstTicket = tickets.get(0);

                String seatInfo = tickets.stream()
                    .map(CancelledTicket::getSeat)
                    .collect(Collectors.joining(", "));

                String eventDate = firstTicket.getEventDate().toLocalDate().format(DATE_FORMAT);
                String eventTime = firstTicket.getEventDate().toLocalTime().format(TIME_FORMAT);

                double unitPrice = firstTicket.getGrossPrice();
                double totalPrice = tickets.stream().mapToDouble(CancelledTicket::getGrossPrice).sum();

                String priceDisplay = "-" + tickets.size() + " x " + String.format("%.2f €", unitPrice)
                    + " = -" + String.format("%.2f €", totalPrice);

                return "<tr>"
                    + "<td class=\"wrap col-service\">" + esc(firstTicket.getEventName()) + "</td>"
                    + "<td class=\"wrap col-seats\">" + esc(seatInfo) + "</td>"
                    + "<td class=\"col-qty\">" + tickets.size() + "</td>"
                    + "<td class=\"col-date\">" + eventDate + "</td>"
                    + "<td class=\"col-time\">" + eventTime + "</td>"
                    + "<td class=\"wrap col-price\">" + priceDisplay + "</td>"
                    + "</tr>";
            })
            .collect(Collectors.joining());

        double netTotal = -creditInvoice.getCancelledTickets().stream().mapToDouble(CancelledTicket::getNetPrice).sum();
        double grossTotal = -creditInvoice.getCancelledTickets().stream().mapToDouble(CancelledTicket::getGrossPrice).sum();

        return """
            <html>
            <head>
                %s
            </head>
            <body>

            %s

            <h1 class="invoice-title">Stornorechnung %s</h1>

            <p><strong>Kunde:</strong> %s %s</p>
            %s
            <p><strong>Originalrechnung:</strong> %s</p>
            <p><strong>Originaldatum:</strong> %s</p>
            <p><strong>Stornodatum:</strong> %s</p>

            <table>
                <tr>
                    <th class="wrap col-service">Art der Dienstleistung</th>
                    <th class="wrap col-seats">Sitzplätze</th>
                    <th class="col-qty">Menge</th>
                    <th class="col-date">Datum</th>
                    <th class="col-time">Uhrzeit</th>
                    <th class="wrap col-price">Preis inkl. Steuer</th>
                </tr>
                %s
            </table>

            <h3>Netto: %.2f €</h3>
            %s
            <h2>Gesamt: %.2f €</h2>

            </body>
            </html>
            """.formatted(
            commonCss(true),
            companyBlockHtml(),
            esc(creditInvoice.getInvoiceNumber()),
            esc(creditInvoice.getUser().getFirstName()),
            esc(creditInvoice.getUser().getLastName()),
            addressHtml(creditInvoice),
            esc(creditInvoice.getOriginalInvoiceNumber()),
            originalDate,
            today,
            ticketsHtml,
            netTotal,
            taxRowsHtml,
            grossTotal
        );
    }

    // =========================
    // PdfService API
    // =========================
    @Override
    public byte[] generateInvoicePdf(Invoice invoice) {

        if (!invoice.getTickets().isEmpty()) {
            return generateInvoicePdfFromHtml(buildTicketInvoiceHtml(invoice));
        }

        if (!invoice.getMerchandiseItems().isEmpty()) {
            return generateInvoicePdfFromHtml(buildMerchInvoiceHtml(invoice));
        }

        throw new IllegalStateException("Invoice has no items");
    }

    @Override
    public byte[] generateCreditInvoicePdf(Invoice creditInvoice) {
        return generateInvoicePdfFromHtml(buildCreditInvoiceHtml(creditInvoice));
    }

    // =========================
    // Escaping
    // =========================
    private String esc(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
