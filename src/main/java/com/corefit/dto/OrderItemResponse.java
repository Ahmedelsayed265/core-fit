package com.corefit.dto;

import java.util.List;


public class OrderItemResponse {
    private long id;
    private String name;
    private String description;
    private double price;
    private int offer;
    private String subCategoryName;
    private List<String> images;
    private int count;
    private double total;

    public OrderItemResponse() {
    }

    public OrderItemResponse(long id, String name, String description, double price, int offer, String subCategoryName, List<String> images, int count, double total) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.offer = offer;
        this.subCategoryName = subCategoryName;
        this.images = images;
        this.count = count;
        this.total = total;
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

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
