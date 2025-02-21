package com.corefit.entity;

import jakarta.persistence.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@Table(name = "cart", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id"})})
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne
    @JoinColumn(nullable = false, unique = true)
    private User user;

    @ManyToOne
    private Market market;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();

    private double totalPrice;

    public void addItemToCart(CartItem cartItem) {
        cartItems.add(cartItem);
        updateTotalPrice();
    }

    public void updateTotalPrice() {
        this.totalPrice = cartItems.stream().mapToDouble(CartItem::getTotal).sum();
    }



    /// /////////////////
    public Cart() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Market getMarket() {
        return market;
    }

    public void setMarket(Market market) {
        this.market = market;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
