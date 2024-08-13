package com.authbox.server.controller;

import com.authbox.base.config.AppProperties;
import com.authbox.base.dao.OrganizationDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.AccessLog;
import com.authbox.base.model.Organization;
import com.authbox.base.service.AccessLogService;
import com.authbox.server.filter.RequestWrapperFilter;
import com.authbox.server.service.ParsingValidationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.Optional;

import static com.authbox.base.config.Constants.MSG_INVALID_REQUEST;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
public abstract class BaseController {

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
        MDC.put(RequestWrapperFilter.REQUEST_DOMAIN_PREFIX_MDC_KEY, host);
        return getByDomainPrefix(host);
    }

    protected Organization getByDomainPrefix(final String organizationDomainPrefix) {
        final Optional<Organization> organization;
        if ("localhost.".equalsIgnoreCase(organizationDomainPrefix)) {
            organization = organizationDao.getByDomainPrefix("localhost");
        } else {
            organization = organizationDao.getByDomainPrefix(organizationDomainPrefix);
        }
        if (organization.isEmpty()) {
            log.debug("Organization not found by domain_prefix='{}'", organizationDomainPrefix);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withError(MSG_INVALID_REQUEST),
                    "Organization not found by domain prefix='%s'", organizationDomainPrefix
            );
            throw new BadRequestException("Domain prefix unknown: " + organizationDomainPrefix);
        }
        if (!organization.get().isEnabled()) {
            log.debug("Organization with domain_prefix='{}' is disabled", organizationDomainPrefix);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withError(MSG_INVALID_REQUEST),
                    "Organization with prefix='%s' is disabled", organizationDomainPrefix
            );
            throw new BadRequestException("Domain is disabled: " + organizationDomainPrefix);
        }
        MDC.put(RequestWrapperFilter.REQUEST_ORGANIZATION_MDC_KEY, organization.get().getId());
        return organization.get();
    }
}
