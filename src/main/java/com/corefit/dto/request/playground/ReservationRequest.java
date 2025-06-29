package com.corefit.dto.request.playground;

import com.corefit.enums.PaymentMethod;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
public class ReservationRequest {
    private Long playgroundId;
    private LocalDate date;
    private List<String> slots;
    private PaymentMethod paymentMethod;
}
