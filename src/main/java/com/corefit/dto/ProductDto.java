package com.corefit.dto;


import java.util.List;

public class ProductDto {
    private long id;
    private String name;
    private String description;
    private double price;
    private int offer;
    private String marketName;
    private String subCategoryName;
    private List<String> images;
    private boolean isHidden;

    public ProductDto() {
    }

    public ProductDto(long id, String name, String description, double price, int offer, String marketName, String subCategoryName, List<String> images, boolean isHidden) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.offer = offer;
        this.marketName = marketName;
        this.subCategoryName = subCategoryName;
        this.images = images;
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

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public String getSubCategoryName() {
        return subCategoryName;
    }

    public void setSubCategoryName(String subCategoryName) {
        this.subCategoryName = subCategoryName;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }
}
