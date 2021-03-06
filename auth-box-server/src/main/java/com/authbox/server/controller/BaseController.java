package com.authbox.server.controller;

import com.authbox.base.config.AppProperties;
import com.authbox.base.dao.OrganizationDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.AccessLog;
import com.authbox.base.model.Organization;
import com.authbox.base.service.AccessLogService;
import com.authbox.server.config.RequestWrapperFilterConfiguration;
import com.authbox.server.service.ParsingValidationService;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Optional;

import static com.authbox.base.config.Constants.MSG_INVALID_REQUEST;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public abstract class BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseController.class);

    @Autowired
    protected AppProperties appProperties;

    @Autowired
    protected OrganizationDao organizationDao;

    @Autowired
    protected ParsingValidationService parsingValidationService;

    @Autowired
    protected AccessLogService accessLogService;

    protected Organization getOrganization(final HttpServletRequest req) {
        String host = URI.create(req.getRequestURL().toString()).getHost();
        final InetAddressValidator validator = InetAddressValidator.getInstance();
        if (isNotBlank(appProperties.getDomain())
                && !validator.isValidInet4Address(host)
                && !validator.isValidInet6Address(host)) {
            host = host.replace("." + appProperties.getDomain(), "");
        }
        MDC.put(RequestWrapperFilterConfiguration.REQUEST_DOMAIN_PREFIX_MDC_KEY, host);
        return getByDomainPrefix(host);
    }

    protected Organization getByDomainPrefix(final String organizationDomainPrefix) {
        final Optional<Organization> organization = organizationDao.getByDomainPrefix(organizationDomainPrefix);
        if (organization.isEmpty() || !organization.get().isEnabled()) {
            LOGGER.debug("Organization not found by domain_prefix='{}' or disabled", organizationDomainPrefix);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withError(MSG_INVALID_REQUEST),
                    "Organization not found by domain prefix='%s' or disabled", organizationDomainPrefix
            );
            throw new BadRequestException("Domain prefix unknown: " + organizationDomainPrefix);
        }
        MDC.put(RequestWrapperFilterConfiguration.REQUEST_ORGANIZATION_MDC_KEY, organization.get().getId());
        return organization.get();
    }
}
