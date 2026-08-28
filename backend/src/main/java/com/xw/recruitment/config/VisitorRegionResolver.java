package com.xw.recruitment.config;

import jakarta.servlet.http.HttpServletRequest;
import com.xw.recruitment.research.ResearchClientIpResolver;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class VisitorRegionResolver {
    private final ResearchClientIpResolver researchClientIpResolver;
    private static final Set<String> SOUTHEAST_ASIA = Set.of(
        "BN", "KH", "ID", "LA", "MY", "MM", "PH", "SG", "TH", "TL", "VN"
    );

    public VisitorRegionResolver(ResearchClientIpResolver researchClientIpResolver) {
        this.researchClientIpResolver = researchClientIpResolver;
    }

    public String country(HttpServletRequest request) {
        String country = firstPresent(request.getHeader("CF-IPCountry"), request.getHeader("X-Country-Code"));
        if (country != null) return country.trim().toUpperCase(Locale.ROOT);
        return isLocalAddress(request.getRemoteAddr()) ? "LOCAL" : "UNKNOWN";
    }

    public boolean usesSoutheastAsiaSalary(HttpServletRequest request) {
        String country = country(request);
        return "LOCAL".equals(country) || SOUTHEAST_ASIA.contains(country);
    }

    public String ipAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && forwardedFor.contains(",")) {
            forwardedFor = forwardedFor.substring(0, forwardedFor.indexOf(','));
        }
        String address = firstPresent(
            request.getHeader("CF-Connecting-IP"),
            forwardedFor,
            request.getHeader("X-Real-IP"),
            request.getRemoteAddr()
        );
        if (address == null) return "";
        address = address.trim();
        return address.length() > 64 ? address.substring(0, 64) : address;
    }

    public String researchIpAddress(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        String address = researchClientIpResolver.isTrustedProxy(request)
            ? firstPresent(request.getHeader("X-Real-IP"), remoteAddress)
            : remoteAddress;
        if (address == null) return "";
        address = address.trim();
        return address.length() > 64 ? address.substring(0, 64) : address;
    }

    public String researchCountry(HttpServletRequest request) {
        if (!researchClientIpResolver.isTrustedProxy(request)) return "UNKNOWN";
        String country = firstPresent(request.getHeader("X-Trusted-Country"));
        return country == null ? "UNKNOWN" : country.trim().toUpperCase(Locale.ROOT);
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
