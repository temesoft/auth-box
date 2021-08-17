package com.authbox.server.service;

import com.authbox.base.model.OauthClient;
import com.authbox.base.model.Organization;

import javax.annotation.Nullable;
import java.util.Map;

public interface TokenDetailsService {

    Map<String, Object> getAccessTokenDetails(Organization organization, String accessToken, @Nullable OauthClient providedClient);

}
