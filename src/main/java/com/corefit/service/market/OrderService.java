package com.corefit.service.market;

import com.corefit.dto.request.market.ChangeStatusRequest;
import com.corefit.dto.request.market.OrderRequest;
import com.corefit.dto.response.GeneralResponse;
import com.corefit.dto.response.market.OrderItemResponse;
import com.corefit.dto.response.market.OrderResponse;
import com.corefit.dto.response.market.OrdersResponse;
import com.corefit.entity.*;
import com.corefit.entity.market.*;
import com.corefit.enums.OrderStatus;
import com.corefit.enums.PaymentMethod;
import com.corefit.enums.UserType;
import com.corefit.exceptions.GeneralException;
import com.corefit.repository.market.CartRepo;
import com.corefit.repository.market.OrderRepo;
import com.corefit.service.NotificationService;
import com.corefit.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepo orderRepo;
    @Autowired
    private AuthService authService;
    @Autowired
    private CartRepo cartRepo;
    @Autowired
    private CartService cartService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private NotificationService notificationService;

    @Transactional
    public GeneralResponse<?> createOrder(OrderRequest orderRequest, HttpServletRequest httpRequest) {
        User user = authService.extractUserFromRequest(httpRequest);
        Cart cart = Optional.ofNullable(cartRepo.findByUserId(user.getId()))
                .orElseThrow(() -> new GeneralException("Your cart is empty. Please add items before placing an order."));

        Market market = cart.getMarket();

        if (cart.getCartItems().isEmpty()) {
            throw new GeneralException("Your cart is empty. Please add items before placing an order.");
        }

        Order order = buildOrderFromCart(cart, orderRequest, user);

        if (orderRequest.getPaymentMethod() == PaymentMethod.WALLET) {
            processWalletPayment(httpRequest, cart.getTotalPrice());
        }

        orderRepo.save(order);
        cartService.deleteCart(httpRequest);
        pushOrderNotifications(user, market, order);

        return new GeneralResponse<>("Order created successfully");
    }

    public GeneralResponse<?> getOrder(Long orderId, HttpServletRequest httpRequest) {
        User user = authService.extractUserFromRequest(httpRequest);
        Order order = fetchOrderOrThrow(orderId);

        boolean isOrderOwner = Objects.equals(order.getUser().getId(), user.getId());
        boolean isProviderMarket = user.getMarket().stream().anyMatch(m -> Objects.equals(m.getId(), order.getMarket().getId()));

        if (!isOrderOwner && !isProviderMarket) {
            throw new GeneralException("You are not authorized to view this order.");
        }

        return new GeneralResponse<>("Order details retrieved successfully", mapToOrderResponse(order));
    }

    public GeneralResponse<?> getOrders(String status, Long marketId, HttpServletRequest httpRequest) {
        User user = authService.findUserById(authService.extractUserIdFromRequest(httpRequest));
        List<Order> orders;

        if (user.getType() == UserType.GENERAL) {
            orders = "previous".equalsIgnoreCase(status)
                    ? orderRepo.findPreviousOrdersByUserId(user.getId())
                    : orderRepo.findActiveOrdersByUserId(user.getId());
        } else {
            if (marketId == null) {
                throw new GeneralException("Market ID is required for providers.");
            }
            orders = getMarketOrdersByStatus(status, marketId);
        }

        List<OrdersResponse> orderResponses = orders.stream()
                .map(this::mapToOrdersResponse)
                .collect(Collectors.toList());

        return new GeneralResponse<>("Orders retrieved successfully", orderResponses);
    }

    @Transactional
    public GeneralResponse<?> cancelOrder(long orderId, HttpServletRequest httpRequest) {
        User user = authService.findUserById(authService.extractUserIdFromRequest(httpRequest));
        Order order = fetchOrderOrThrow(orderId);

        if (user.getType() != UserType.GENERAL || !order.getUser().getId().equals(user.getId())) {
            throw new GeneralException("You are not authorized to cancel this order.");
        }

        if (order.getStatus() == OrderStatus.ORDER_DELIVERED || order.getStatus() == OrderStatus.ORDER_UNDER_DELIVERY) {
            throw new GeneralException("Delivered orders cannot be canceled.");
        }

        if (order.getStatus() == OrderStatus.ORDER_CANCELED) {
            throw new GeneralException("This order is already canceled.");
        }

        refundWalletIfNeeded(order);
        order.setStatus(OrderStatus.ORDER_CANCELED);
        orderRepo.save(order);

        return new GeneralResponse<>("Order canceled successfully");
    }

    @Transactional
    public GeneralResponse<?> changeStatus(ChangeStatusRequest request, HttpServletRequest httpRequest) {
        User user = authService.findUserById(authService.extractUserIdFromRequest(httpRequest));
        Order order = fetchOrderOrThrow(request.getOrderId());

        if (user.getType() != UserType.PROVIDER) {
            throw new GeneralException("Only providers can update order status.");
        }

        boolean isProviderMarket = user.getMarket().stream().map(Market::getId)
                .anyMatch(id -> Objects.equals(id, order.getMarket().getId()));

        if (!isProviderMarket) {
            throw new GeneralException("This market does not belong to the provider.");
        }

        OrderStatus newStatus = parseOrderStatus(request.getStatus());

        if (order.getStatus() == newStatus) {
            throw new GeneralException("Order is already in the status: " + newStatus);
        }

        if (newStatus.ordinal() <= order.getStatus().ordinal()) {
            throw new GeneralException("Cannot downgrade order status.");
        }

        if (newStatus == OrderStatus.ORDER_CANCELED) {
            if (order.getStatus() == OrderStatus.ORDER_DELIVERED || order.getStatus() == OrderStatus.ORDER_UNDER_DELIVERY) {
                throw new GeneralException("Delivered orders cannot be canceled.");
            }
            refundWalletIfNeeded(order);
        }

        order.setStatus(newStatus);
        orderRepo.save(order);

        return new GeneralResponse<>("Order status updated to: " + newStatus, mapToOrderResponse(order));
    }

    // ------------------------------ Private Helpers ------------------------------

    private Order buildOrderFromCart(Cart cart, OrderRequest request, User user) {
        List<OrderItem> orderItems = cart.getCartItems().stream().map(cartItem -> {
            OrderItem item = new OrderItem();
            item.setQuantity(cartItem.getQuantity());
            item.setProduct(cartItem.getProduct());
            item.setTotal(cartItem.getTotal());
            return item;
        }).collect(Collectors.toList());

        Order order = new Order();
        order.setUser(user);
        order.setLongitude(request.getLongitude());
        order.setLatitude(request.getLatitude());
        order.setClientAddress(request.getClientAddress());
        order.setClientName(request.getClientName());
        order.setClientPhone(request.getClientPhone());
        order.setAdditionalInfo(request.getAdditionalInfo());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setMarket(cart.getMarket());
        order.setTotalPrice(cart.getTotalPrice());
        order.setOrderItems(orderItems);

        orderItems.forEach(item -> item.setOrder(order));
        return order;
    }

    private void processWalletPayment(HttpServletRequest request, double amount) {
        try {
            walletService.withdraw(request, amount);
        } catch (Exception e) {
            throw new GeneralException("Wallet payment failed: " + e.getMessage());
        }
    }

    private void refundWalletIfNeeded(Order order) {
        if (order.getPaymentMethod() == PaymentMethod.WALLET) {
            try {
                walletService.deposit(order.getUser().getId(), order.getTotalPrice());
            } catch (Exception e) {
                throw new GeneralException("Wallet refund failed: " + e.getMessage());
            }
        }
    }

    private OrderStatus parseOrderStatus(String statusStr) {
        return switch (statusStr.toLowerCase()) {
            case "cancelled" -> OrderStatus.ORDER_CANCELED;
            case "confirmed" -> OrderStatus.ORDER_CONFIRMED;
            case "under_prep" -> OrderStatus.ORDER_UNDER_PREPARATION;
            case "under_deliver" -> OrderStatus.ORDER_UNDER_DELIVERY;
            case "delivered" -> OrderStatus.ORDER_DELIVERED;
            default -> throw new GeneralException("Invalid status: " + statusStr);
        };
    }

    private Order fetchOrderOrThrow(Long id) {
        return orderRepo.findById(id).orElseThrow(() -> new GeneralException("Order not found with ID: " + id));
    }

    private List<Order> getMarketOrdersByStatus(String status, Long marketId) {
        return switch (Optional.ofNullable(status).orElse("new").toLowerCase()) {
            case "new" -> orderRepo.findNewMarketOrders(marketId);
            case "current" -> orderRepo.findCurrentMarketOrders(marketId);
            case "completed" -> orderRepo.findCompletedMarketOrders(marketId);
            default -> throw new GeneralException("Invalid status. Allowed: new, current, completed.");
        };
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream().map(item -> new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getDescription(),
                item.getProduct().getPrice(),
                item.getProduct().getOffer(),
                item.getProduct().getSubCategory().getName(),
                item.getProduct().getImages(),
                item.getQuantity(),
                item.getTotal()
        )).collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getMarket().getId(),
                order.getCreatedAt(),
                order.getMarket().getName(),
                order.getClientName(),
                order.getClientAddress(),
                order.getClientPhone(),
                order.getLatitude(),
                order.getLongitude(),
                order.getAdditionalInfo(),
                order.getPaymentMethod(),
                order.getStatus(),
                order.getTotalPrice(),
                itemResponses
        );
    }

    private OrdersResponse mapToOrdersResponse(Order order) {
        return new OrdersResponse(
                order.getId(),
                order.getUser().getId(),
                order.getMarket().getId(),
                order.getCreatedAt(),
                order.getMarket().getName(),
                order.getClientAddress(),
                order.getStatus(),
                order.getTotalPrice()
        );
    }

    private void pushOrderNotifications(User user, Market market, Order order) {
        Notification userNotification = new Notification();
        userNotification.setTitle("Order Created");
        userNotification.setUser(user);
        userNotification.setMessage("Your order #" + order.getId() + " has been placed. Please wait for the market's response.");

        Notification marketNotification = new Notification();
        marketNotification.setTitle("New Order Received");
        marketNotification.setUser(market.getUser());
        marketNotification.setMessage("You have received a new order in your market: " + market.getName());

        notificationService.pushNotification(userNotification);
        notificationService.pushNotification(marketNotification);
    }
}
