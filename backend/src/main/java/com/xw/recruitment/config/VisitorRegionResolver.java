package com.xw.recruitment.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class VisitorRegionResolver {
    private static final Set<String> SOUTHEAST_ASIA = Set.of(
        "BN", "KH", "ID", "LA", "MY", "MM", "PH", "SG", "TH", "TL", "VN"
    );

    public String country(HttpServletRequest request) {
        String country = firstPresent(request.getHeader("CF-IPCountry"), request.getHeader("X-Country-Code"));
        if (country != null) return country.trim().toUpperCase(Locale.ROOT);
        return isLocalAddress(request.getRemoteAddr()) ? "LOCAL" : "UNKNOWN";
    }

    public boolean usesSoutheastAsiaSalary(HttpServletRequest request) {
        String country = country(request);
        return "LOCAL".equals(country) || SOUTHEAST_ASIA.contains(country);
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private boolean isLocalAddress(String address) {
        if (address == null) return false;
        return address.equals("127.0.0.1") || address.equals("0:0:0:0:0:0:0:1") || address.equals("::1")
            || address.startsWith("10.") || address.startsWith("192.168.")
            || address.matches("172\\.(1[6-9]|2[0-9]|3[01])\\..*");
    }
}
