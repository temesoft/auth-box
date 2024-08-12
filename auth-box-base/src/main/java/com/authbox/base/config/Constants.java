package com.authbox.base.config;

import com.google.common.base.Splitter;

public class Constants {

    public static final String COMMA = ",";
    public static final String COLON = ":";
    public static final String SPACE = " ";
    public static final String PERIOD = ".";

    public static final Splitter CSV_SPLITTER = Splitter.on(COMMA).trimResults().omitEmptyStrings();
    public static final Splitter COLON_SPLITTER = Splitter.on(COLON).trimResults().omitEmptyStrings();
    public static final Splitter SPACE_SPLITTER = Splitter.on(SPACE).trimResults().omitEmptyStrings();

    public static final String OAUTH_PREFIX = "/oauth";

    public static final String OAUTH2_ATTR_GRANT_TYPE = "grant_type";
    public static final String OAUTH2_ATTR_TOKEN_TYPE = "token_type";
    public static final String OAUTH2_ATTR_CLIENT_ID = "client_id";
    public static final String OAUTH2_ATTR_CLIENT_SECRET = "client_secret";
    public static final String OAUTH2_ATTR_ACCESS_TOKEN = "access_token";
    public static final String OAUTH2_ATTR_REFRESH_TOKEN = "refresh_token";
    public static final String OAUTH2_ATTR_EXPIRES_IN = "expires_in";
    public static final String OAUTH2_ATTR_EXPIRES = "expires";
    public static final String OAUTH2_ATTR_ACTIVE = "active";
    public static final String OAUTH2_ATTR_USER_ID = "user_id";
    public static final String OAUTH2_ATTR_METADATA = "metadata";
    public static final String OAUTH2_ATTR_USERNAME = "username";
    public static final String OAUTH2_ATTR_PASSWORD = "password";
    public static final String OAUTH2_ATTR_SCOPE = "scope";
    public static final String OAUTH2_ATTR_RESPONSE_TYPE = "response_type";
    public static final String OAUTH2_ATTR_REDIRECT_URI = "redirect_uri";
    public static final String OAUTH2_ATTR_STATE = "state";
    public static final String OAUTH2_ATTR_CODE = "code";
    public static final String OAUTH2_ATTR_2FA_CODE = "code2fa";
    public static final String OAUTH2_ATTR_ORGANIZATION_ID = "organization_id";

    public static final String OAUTH2_TOKEN_TYPE_BEARER = "bearer";

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_AUTHORIZATION_PREFIX_BASIC = "Basic ";
    public static final String HEADER_AUTHORIZATION_PREFIX_BEARER = "Bearer ";

    public static final String MSG_UNAUTHORIZED_REQUEST = "unauthorized request";
    public static final String MSG_INVALID_REQUEST = "invalid request";
    public static final String MSG_INVALID_GRANT_TYPE = "invalid grant_type";
    public static final String MSG_INVALID_TOKEN = "invalid token";
    public static final String MSG_INVALID_SCOPE = "invalid scope";
    public static final String MSG_ACCESS_DENIED = "access denied";

    public static final String METRIC_KEY_ACCESS_LOG_SERVICE_QUEUE = "log.service.queue";
}
