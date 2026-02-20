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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "invoice_merchandise_items")
public class InvoiceMerchandiseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchandise_id", nullable = false)
    private Merchandise merchandise;

    @NotNull
    @Column(nullable = false)
    private Integer quantity;

    @NotNull
    @Column(nullable = false)
    private Boolean redeemedWithPoints = false;

    public InvoiceMerchandiseItem() {
    }

    public InvoiceMerchandiseItem(Invoice invoice, Merchandise merchandise, Integer quantity) {
        this.invoice = invoice;
        this.merchandise = merchandise;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Merchandise getMerchandise() {
        return merchandise;
    }

    public void setMerchandise(Merchandise merchandise) {
        this.merchandise = merchandise;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Boolean getRedeemedWithPoints() {
        return redeemedWithPoints;
    }

    public void setRedeemedWithPoints(Boolean redeemedWithPoints) {
        this.redeemedWithPoints = redeemedWithPoints;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof InvoiceMerchandiseItem that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}