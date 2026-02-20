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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
    name = "tickets",
    uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "seat_id"})
)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Redundant but useful for querying/filtering without joining seat -> sector -> location.
     * Must be consistent with seat/sector and event/location in service logic.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    // optional: link to reservation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Reservation reservation;

    // optional: link to invoice (if ticket purchased)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @NotNull
    @Column(name = "net_price", nullable = false)
    private Double netPrice;

    @NotNull
    @Column(name = "tax_rate", nullable = false)
    private Double taxRate;

    @NotNull
    @Column(name = "gross_price", nullable = false)
    private Double grossPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    public Ticket() {
    }

    /**
     * dev-style constructor.
     */
    public Ticket(Location location, Seat seat, Event event) {
        this.location = location;
        this.seat = seat;
        this.event = event;
        this.sector = seat.getSector();
    }

    /**
     * seatmap-style constructor (compat)
     * Assumes Event has a non-null location and Seat has a non-null sector.
     */
    public Ticket(Seat seat, Event event) {
        this.seat = seat;
        this.event = event;
        this.sector = seat.getSector();
        this.location = event.getLocation();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Sector getSector() {
        return sector;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Double getNetPrice() {
        return netPrice;
    }

    public void setNetPrice(Double netPrice) {
        this.netPrice = netPrice;
    }

    public Double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(Double taxRate) {
        this.taxRate = taxRate;
    }

    public Double getGrossPrice() {
        return grossPrice;
    }

    public void setGrossPrice(Double grossPrice) {
        this.grossPrice = grossPrice;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ticket ticket)) {
            return false;
        }
        return id != null && id.equals(ticket.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
