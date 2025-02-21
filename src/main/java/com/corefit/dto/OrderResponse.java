package com.corefit.dto;

import com.corefit.entity.Market;
import com.corefit.entity.User;
import com.corefit.enums.OrderStatus;
import com.corefit.enums.PaymentMethod;

import java.util.List;


public class OrderResponse {
    private Long id;
    private User user;
    private String clientName;
    private String clientAddress;
    private String clientPhone;
    private Double latitude;
    private Double longitude;
    private String additionalInfo;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private Double totalPrice;
    private Market market;
    private List<OrderItemResponse> orderItems ;

    public OrderResponse() {
    }

    public OrderResponse(Long id, User user, String clientName, String clientAddress, String clientPhone, Double latitude, Double longitude, String additionalInfo, OrderStatus status, PaymentMethod paymentMethod, Double totalPrice, Market market, List<OrderItemResponse> orderItems) {
        this.id = id;
        this.user = user;
        this.clientName = clientName;
        this.clientAddress = clientAddress;
        this.clientPhone = clientPhone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.additionalInfo = additionalInfo;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.totalPrice = totalPrice;
        this.market = market;
        this.orderItems = orderItems;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public String getClientPhone() {
        return clientPhone;
    }

    public void setClientPhone(String clientPhone) {
        this.clientPhone = clientPhone;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Market getMarket() {
        return market;
    }

    public void setMarket(Market market) {
        this.market = market;
    }

    public List<OrderItemResponse> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemResponse> orderItems) {
        this.orderItems = orderItems;
    }
}
