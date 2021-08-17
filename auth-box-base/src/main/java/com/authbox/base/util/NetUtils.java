package com.authbox.base.util;

import javax.servlet.http.HttpServletRequest;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class NetUtils {

    public static String getIp(final HttpServletRequest req) {
        final String ip = req.getHeader("x-forwarded-for");
        if (isNotBlank(ip)) {
            return ip;
        }
        return req.getRemoteHost();
    }

    public static String getUserAgent(final HttpServletRequest req) {
        return req.getHeader("User-Agent");
    }

}
