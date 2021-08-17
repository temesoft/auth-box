package com.authbox.web.config;

import com.google.common.base.Splitter;

public class Constants {

    public static final String COMMA = ",";
    public static final String COLON = ":";
    public static final String SPACE = " ";
    public static final String PERIOD = ".";

    public static final Splitter CSV_SPLITTER = Splitter.on(COMMA).trimResults().omitEmptyStrings();
    public static final Splitter COLON_SPLITTER = Splitter.on(COLON).trimResults().omitEmptyStrings();
    public static final Splitter SPACE_SPLITTER = Splitter.on(SPACE).trimResults().omitEmptyStrings();

    public static final String API_PREFIX = "/api";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_AUTHORIZATION_PREFIX_BEARER = "Bearer ";

}
