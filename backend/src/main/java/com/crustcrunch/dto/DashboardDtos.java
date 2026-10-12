package com.crustcrunch.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record SalesPoint(String label, long orders, BigDecimal revenue) {
    }

    public record TopProduct(Long productId, String name, long quantity, BigDecimal revenue) {
    }

    public record CategorySales(String name, long quantity, BigDecimal revenue) {
    }

    public record DashboardStats(
            long totalOrders, BigDecimal totalRevenue, BigDecimal averageOrderValue,
            long todayOrders, BigDecimal todayRevenue,
            long weekOrders, BigDecimal weekRevenue,
            long monthOrders, BigDecimal monthRevenue,
            long activeOrders, long completedOrders, long cancelledOrders, long customers,
            Map<String, Long> statusCounts,
            List<SalesPoint> daily, List<SalesPoint> weekly, List<SalesPoint> monthly,
            List<TopProduct> topProducts, List<CategorySales> categorySales,
            List<OrderDtos.OrderSummaryResponse> recentOrders) {
    }
}
