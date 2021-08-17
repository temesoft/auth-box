package com.authbox.server.service;

import com.authbox.base.dao.OauthScopeDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthScope;
import com.authbox.base.service.AccessLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.authbox.base.config.Constants.MSG_INVALID_SCOPE;
import static com.authbox.base.config.Constants.SPACE;
import static com.authbox.base.config.Constants.SPACE_SPLITTER;
import static com.authbox.base.model.AccessLog.AccessLogBuilder.accessLogBuilder;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;
import static org.springframework.util.ObjectUtils.isEmpty;

public class ScopeServiceImpl implements ScopeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScopeServiceImpl.class);

    private final OauthScopeDao oauthScopeDao;
    private final AccessLogService accessLogService;

    public ScopeServiceImpl(final OauthScopeDao oauthScopeDao, final AccessLogService accessLogService) {
        this.oauthScopeDao = oauthScopeDao;
        this.accessLogService = accessLogService;
    }

    @Override
    public String getScopeStringBasedOnRequestedAndAllowed(final String scopeStr, final OauthClient oauthClient) {
        return oauthScopeListToSpaceDelimitedString(getScopeListBasedOnRequestedAndAllowed(scopeStr, oauthClient));
    }

    @Override
    public List<OauthScope> getScopeListBasedOnRequestedAndAllowed(final String scopeStr, final OauthClient oauthClient) {
        final Optional<List<String>> requestedScopes = getScopes(scopeStr);
        final List<OauthScope> oauthClientScopes = oauthScopeDao.listByClientId(oauthClient.id);
        if (requestedScopes.isPresent() && !validateScopes(requestedScopes.get(), oauthClientScopes)) {
            LOGGER.debug("Requested scope='{}' is not found in OauthClient scopes=[{}]", String.join(SPACE, requestedScopes.get()), oauthScopeListToSpaceDelimitedString(oauthClientScopes));
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.organizationId)
                            .withClientId(oauthClient.id)
                            .withError(MSG_INVALID_SCOPE),
                    "Requested scope='%s' is not found in Oauth2 client scopes=[%s]",
                    String.join(SPACE, requestedScopes.get()),
                    oauthScopeListToSpaceDelimitedString(oauthClientScopes)
            );
            throw new BadRequestException(MSG_INVALID_SCOPE);
        }
        return oauthClientScopes
                .stream()
                .filter(oauthScope -> requestedScopes.isPresent())
                .filter(oauthScope -> requestedScopes.get().contains(oauthScope.scope))
                .sorted()
                .collect(Collectors.toList());
    }

    private Optional<List<String>> getScopes(final String scopeStr) {
        if (isEmpty(scopeStr)) {
            return Optional.empty();
        }
        return Optional.of(SPACE_SPLITTER.splitToList(scopeStr));
    }

    private boolean validateScopes(final List<String> requestedScopes, final List<OauthScope> clientScopes) {
        if (requestedScopes.isEmpty()) {
            return true;
        }
        return requestedScopes.stream()
                .filter(scope -> !containsScope(scope, clientScopes))
                .findAny()
                .isEmpty();
    }

    private boolean containsScope(final String scope, final List<OauthScope> clientScopes) {
        return clientScopes.stream().anyMatch(oauthScope -> oauthScope.scope.equals(scope));
    }

    private String oauthScopeListToSpaceDelimitedString(final List<OauthScope> clientScopes) {
        return clientScopes.stream().map(oauthScope -> oauthScope.scope).collect(Collectors.joining(SPACE));
    }
}
