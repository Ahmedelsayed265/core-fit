package com.corefit.service.market;

import com.corefit.dto.request.market.ChangeStatusRequest;
import com.corefit.dto.request.market.OrderRequest;
import com.corefit.dto.response.GeneralResponse;
import com.corefit.dto.response.market.OrderItemResponse;
import com.corefit.dto.response.market.OrderResponse;
import com.corefit.entity.*;
import com.corefit.entity.market.*;
import com.corefit.enums.OrderStatus;
import com.corefit.enums.PaymentMethod;
import com.corefit.enums.UserType;
import com.corefit.exceptions.GeneralException;
import com.corefit.repository.market.CartRepo;
import com.corefit.repository.market.OrderRepo;
import com.corefit.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    @Transactional
    public GeneralResponse<?> createOrder(OrderRequest orderRequest, HttpServletRequest httpRequest) {
        User user = authService.extractUserFromRequest(httpRequest);

        Cart cart = cartRepo.findByUserId(user.getId());

        if (cart == null || cart.getCartItems().isEmpty()) {
            throw new GeneralException("Cart is empty. Add items before placing an order.");
        }

        List<CartItem> cartItems = cart.getCartItems();
        List<OrderItem> orderItems = new ArrayList<>();

        Order order = new Order();

        order.setUser(user);
        order.setLongitude(orderRequest.getLongitude());
        order.setLatitude(orderRequest.getLatitude());
        order.setClientAddress(orderRequest.getClientAddress());
        order.setClientName(orderRequest.getClientName());
        order.setClientPhone(orderRequest.getClientPhone());
        order.setAdditionalInfo(orderRequest.getAdditionalInfo());
        order.setPaymentMethod(orderRequest.getPaymentMethod());
        order.setMarket(cart.getMarket());
        order.setTotalPrice(cart.getTotalPrice());

        Order finalOrder = order;
        cartItems.forEach(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setOrder(finalOrder);
            orderItem.setTotal(cartItem.getTotal());
            orderItems.add(orderItem);
        });
        order.setOrderItems(orderItems);

        if (orderRequest.getPaymentMethod() == PaymentMethod.WALLET) {
            try {
                walletService.withdraw(httpRequest, cart.getTotalPrice());
            } catch (Exception e) {
                throw new GeneralException("Wallet payment failed: " + e.getMessage());
            }
        }

        order = orderRepo.save(order);
        cartService.deleteCart(httpRequest);

        return new GeneralResponse<>("Order created successfully", mapToOrderResponse(order));
    }

    public GeneralResponse<?> getOrder(Long orderId, HttpServletRequest httpRequest) {
        User user = authService.extractUserFromRequest(httpRequest);
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new GeneralException("Order not found"));

        boolean isOrderOwner = Objects.equals(order.getUser().getId(), user.getId());
        boolean isProviderMarket = user.getMarket().stream().map(Market::getId).anyMatch(id -> Objects.equals(id, order.getMarket().getId()));

        if (!isOrderOwner && !isProviderMarket) {
            throw new GeneralException("User not authorized to access this order");
        }

        return new GeneralResponse<>("Success", mapToOrderResponse(order));
    }

    public GeneralResponse<?> getOrders(String status, Long marketId, HttpServletRequest httpRequest) {
        long userId = authService.extractUserIdFromRequest(httpRequest);
        User user = authService.findUserById(userId);

        List<Order> orders;

        if (user.getType() == UserType.GENERAL) {
            if ("previous".equalsIgnoreCase(status)) {
                orders = orderRepo.findPreviousOrdersByUserId(userId);
            } else {
                orders = orderRepo.findActiveOrdersByUserId(userId);
            }
        } else {
            if (marketId == null) {
                throw new GeneralException("Market ID is required for providers.");
            }
            if (status == null || status.isEmpty())
                status = "new";

            switch (status.toLowerCase()) {
                case "new":
                    orders = orderRepo.findNewMarketOrders(marketId);
                    break;
                case "current":
                    orders = orderRepo.findCurrentMarketOrders(marketId);
                    break;
                case "completed":
                    orders = orderRepo.findCompletedMarketOrders(marketId);
                    break;
                default:
                    throw new GeneralException("Invalid status. Use 'new', 'current', or 'completed'");
            }
        }


        List<OrderResponse> orderResponses = orders.stream().map(this::mapToOrderResponse).collect(Collectors.toList());
        return new GeneralResponse<>("Success", orderResponses);
    }

    @Transactional
    public GeneralResponse<?> cancelOrder(long orderId, HttpServletRequest httpRequest) {
        long userId = authService.extractUserIdFromRequest(httpRequest);
        User user = authService.findUserById(userId);

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new GeneralException("Order not found"));

        if (user.getType() == UserType.GENERAL) {
            if (!order.getUser().getId().equals(userId)) {
                throw new GeneralException("User not authorized to cancel this order");
            }

            if (order.getStatus() == OrderStatus.ORDER_DELIVERED || order.getStatus() == OrderStatus.ORDER_UNDER_DELIVERY) {
                throw new GeneralException("Order has already been delivered and cannot be canceled");
            } else if (order.getStatus() == OrderStatus.ORDER_CANCELED) {
                throw new GeneralException("Order is already canceled");
            }

            if (order.getPaymentMethod() == PaymentMethod.WALLET) {
                try {
                    walletService.deposit(order.getUser().getId(), order.getTotalPrice());
                } catch (Exception e) {
                    throw new GeneralException("Refund failed: " + e.getMessage());
                }
            }
            order.setStatus(OrderStatus.ORDER_CANCELED);
            orderRepo.save(order);

            return new GeneralResponse<>("Order canceled successfully");
        } else {
            throw new GeneralException("User not authorized to cancel this order");
        }
    }

    public GeneralResponse<?> changeStatus(ChangeStatusRequest request, HttpServletRequest httpRequest) {
        long userId = authService.extractUserIdFromRequest(httpRequest);
        User user = authService.findUserById(userId);

        Order order = orderRepo.findById(request.getOrderId())
                .orElseThrow(() -> new GeneralException("Order not found"));


        if (user.getType() != UserType.PROVIDER) {
            throw new GeneralException("User is not a provider");
        }

        boolean isProviderMarket = user.getMarket().stream().map(Market::getId).collect(Collectors.toSet()).contains(order.getMarket().getId());
        if (!isProviderMarket) {
            throw new GeneralException("Market does not belong to this provider");
        }

        OrderStatus newStatus = switch (request.getStatus().toLowerCase()) {
            case "cancelled" -> OrderStatus.ORDER_CANCELED;
            case "confirmed" -> OrderStatus.ORDER_CONFIRMED;
            case "under_prep" -> OrderStatus.ORDER_UNDER_PREPARATION;
            case "under_deliver" -> OrderStatus.ORDER_UNDER_DELIVERY;
            case "delivered" -> OrderStatus.ORDER_DELIVERED;
            default -> throw new GeneralException("Invalid status provided: " + request.getStatus());
        };


        if (order.getStatus() == newStatus) {
            throw new GeneralException("Order is already in status: " + newStatus);
        }

        if (newStatus.ordinal() <= order.getStatus().ordinal()) {
            throw new GeneralException("Cannot move from " + order.getStatus() + " to " + newStatus);
        }

        if (newStatus == OrderStatus.ORDER_CANCELED) {
            if (order.getStatus() == OrderStatus.ORDER_DELIVERED || order.getStatus() == OrderStatus.ORDER_UNDER_DELIVERY) {
                throw new GeneralException("Order has already been delivered and cannot be canceled");
            } else if (order.getStatus() == OrderStatus.ORDER_CANCELED) {
                throw new GeneralException("Order is already canceled");
            }

            if (order.getPaymentMethod() == PaymentMethod.WALLET) {
                try {
                    walletService.deposit(order.getUser().getId(), order.getTotalPrice());
                } catch (Exception e) {
                    throw new GeneralException("Refund failed: " + e.getMessage());
                }
            }
        }

        order.setStatus(newStatus);
        orderRepo.save(order);

        return new GeneralResponse<>("Order status updated successfully to: " + order.getStatus(), mapToOrderResponse(order));
    }

    /// Helper method
    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> orderItems = order.getOrderItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getDescription(),
                        item.getProduct().getPrice(),
                        item.getProduct().getOffer(),
                        item.getProduct().getSubCategory().getName(),
                        item.getProduct().getImages(),
                        item.getQuantity(),
                        item.getTotal())).collect(Collectors.toList());

        return new OrderResponse(order.getId(), order.getUser().getId(), order.getClientName(), order.getClientAddress(), order.getClientPhone()
                , order.getLatitude(), order.getLongitude(), order.getAdditionalInfo(), order.getStatus(), order.getPaymentMethod(),
                order.getTotalPrice(), order.getMarket(), orderItems);
    }
}

