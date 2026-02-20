package at.ac.tuwien.sepr.groupphase.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 255)
    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User user;

    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Ticket> tickets = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CancelledTicket> cancelledTickets = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<InvoiceMerchandiseItem> merchandiseItems = new ArrayList<>();

    @NotNull
    @Column(name = "net_total", nullable = false)
    private Double netTotal = 0.0;

    @NotNull
    @Column(name = "tax_total", nullable = false)
    private Double taxTotal = 0.0;

    @NotNull
    @Column(name = "gross_total", nullable = false)
    private Double grossTotal = 0.0;


    @NotNull
    @Column(name = "invoice_date", nullable = false)
    private java.time.LocalDate invoiceDate;


    @Column(name = "invoice_cancellation_date")
    private java.time.LocalDate invoiceCancellationDate;

    @Column(name = "originalInvoiceNumber")
    private String originalInvoiceNumber;

    @NotNull
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    public Invoice() {
    }

    public Invoice(User user, java.time.LocalDate invoiceDate, LocalDateTime eventDateDate) {
        this.user = user;
        this.invoiceDate = invoiceDate;
        this.eventDate = eventDateDate;
    }

    public Invoice(User user, String invoiceNumber) {
        this.user = user;
        this.invoiceNumber = invoiceNumber;
    }

    public void setInvoiceCancellationDate(LocalDate invoiceCancellationDate) {
        this.invoiceCancellationDate = invoiceCancellationDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public List<InvoiceMerchandiseItem> getMerchandiseItems() {
        return merchandiseItems;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public void setTaxTotal(Double taxTotal) {
        this.taxTotal = taxTotal;
    }

    public void setGrossTotal(Double grossTotal) {
        this.grossTotal = grossTotal;
    }

    public java.time.LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(java.time.LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public void addMerchandiseItem(InvoiceMerchandiseItem merchandiseItem) {
        merchandiseItems.add(merchandiseItem);
        merchandiseItem.setInvoice(this);
    }


    public void addCancelledTicket(CancelledTicket cancelledTicket) {
        cancelledTickets.add(cancelledTicket);
    }

    public List<CancelledTicket> getCancelledTickets() {
        return cancelledTickets;
    }

    public void setOriginalInvoiceNumber(String originalInvoiceId) {
        this.originalInvoiceNumber = originalInvoiceId;
    }

    public String getOriginalInvoiceNumber() {
        return originalInvoiceNumber;
    }

    public LocalDate getInvoiceCancellationDate() {
        return invoiceCancellationDate;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Invoice invoice)) {
            return false;
        }
        return id != null && id.equals(invoice.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}