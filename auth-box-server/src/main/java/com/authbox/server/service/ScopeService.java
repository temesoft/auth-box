package com.authbox.server.service;

import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthScope;

import java.util.List;

public interface ScopeService {

    String getScopeStringBasedOnRequestedAndAllowed(String scopeStr, OauthClient oauthClient);

    List<OauthScope> getScopeListBasedOnRequestedAndAllowed(String scopeStr, OauthClient oauthClient);

}
