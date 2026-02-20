package at.ac.tuwien.sepr.groupphase.backend.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancelled_tickets")
public class CancelledTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    private String eventName;
    private LocalDateTime eventDate;
    private LocalDate cancellationDate;

    @Column(length = 1000)
    private String seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false)
    private Double netPrice;

    @Column(nullable = false)
    private Double taxRate;

    @Column(nullable = false)
    private Double grossPrice;

    public CancelledTicket() {

    }

    public CancelledTicket(User user, String eventName, LocalDateTime eventDate,
                           LocalDate cancellationDate, String seat, Double netPrice, Double taxRate, Double grossPrice, Invoice invoice) {
        this.user = user;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.cancellationDate = cancellationDate;
        this.seat = seat;
        this.netPrice = netPrice;
        this.taxRate = taxRate;
        this.grossPrice = grossPrice;
        this.invoice = invoice;
    }

    // ---------- Getter / Setter ----------

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public LocalDate getCancellationDate() {
        return cancellationDate;
    }

    public String getSeat() {
        return seat;
    }

    public void setSeat(String seat) {
        this.seat = seat;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Double getGrossPrice() {
        return grossPrice;
    }

    public Double getNetPrice() {
        return netPrice;
    }

    public Double getTaxRate() {
        return taxRate;
    }
}
