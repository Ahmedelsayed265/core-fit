package com.corefit.dto.request;

import com.corefit.enums.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequest {
    private String clientName;
    private String clientAddress;
    private String clientPhone;
    private Double latitude;
    private Double longitude;
    private String additionalInfo;
    private PaymentMethod paymentMethod;
}
