package com.corefit.entity.market;

import com.corefit.entity.User;
import com.corefit.enums.OrderStatus;
import com.corefit.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private User user;

    private String clientName;

    private String clientAddress;

    private String clientPhone;

    private double latitude;

    private double longitude;

    private String additionalInfo;

    @CreatedDate
    private Date createdAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.ORDER_RECEIVED;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private double totalPrice;

    @ManyToOne
    private Market market;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

}
