package com.xw.recruitment.research;

import jakarta.servlet.http.HttpServletRequest;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResearchClientIpResolver {
    private static final int MAX_FORWARDED_HEADER_LENGTH = 2_048;
    private static final int MAX_FORWARDED_HOPS = 32;
    private static final int MAX_TRUSTED_PROXIES = 32;
    private static final String UNKNOWN_CLIENT = "unknown";

    private final Set<String> trustedProxies;

    public ResearchClientIpResolver(
            @Value("${xw.research.trusted-proxies:}") String configuredTrustedProxies) {
        LinkedHashSet<String> parsed = new LinkedHashSet<>();
        for (String configured : configuredTrustedProxies.split(",")) {
            if (configured.isBlank()) continue;
            String address = normalizeLiteral(configured);
            if (address == null) {
                throw new IllegalStateException(
                    "Research trusted proxies must contain only literal IP addresses");
            }
            parsed.add(address);
        }
        if (parsed.size() > MAX_TRUSTED_PROXIES) {
            throw new IllegalStateException("Research trusted proxy chain is too large");
        }
        trustedProxies = Set.copyOf(parsed);
    }

    public String clientIp(HttpServletRequest request) {
        String directAddress = normalizeLiteral(request.getRemoteAddr());
        if (directAddress == null) return UNKNOWN_CLIENT;
        if (!trustedProxies.contains(directAddress)) return directAddress;

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) return directAddress;
        if (forwardedFor.length() > MAX_FORWARDED_HEADER_LENGTH) return directAddress;
        String[] forwardedHops = forwardedFor.split(",", -1);
        if (forwardedHops.length > MAX_FORWARDED_HOPS) return directAddress;

        String current = directAddress;
        for (int index = forwardedHops.length - 1; index >= 0; index--) {
            if (!trustedProxies.contains(current)) return current;
            String forwardedAddress = normalizeLiteral(forwardedHops[index]);
            if (forwardedAddress == null) return directAddress;
            current = forwardedAddress;
        }
        return current;
    }

    private String normalizeLiteral(String value) {
        if (value == null) return null;
        String candidate = value.trim();
        if (candidate.startsWith("[") && candidate.endsWith("]")) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }
        if (candidate.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) {
            return normalizeIpv4(candidate);
        }
        if (!candidate.contains(":") || !candidate.matches("[0-9A-Fa-f:.]+")) {
            return null;
        }
        try {
            InetAddress parsed = InetAddress.getByName(candidate);
            return parsed instanceof Inet6Address ? parsed.getHostAddress() : null;
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private String normalizeIpv4(String value) {
        int[] octets = Arrays.stream(value.split("\\."))
            .mapToInt(Integer::parseInt)
            .toArray();
        if (Arrays.stream(octets).anyMatch(octet -> octet > 255)) return null;
        return octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3];
    }
}
