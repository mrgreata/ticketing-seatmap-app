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
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @NotNull
    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    // Relation: Seat → belongs to Sector
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    public Seat() {
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_category_id")
    private PriceCategory priceCategory;

    public PriceCategory getPriceCategory() {
        return priceCategory;
    }

    public void setPriceCategory(PriceCategory priceCategory) {
        this.priceCategory = priceCategory;
    }



    public Seat(int rowNumber, int seatNumber, Sector sector) {
        this.rowNumber = rowNumber;
        this.seatNumber = seatNumber;
        this.sector = sector;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Sector getSector() {
        return sector;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Seat seat)) {
            return false;
        }
        return id != null && id.equals(seat.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}