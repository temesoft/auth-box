package com.authbox.web.controller;

import com.authbox.base.dao.AccessLogRepository;
import com.authbox.base.model.AccessLog;
import com.authbox.web.Application;
import com.authbox.web.TestUtils;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static com.authbox.base.util.IdUtils.createId;
import static com.authbox.web.TestConstants.VALID_ORGANIZATION_ID;
import static com.authbox.web.config.Constants.API_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@TestPropertySource(locations = "classpath:application-test.properties")
public class AccessLogControllerTest {

    private static final String IP = "142.250.191.46";
    private static final String IP_DETAILS = """
            {"ip":"142.250.191.46","type":"ipv4","continent_code":"NA","continent_name":"North America","country_code":"US","country_name":"United States","region_code":"IL","region_name":"Illinois","city":"Chicago","zip":"60608","latitude":41.84885025024414,"longitude":-87.67124938964844,"msa":"16980","dma":"602","radius":"0","ip_routing_type":"fixed","connection_type":"tx","location":{"geoname_id":4887539,"capital":"Washington D.C.","languages":[{"code":"en","name":"English","native":"English"}],"country_flag":"https://assets.ipstack.com/flags/us.svg","country_flag_emoji":"🇺🇸","country_flag_emoji_unicode":"U+1F1FA U+1F1F8","calling_code":"1","is_eu":false}}
            """;

    @LocalServerPort
    private int port;
    @Autowired
    private RestTestClient restTestClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AccessLogController accessLogController;
    @Autowired
    private AccessLogRepository accessLogRepository;

    private String jSessionId;

    @BeforeEach
    public void setup() {
        jSessionId = TestUtils.authenticateAccountGetCookie(port);

        // setup cache for IpDetails
        accessLogController.getIpDetailsCache().put(IP, objectMapper.readTree(IP_DETAILS));
    }

    @Test
    public void testGetAccessLogByRequestId() {
        val requestId = createId();
        accessLogRepository.save(
                AccessLog.builder()
                        .withId(createId())
                        .withCreateTime(Instant.now())
                        .withMessage("Test message")
                        .withSource(AccessLog.Source.Oauth2Server)
                        .withRequestId(requestId)
                        .withOrganizationId(VALID_ORGANIZATION_ID)
                        .build()
        );

        val logByRequestId =
                objectMapper.readTree(
                        restTestClient.get().uri(API_PREFIX + "/access-log/" + requestId)
                                .header("cookie", jSessionId)
                                .exchange()
                                .returnResult()
                                .getResponseBodyContent()
                );
        assertThat(logByRequestId).isNotNull();
        assertThat(logByRequestId.get("page")).isNotNull();
        val pageDetails = logByRequestId.get("page");
        assertThat(pageDetails.get("totalElements")).isNotNull();
        assertThat(pageDetails.get("totalElements").intValue()).isEqualTo(1);
        assertThat(pageDetails.get("totalPages")).isNotNull();
        assertThat(pageDetails.get("totalPages").intValue()).isEqualTo(1);
        assertThat(logByRequestId.get("content")).isNotNull();
    }

    @Test
    public void testGetIpDetails_Success() {
        val ipDetailsResult = restTestClient.get().uri(API_PREFIX + "/access-log/ip/" + IP)
                .header("cookie", jSessionId)
                .exchange()
                .returnResult();
        val ipDetails = objectMapper.readTree(ipDetailsResult.getResponseBodyContent());
        assertThat(ipDetails).isNotNull();
        assertThat(ipDetails.get("country_code")).isNotNull();
        assertThat(ipDetails.get("country_code").stringValue()).isEqualTo("US");
        assertThat(ipDetails.get("latitude")).isNotNull();
        assertThat(ipDetails.get("latitude").doubleValue()).isNotZero();
        assertThat(ipDetails.get("longitude")).isNotNull();
        assertThat(ipDetails.get("longitude").doubleValue()).isNotZero();
        assertThat(ipDetails.get("location")).isNotNull();
        assertThat(ipDetails.get("location").get("country_flag_emoji")).isNotNull();
    }

    @Test
    public void testGetIpDetails_Empty() {
        val ipDetailsResult = restTestClient.get().uri(API_PREFIX + "/access-log/ip/1.2.3.4")
                .header("cookie", jSessionId)
                .exchange()
                .returnResult();
        val ipDetails = objectMapper.readTree(ipDetailsResult.getResponseBodyContent());
        assertThat(ipDetails).isNotNull();
        assertThat(ipDetails.toString()).isEqualTo("{}");
    }
}