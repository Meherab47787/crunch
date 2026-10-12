package com.crustcrunch.dto;

import com.crustcrunch.enums.CustomizationKind;
import com.crustcrunch.enums.FulfillmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cart lines only carry IDs and quantities. All prices are computed server-side;
 * any price a client might send is never read.
 */
public final class CartDtos {

    private CartDtos() {
    }

    public record CartLineRequest(
            @NotNull Long productId,
            Long variantId,
            @Size(max = 30) List<@NotNull Long> optionIds,
            @Size(max = 20) List<@Valid @NotNull ComboSelectionRequest> comboSelections,
            @Min(1) @Max(50) int quantity,
            @Size(max = 300) String notes) {

        public List<Long> optionIdsOrEmpty() {
            return optionIds == null ? List.of() : optionIds;
        }

        public List<ComboSelectionRequest> comboSelectionsOrEmpty() {
            return comboSelections == null ? List.of() : comboSelections;
        }
    }

    public record ComboSelectionRequest(@NotNull Long componentId, @NotNull Long productId) {
    }

    public record QuoteRequest(
            @NotEmpty @Size(max = 50) List<@Valid @NotNull CartLineRequest> items,
            @Size(max = 40) String couponCode,
            FulfillmentType fulfillmentType) {
    }

    public record SelectionLine(CustomizationKind kind, String groupName, String optionName, BigDecimal priceDelta) {
    }

    public record QuoteLine(int index, Long productId, String productName, String productSlug, String imageUrl,
                            String categoryName, String variantName, List<SelectionLine> selections,
                            BigDecimal unitPrice, int quantity, BigDecimal lineSubtotal, BigDecimal promotionDiscount,
                            String promotionName, BigDecimal lineTotal, boolean valid, String error) {
    }

    public record QuoteResponse(List<QuoteLine> lines, BigDecimal subtotal, BigDecimal promotionDiscount,
                                BigDecimal couponDiscount, BigDecimal discountTotal, BigDecimal deliveryFee,
                                BigDecimal tax, BigDecimal total, String couponCode, boolean couponApplied,
                                String couponMessage, BigDecimal freeDeliveryThreshold, List<String> issues,
                                boolean valid) {
    }
}
