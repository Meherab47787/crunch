package com.crustcrunch.dto;

import com.crustcrunch.dto.CartDtos.CartLineRequest;
import com.crustcrunch.enums.*;
import com.crustcrunch.validation.DeliveryAddressRequired;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record ContactInfo(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Pattern(regexp = "^[+0-9 ()-]{7,30}$", message = "is not a valid phone number") String phone) {
    }

    public record DeliveryAddress(
            @NotBlank @Size(max = 200) String line1,
            @Size(max = 200) String line2,
            @NotBlank @Size(max = 100) String city,
            @Size(max = 20) String postalCode) {
    }

    @DeliveryAddressRequired
    public record CheckoutRequest(
            @NotEmpty @Size(max = 50) List<@Valid @NotNull CartLineRequest> items,
            @Size(max = 40) String couponCode,
            @NotNull FulfillmentType fulfillmentType,
            @NotNull @Valid ContactInfo contact,
            @Valid DeliveryAddress address,
            @Size(max = 500) String deliveryInstructions,
            @Size(max = 500) String specialInstructions,
            @Size(max = 500) String allergyNotes,
            @NotNull PaymentMethod paymentMethod) {
    }

    public record CustomizationResponse(CustomizationKind kind, String groupName, String optionName,
                                        BigDecimal priceDelta) {
    }

    public record OrderItemResponse(Long id, Long productId, String productName, String categoryName,
                                    String variantName, BigDecimal basePrice, BigDecimal variantPriceDelta,
                                    BigDecimal unitPrice, int quantity, BigDecimal discount, BigDecimal lineTotal,
                                    String notes, List<CustomizationResponse> customizations) {
    }

    public record StatusHistoryResponse(OrderStatus fromStatus, OrderStatus toStatus, String changedBy, String note,
                                        Instant createdAt) {
    }

    public record OrderResponse(Long id, String orderNumber, String trackingToken, OrderStatus status,
                                OrderStatus nextStatus, OrderStatus previousStatus, FulfillmentType fulfillmentType,
                                PaymentMethod paymentMethod,
                                PaymentStatus paymentStatus, ContactInfo contact, DeliveryAddress address,
                                String deliveryInstructions, String specialInstructions, String allergyNotes,
                                List<OrderItemResponse> items, BigDecimal subtotal, BigDecimal discountTotal,
                                BigDecimal deliveryFee, BigDecimal tax, BigDecimal total, String couponCode,
                                Instant estimatedReadyAt, String cancelReason, Instant createdAt, Instant updatedAt,
                                List<StatusHistoryResponse> history, boolean cancellableByCustomer,
                                Long customerId, List<Long> reviewedProductIds) {
    }

    public record OrderSummaryResponse(Long id, String orderNumber, OrderStatus status,
                                       FulfillmentType fulfillmentType, PaymentMethod paymentMethod,
                                       PaymentStatus paymentStatus, BigDecimal total, int itemCount,
                                       String itemsPreview, String contactName, Instant createdAt) {
    }

    public record StatusUpdateRequest(@NotNull OrderStatus status, @Size(max = 300) String note) {
    }

    public record CancelRequest(@Size(max = 300) String reason) {
    }

    /** A cart line rebuilt from a past order, with current display info and price. */
    public record ReorderLine(CartLineRequest request, String name, String slug, String imageUrl, String categoryName,
                              List<String> summary, BigDecimal unitPrice) {
    }

    public record ReorderResponse(List<ReorderLine> items, List<String> skipped) {
    }

    public record CreateReviewRequest(
            @NotNull Long orderId,
            @NotNull Long productId,
            @Min(1) @Max(5) int rating,
            @Size(max = 1000) String comment) {
    }
}
