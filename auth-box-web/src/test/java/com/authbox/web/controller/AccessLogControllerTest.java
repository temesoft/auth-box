package com.authbox.web.controller;

import com.authbox.base.dao.AccessLogDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AccessLogControllerTest {

    @Test
    public void testGetIpDetails() throws ExecutionException {
        val controller = new AccessLogController(
                mock(AccessLogDao.class),
                new RestTemplate(),
                new ObjectMapper(),
                "http://api.ipstack.com/{ip}?access_key=2fce8179cf1e7b4e7ea750a374485b7b",
                true);
        val json = controller.getIpDetails("142.250.191.46");
        assertThat(json).isNotNull();
        assertThat(json.get("country_code")).isNotNull();
        assertThat(json.get("country_code").textValue()).isEqualTo("US");
        assertThat(json.get("latitude")).isNotNull();
        assertThat(json.get("latitude").doubleValue()).isNotNull();
        assertThat(json.get("longitude")).isNotNull();
        assertThat(json.get("longitude").doubleValue()).isNotNull();
        assertThat(json.get("location")).isNotNull();
        assertThat(json.get("location").get("country_flag_emoji")).isNotNull();
    }
}