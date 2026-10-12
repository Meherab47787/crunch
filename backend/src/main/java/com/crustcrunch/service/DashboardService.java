package com.crustcrunch.service;

import com.crustcrunch.config.AppProperties;
import com.crustcrunch.dto.DashboardDtos.*;
import com.crustcrunch.enums.OrderStatus;
import com.crustcrunch.enums.Role;
import com.crustcrunch.mapper.OrderMapper;
import com.crustcrunch.repository.OrderRepository;
import com.crustcrunch.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final OrderRepository orders;
    private final UserRepository users;
    private final OrderMapper mapper;
    private final ZoneId zone;
    private final Clock clock;

    public DashboardService(OrderRepository orders, UserRepository users, OrderMapper mapper, AppProperties props,
                            Clock clock) {
        this.orders = orders;
        this.users = users;
        this.mapper = mapper;
        this.zone = ZoneId.of(props.restaurant().timezone());
        this.clock = clock;
    }

    public DashboardStats stats() {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Instant todayStart = today.atStartOfDay(zone).toInstant();
        Instant weekStart = today.minusDays(6).atStartOfDay(zone).toInstant();
        Instant monthStart = today.minusDays(29).atStartOfDay(zone).toInstant();

        long total = orders.count() - orders.countByStatus(OrderStatus.CANCELLED);
        BigDecimal revenue = orders.totalRevenue();
        BigDecimal aov = total == 0 ? BigDecimal.ZERO.setScale(2) : revenue.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            statusCounts.put(s.name(), orders.countByStatus(s));
        }

        // Daily: last 14 days, weekly: last 12 ISO weeks, monthly: last 12 months — gaps filled with zeroes
        LocalDate dailyFrom = today.minusDays(13);
        List<SalesPoint> daily = fill(orders.salesBuckets("day", zone.getId(), dailyFrom.atStartOfDay(zone).toInstant()),
                dailyFrom, 14, d -> d.plusDays(1));
        LocalDate weeklyFrom = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(11);
        List<SalesPoint> weekly = fill(orders.salesBuckets("week", zone.getId(), weeklyFrom.atStartOfDay(zone).toInstant()),
                weeklyFrom, 12, d -> d.plusWeeks(1));
        LocalDate monthlyFrom = today.withDayOfMonth(1).minusMonths(11);
        List<SalesPoint> monthly = fill(orders.salesBuckets("month", zone.getId(), monthlyFrom.atStartOfDay(zone).toInstant()),
                monthlyFrom, 12, d -> d.plusMonths(1));

        List<TopProduct> top = orders.topProducts(monthStart, 8).stream()
                .map(r -> new TopProduct(r[0] == null ? null : ((Number) r[0]).longValue(), (String) r[1],
                        ((Number) r[2]).longValue(), money(r[3]))).toList();
        List<CategorySales> cats = orders.categorySales(monthStart).stream()
                .map(r -> new CategorySales((String) r[0], ((Number) r[1]).longValue(), money(r[2]))).toList();
        var recent = orders.findAll(PageRequest.of(0, 8, Sort.by(Sort.Order.desc("createdAt"))))
                .map(mapper::toSummary).getContent();

        return new DashboardStats(total, revenue, aov,
                orders.countSince(todayStart), orders.revenueSince(todayStart),
                orders.countSince(weekStart), orders.revenueSince(weekStart),
                orders.countSince(monthStart), orders.revenueSince(monthStart),
                orders.countByStatusIn(OrderStatus.ACTIVE), orders.countByStatus(OrderStatus.DELIVERED),
                orders.countByStatus(OrderStatus.CANCELLED), users.countByRole(Role.CUSTOMER),
                statusCounts, daily, weekly, monthly, top, cats, recent);
    }

    private static List<SalesPoint> fill(List<Object[]> rows, LocalDate from, int n,
                                         java.util.function.UnaryOperator<LocalDate> step) {
        Map<String, Object[]> byKey = new HashMap<>();
        rows.forEach(r -> byKey.put((String) r[0], r));
        List<SalesPoint> out = new ArrayList<>(n);
        LocalDate d = from;
        for (int i = 0; i < n; i++) {
            Object[] r = byKey.get(d.toString());
            out.add(r == null ? new SalesPoint(d.toString(), 0, BigDecimal.ZERO.setScale(2))
                    : new SalesPoint(d.toString(), ((Number) r[1]).longValue(), money(r[2])));
            d = step.apply(d);
        }
        return out;
    }

    private static BigDecimal money(Object o) {
        return o == null ? BigDecimal.ZERO.setScale(2) : new BigDecimal(o.toString()).setScale(2, RoundingMode.HALF_UP);
    }
}
