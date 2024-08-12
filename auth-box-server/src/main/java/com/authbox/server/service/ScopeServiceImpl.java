package com.authbox.server.service;

import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.AccessLog;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthScope;
import com.authbox.base.service.AccessLogService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.authbox.base.config.Constants.MSG_INVALID_SCOPE;
import static com.authbox.base.config.Constants.SPACE;
import static com.authbox.base.config.Constants.SPACE_SPLITTER;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;
import static org.springframework.util.ObjectUtils.isEmpty;

@AllArgsConstructor
@Slf4j
public class ScopeServiceImpl implements ScopeService {

    private final AccessLogService accessLogService;

    @Override
    public String getScopeStringBasedOnRequestedAndAllowed(final String scopeStr, final OauthClient oauthClient) {
        return oauthScopeListToSpaceDelimitedString(getScopeListBasedOnRequestedAndAllowed(scopeStr, oauthClient));
    }

    @Override
    public List<OauthScope> getScopeListBasedOnRequestedAndAllowed(final String scopeStr, final OauthClient oauthClient) {
        val requestedScopes = getScopes(scopeStr);
        if (requestedScopes.isPresent() && !validateScopes(requestedScopes.get(), oauthClient.getScopes())) {
            log.debug("Requested scope='{}' is not found in OauthClient scopes=[{}]",
                    String.join(SPACE, requestedScopes.get()), oauthScopeListToSpaceDelimitedString(oauthClient.getScopes()));
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.getOrganizationId())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_SCOPE),
                    "Requested scope='%s' is not found in Oauth2 client scopes=[%s]",
                    String.join(SPACE, requestedScopes.get()),
                    oauthScopeListToSpaceDelimitedString(oauthClient.getScopes())
            );
            throw new BadRequestException(MSG_INVALID_SCOPE);
        }
        return oauthClient.getScopes()
                .stream()
                .filter(oauthScope -> requestedScopes.isPresent())
                .filter(oauthScope -> requestedScopes.get().contains(oauthScope.getScope()))
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
        return clientScopes.stream().anyMatch(oauthScope -> oauthScope.getScope().equals(scope));
    }

    private String oauthScopeListToSpaceDelimitedString(final List<OauthScope> clientScopes) {
        return clientScopes.stream().map(OauthScope::getScope).collect(Collectors.joining(SPACE));
    }
}
