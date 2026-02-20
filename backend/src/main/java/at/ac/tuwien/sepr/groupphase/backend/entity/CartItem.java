package at.ac.tuwien.sepr.groupphase.backend.entity;

import at.ac.tuwien.sepr.groupphase.backend.type.CartItemType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"cart_id", "type", "merchandise_id"}),
        @UniqueConstraint(columnNames = {"cart_id", "type", "ticketId"})
    }
)
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private CartItemType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchandise_id")
    private Merchandise merchandise;

    @Column(name = "quantity")
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @NotNull
    @Column(name = "redeem_with_points", nullable = false)
    private Boolean redeemWithPoints = false;

    public CartItem() {
    }

    public CartItem(CartItemType type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public CartItemType getType() {
        return type;
    }

    public void setType(CartItemType type) {
        this.type = type;
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

    public Ticket getTicket() {
        return ticket;
    }

    public Long getTicketId() {
        return ticket.getId();
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public Boolean getRedeemWithPoints() {
        return redeemWithPoints;
    }

    public void setRedeemWithPoints(Boolean redeemWithPoints) {
        this.redeemWithPoints = redeemWithPoints;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CartItem that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
