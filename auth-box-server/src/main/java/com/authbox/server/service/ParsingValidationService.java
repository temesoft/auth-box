package com.authbox.server.service;

import com.authbox.base.model.OauthClient;
import com.authbox.base.model.Organization;
import org.springframework.data.util.Pair;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface ParsingValidationService {

    OauthClient getOauthClient(final HttpServletRequest req, final Organization organization);

    Optional<Pair<String, String>> getCredentialsFromBasicAuthHeader(HttpServletRequest req);

    Optional<Pair<String, String>> getCredentialsFromParameters(HttpServletRequest req, String param1, String param2);

}
