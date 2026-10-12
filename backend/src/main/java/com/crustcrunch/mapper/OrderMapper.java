package com.crustcrunch.mapper;

import com.crustcrunch.dto.OrderDtos.*;
import com.crustcrunch.entity.Order;
import com.crustcrunch.entity.OrderItem;
import com.crustcrunch.enums.FulfillmentType;
import com.crustcrunch.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    /**
     * @param includeTrackingToken only true for the customer who placed the order (or staff)
     */
    public OrderResponse toResponse(Order o, boolean includeTrackingToken, List<Long> reviewedProductIds) {
        DeliveryAddress address = o.getFulfillmentType() == FulfillmentType.DELIVERY
                ? new DeliveryAddress(o.getAddressLine1(), o.getAddressLine2(), o.getAddressCity(), o.getAddressPostalCode())
                : null;
        return new OrderResponse(o.getId(), o.getOrderNumber(), includeTrackingToken ? o.getTrackingToken() : null,
                o.getStatus(), o.getStatus().next(o.getFulfillmentType()), o.getStatus().previous(),
                o.getFulfillmentType(), o.getPaymentMethod(), o.getPaymentStatus(),
                new ContactInfo(o.getContactName(), o.getContactEmail(), o.getContactPhone()), address,
                o.getDeliveryInstructions(), o.getSpecialInstructions(), o.getAllergyNotes(),
                o.getItems().stream().map(this::toResponse).toList(), o.getSubtotal(), o.getDiscountTotal(),
                o.getDeliveryFee(), o.getTax(), o.getTotal(), o.getCouponCode(), o.getEstimatedReadyAt(),
                o.getCancelReason(), o.getCreatedAt(), o.getUpdatedAt(),
                o.getStatusHistory().stream().map(h -> new StatusHistoryResponse(h.getFromStatus(), h.getToStatus(),
                        h.getChangedBy() == null ? null : h.getChangedBy().getFullName(), h.getNote(),
                        h.getCreatedAt())).toList(),
                o.getStatus() == OrderStatus.PENDING,
                o.getUser() == null ? null : o.getUser().getId(),
                reviewedProductIds == null ? List.of() : reviewedProductIds);
    }

    public OrderItemResponse toResponse(OrderItem i) {
        return new OrderItemResponse(i.getId(), i.getProductId(), i.getProductName(), i.getCategoryName(),
                i.getVariantName(), i.getBasePrice(), i.getVariantPriceDelta(), i.getUnitPrice(), i.getQuantity(),
                i.getDiscount(), i.getLineTotal(), i.getNotes(),
                i.getCustomizations().stream().map(c -> new CustomizationResponse(c.getKind(), c.getGroupName(),
                        c.getOptionName(), c.getPriceDelta())).toList());
    }

    public OrderSummaryResponse toSummary(Order o) {
        int count = o.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        String preview = o.getItems().stream()
                .map(i -> i.getQuantity() + "× " + i.getProductName())
                .limit(3)
                .collect(Collectors.joining(", "));
        if (o.getItems().size() > 3) {
            preview += " +" + (o.getItems().size() - 3) + " more";
        }
        return new OrderSummaryResponse(o.getId(), o.getOrderNumber(), o.getStatus(), o.getFulfillmentType(),
                o.getPaymentMethod(), o.getPaymentStatus(), o.getTotal(), count, preview, o.getContactName(),
                o.getCreatedAt());
    }
}
