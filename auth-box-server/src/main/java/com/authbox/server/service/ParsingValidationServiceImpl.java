package com.authbox.server.service;

import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.AccessLog;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.Organization;
import com.authbox.base.service.AccessLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.util.Pair;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

import static com.authbox.base.config.Constants.COLON;
import static com.authbox.base.config.Constants.COLON_SPLITTER;
import static com.authbox.base.config.Constants.HEADER_AUTHORIZATION;
import static com.authbox.base.config.Constants.HEADER_AUTHORIZATION_PREFIX_BASIC;
import static com.authbox.base.config.Constants.MSG_INVALID_REQUEST;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_CLIENT_ID;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_CLIENT_SECRET;
import static com.authbox.server.config.RequestWrapperFilterConfiguration.REQUEST_ID_MDC_KEY;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;
import static com.google.common.io.BaseEncoding.base64;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ParsingValidationServiceImpl implements ParsingValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParsingValidationServiceImpl.class);

    private final OauthClientDao oauthClientDao;
    private final AccessLogService accessLogService;

    public ParsingValidationServiceImpl(final OauthClientDao oauthClientDao, final AccessLogService accessLogService) {
        this.oauthClientDao = oauthClientDao;
        this.accessLogService = accessLogService;
    }

    @Override
    public OauthClient getOauthClient(final HttpServletRequest req, final Organization organization) {
        Optional<Pair<String, String>> requestClientDetails = getCredentialsFromBasicAuthHeader(req);
        if (requestClientDetails.isEmpty()) {
            requestClientDetails = getCredentialsFromParameters(req, OAUTH2_ATTR_CLIENT_ID, OAUTH2_ATTR_CLIENT_SECRET);
        }
        if (requestClientDetails.isEmpty()) {
            LOGGER.debug("Request missing client credentials");
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Request missing client credentials"
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        final Optional<OauthClient> oauthClient = oauthClientDao.getById(requestClientDetails.get().getFirst());
        if (oauthClient.isEmpty()) {
            LOGGER.debug("OauthClient not found by id='{}'", requestClientDetails.get().getFirst());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client not found by id='%s'",
                    requestClientDetails.get().getFirst()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        if (!oauthClient.get().isEnabled()) {
            LOGGER.debug("OauthClient is disabled");
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(MDC.get(REQUEST_ID_MDC_KEY))
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client is disabled"
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        if (!oauthClient.get().getOrganizationId().equals(organization.getId())) {
            LOGGER.debug("Oauth2 client organization details do not match domain prefix specified in request: '{}'", organization.getDomainPrefix());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client organization details do not match domain prefix specified in request: '%s'",
                    organization.getDomainPrefix()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        if (!oauthClient.get().getSecret().equals(requestClientDetails.get().getSecond())) {
            LOGGER.debug("OauthClient secret does not match");
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withError(MSG_INVALID_REQUEST)
                            .withClientId(oauthClient.get().getId()),
                    "Oauth2 client secret does not match provided value"
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        return oauthClient.get();
    }

    @Override
    public Optional<Pair<String, String>> getCredentialsFromBasicAuthHeader(final HttpServletRequest req) {
        final String authHeader = req.getHeader(HEADER_AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(HEADER_AUTHORIZATION_PREFIX_BASIC)) {
            final String base64EncodedPair = authHeader.replace(HEADER_AUTHORIZATION_PREFIX_BASIC, "");
            if (!base64().canDecode(base64EncodedPair)) {
                return Optional.empty();
            }
            final String decodedPair = new String(base64().decode(base64EncodedPair), UTF_8);
            if (!decodedPair.contains(COLON)) {
                return Optional.empty();
            }
            final List<String> listOfValues = COLON_SPLITTER.splitToList(decodedPair);
            return Optional.of(Pair.of(listOfValues.get(0), listOfValues.get(1)));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Pair<String, String>> getCredentialsFromParameters(final HttpServletRequest req, final String param1, final String param2) {
        final String first = req.getParameter(param1);
        final String second = req.getParameter(param2);
        if (first != null && second != null) {
            return Optional.of(Pair.of(first, second));
        }
        return Optional.empty();
    }
}
