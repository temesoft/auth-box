package com.authbox.base.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NetUtilsTest {

    @Test
    void testGetIpWithXForwardedFor() {
        val request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        val result = NetUtils.getIp(request);
        assertThat(result).isEqualTo("192.168.1.1");
    }

    @Test
    void testGetIpWithRemoteHost() {
        val request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteHost()).thenReturn("127.0.0.1");
        val result = NetUtils.getIp(request);
        assertThat(result).isEqualTo("127.0.0.1");
    }

    @Test
    void testGetIpWithBlankXForwardedFor() {
        val request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(" ");
        when(request.getRemoteHost()).thenReturn("127.0.0.1");
        val result = NetUtils.getIp(request);
        assertThat(result).isEqualTo("127.0.0.1");
    }

    @Test
    void testGetUserAgent() {
        val request = mock(HttpServletRequest.class);
        val agent = "Mozilla/5.0";
        when(request.getHeader("User-Agent")).thenReturn(agent);
        val result = NetUtils.getUserAgent(request);
        assertThat(result).isEqualTo(agent);
    }
}