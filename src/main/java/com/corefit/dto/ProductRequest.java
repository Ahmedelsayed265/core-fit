package com.corefit.dto;




public class ProductRequest {
    private long id;
    private String name;
    private String description;
    private double price;
    private int offer;
    private Long marketId;
    private Long subCategoryId;
    private boolean isHidden;

    public ProductRequest() {
    }

    public ProductRequest(long id, String name, String description, double price, int offer, Long marketId, Long subCategoryId, boolean isHidden) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.offer = offer;
        this.marketId = marketId;
        this.subCategoryId = subCategoryId;
        this.isHidden = isHidden;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getOffer() {
        return offer;
    }

    public void setOffer(int offer) {
        this.offer = offer;
    }

    public Long getMarketId() {
        return marketId;
    }

    public void setMarketId(Long marketId) {
        this.marketId = marketId;
    }

    public Long getSubCategoryId() {
        return subCategoryId;
    }

    public void setSubCategoryId(Long subCategoryId) {
        this.subCategoryId = subCategoryId;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }
}
