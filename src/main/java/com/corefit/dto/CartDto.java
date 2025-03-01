package com.corefit.dto;

import java.util.List;



public class CartDto {
    private long id;
    private Long marketId;
    private List<CartItemDto> products;
    private double totalPrice;

    public CartDto(long id, Long marketId, List<CartItemDto> products, double totalPrice) {
        this.id = id;
        this.marketId = marketId;
        this.products = products;
        this.totalPrice = totalPrice;
    }

    public CartDto() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getMarketId() {
        return marketId;
    }

    public void setMarketId(Long marketId) {
        this.marketId = marketId;
    }

    public List<CartItemDto> getProducts() {
        return products;
    }

    public void setProducts(List<CartItemDto> products) {
        this.products = products;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
