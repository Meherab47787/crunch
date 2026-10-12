package com.crustcrunch.config;

import com.crustcrunch.dto.MenuDtos.HoursEntry;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;
import java.util.List;

/** All tunables come from application.yml, which in turn reads environment variables. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Pricing pricing, Restaurant restaurant, Bootstrap bootstrap,
                            Uploads uploads) {

    public record Jwt(String secret, @DefaultValue("1440") long expirationMinutes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Pricing(@DefaultValue("0.08") BigDecimal taxRate,
                          @DefaultValue("2.99") BigDecimal deliveryFee,
                          @DefaultValue("35.00") BigDecimal freeDeliveryThreshold,
                          @DefaultValue("10.00") BigDecimal minDeliveryOrder,
                          @DefaultValue("USD") String currency) {
    }

    public record Restaurant(String name, String tagline, String phone, String email, String address,
                             @DefaultValue("UTC") String timezone,
                             @DefaultValue("35") int deliveryMinutes,
                             @DefaultValue("15") int pickupMinutes,
                             List<HoursEntry> hours) {
    }

    /** Creates the first admin (and optionally a kitchen account) on an empty database. */
    public record Bootstrap(String adminEmail, String adminPassword, @DefaultValue("Restaurant Admin") String adminName,
                            String kitchenEmail, String kitchenPassword,
                            @DefaultValue("false") boolean demoData) {
    }

    public record Uploads(@DefaultValue("./uploads") String dir, @DefaultValue("/uploads") String publicPath) {
    }
}
