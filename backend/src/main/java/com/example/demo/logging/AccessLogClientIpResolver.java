package com.example.demo.logging;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * Resolves client IP values with an explicit trusted-proxy allowlist.
 * 
 * <pre>
 * Responsibilities:
 * 1) Parse and normalize trusted proxy addresses from configuration.
 * 2) Resolve X-Forwarded-For only when remote address is explicitly trusted.
 * 3) Fall back to request remote address when trust checks do not pass.
 * </pre>
 */
final class AccessLogClientIpResolver {
    /** Default trusted proxies used when configuration is absent. */
    private static final String DEF_TRUSTED = "127.0.0.1,::1,0:0:0:0:0:0:0:1";

    /** Forwarded header key used for client IP resolution. */
    private static final String HDR_XFF = "X-Forwarded-For";

    /**
     * Utility class.
     * 
     * <pre>
     * Design note:
     * 1) Exposes static resolver helpers only.
     * 2) Constructor is private to prevent accidental instantiation.
     * </pre>
     */
    private AccessLogClientIpResolver() {}

    /**
     * Returns the default trusted-proxy address set.
     * 
     * <pre>
     * Algorithm:
     * 1) Parse the built-in default proxy list.
     * 2) Return a normalized immutable set.
     * </pre>
     *
     * @return default trusted-proxy addresses
     */
    /* default */ static Set<String> defaultTrustedProxyAddresses() {
        return parseTrustedProxyAddresses(DEF_TRUSTED);
    }

    /**
     * Parses configured trusted proxies.
     * 
     * <pre>
     * Algorithm:
     * 1) Split the comma-separated value.
     * 2) Trim entries and discard blank tokens.
     * 3) Normalize entries to lowercase canonical form where possible.
     * </pre>
     *
     * @param configured comma-separated proxy addresses
     * @return normalized trusted-proxy set
     */
    /* default */ static Set<String> parseTrustedProxyAddresses(final String configured) {
        final String raw = configured == null ? "" : configured;
        return Arrays.stream(raw.split(",")).map(String::trim).filter(StringUtils::hasText)
                .map(AccessLogClientIpResolver::normalizeAddress)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Resolves the effective client IP.
     * 
     * <pre>
     * Algorithm:
     * 1) Read remoteAddr as the default client IP.
     * 2) Read X-Forwarded-For and parse its first hop when present.
     * 3) Use forwarded IP only when remoteAddr is in trusted proxy allowlist.
     * 4) Otherwise return remoteAddr unchanged.
     * </pre>
     *
     * @param request current request
     * @param trustedProxies trusted proxy allowlist
     * @return client IP
     */
    /* default */ static String resolveClientIp(final HttpServletRequest request,
            final Set<String> trustedProxies) {
        String clientIp = request.getRemoteAddr();
        final String forwarded = request.getHeader(HDR_XFF);
        final String remoteAddr = request.getRemoteAddr();
        if (StringUtils.hasText(forwarded) && isTrustedProxy(remoteAddr, trustedProxies)) {
            final int commaIndex = forwarded.indexOf(',');
            clientIp =
                    commaIndex > 0 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
        }
        return clientIp;
    }

    /**
     * Checks whether a remote address belongs to the trusted proxy allowlist.
     * 
     * <pre>
     * Algorithm:
     * 1) Reject blank remote addresses.
     * 2) Normalize both literal and canonical address forms.
     * 3) Match either normalized representation against trusted proxy allowlist.
     * </pre>
     *
     * @param remoteAddr remote address from request
     * @param trustedProxies trusted proxy allowlist
     * @return true when the proxy is trusted
     */
    /* default */ static boolean isTrustedProxy(final String remoteAddr,
            final Set<String> trustedProxies) {
        boolean trusted = false;
        if (StringUtils.hasText(remoteAddr) && trustedProxies != null
                && !trustedProxies.isEmpty()) {
            final String normalizedRemote = normalizeAddress(remoteAddr);
            if (trustedProxies.contains(normalizedRemote)) {
                trusted = true;
            } else {
                try {
                    final InetAddress address = InetAddress.getByName(remoteAddr);
                    trusted = trustedProxies.contains(normalizeAddress(address.getHostAddress()));
                } catch (final UnknownHostException ex) {
                    trusted = false;
                }
            }
        }
        return trusted;
    }

    /**
     * Normalizes addresses for case-insensitive matching.
     * 
     * <pre>
     * Algorithm:
     * 1) Trim input value.
     * 2) Convert to lowercase using Locale.ROOT.
     * 3) Return normalized string.
     * </pre>
     *
     * @param address raw address
     * @return normalized address
     */
    private static String normalizeAddress(final String address) {
        return address.trim().toLowerCase(Locale.ROOT);
    }
}
