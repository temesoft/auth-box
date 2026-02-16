package com.authbox.base.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import jakarta.servlet.http.HttpServletRequest;
import lombok.val;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Provides various client request networking related functionalities
 */
public class NetUtils {

    private NetUtils() {
    }

    /**
     * Returns IP address of the client,
     * when using proxy to receive request and X-Forwarded-For is available returns correct IP address
     */
    public static String getIp(final HttpServletRequest req) {
        val ip = req.getHeader("X-Forwarded-For");
        if (isNotBlank(ip)) {
            return Iterables.get(Splitter.on(',').split(ip), 0);
        }
        return req.getRemoteHost();
    }

    /**
     * Returns user agent header to identify client making a request
     */
    public static String getUserAgent(final HttpServletRequest req) {
        return req.getHeader("User-Agent");
    }
}
