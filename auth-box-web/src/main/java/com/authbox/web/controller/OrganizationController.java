package com.authbox.web.controller;

import com.authbox.base.dao.OrganizationDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.Organization;
import com.authbox.web.config.Constants;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@RestController
@RequestMapping(Constants.API_PREFIX + "/organization")
public class OrganizationController extends BaseController {

    private static final String DOMAIN_PREFIX_REGEX = "^[a-zA-Z0-9]+$";
    private static final Collection<String> DISALLOWED_DOMAIN_PREFIX = ImmutableList.of("www");
    private static final Pattern DOMAIN_PREFIX_PATTERN = Pattern.compile(DOMAIN_PREFIX_REGEX);

    private final OrganizationDao organizationDao;
    private final Clock defaultClock;

    public OrganizationController(final OrganizationDao organizationDao, final Clock defaultClock) {
        this.organizationDao = organizationDao;
        this.defaultClock = defaultClock;
    }

    @GetMapping
    public Organization getOrganizationDetails() {
        return getOrganization();
    }

    @GetMapping("/available-domain-prefix/{domainPrefix}")
    public Map<String, Object> checkAvailableDomainPrefix(@PathVariable("domainPrefix") final String domainPrefix) {
        final Organization organization = getOrganization();
        validateDomainPrefix(organizationDao, domainPrefix, organization.getId());
        return ImmutableMap.of("exists", organizationDao.getByDomainPrefix(domainPrefix.trim()).isPresent());
    }


    @PostMapping
    @Transactional
    public Organization update(@RequestBody final Organization updatedOrganization) {
        final Organization organization = getOrganization();
        if (!organization.getId().equals(updatedOrganization.getId())) {
            throw new BadRequestException("Invalid company id");
        }
        validateDomainPrefix(organizationDao, updatedOrganization.getDomainPrefix(), organization.getId());
        if (isEmpty(updatedOrganization.getName())) {
            throw new BadRequestException("Organization name can not be empty");
        }
        updatedOrganization.setLastUpdated(Instant.now(defaultClock));
        organizationDao.update(
                updatedOrganization.getId(),
                updatedOrganization.getName(),
                updatedOrganization.getDomainPrefix(),
                updatedOrganization.getAddress(),
                updatedOrganization.isEnabled(),
                updatedOrganization.getLogoUrl(),
                updatedOrganization.getLastUpdated()
        );
        return updatedOrganization;
    }

    static void validateDomainPrefix(final OrganizationDao organizationDao, final String domainPrefix, final String providedOrganizationId) {
        if (!DOMAIN_PREFIX_PATTERN.matcher(domainPrefix).matches()) {
            throw new BadRequestException("Domain prefix can only contain letters and numbers");
        }
        if (DISALLOWED_DOMAIN_PREFIX.contains(domainPrefix.toLowerCase())) {
            throw new BadRequestException("Selected domain prefix is not allowed");
        }
        final Optional<Organization> existingByDomainPrefix = organizationDao.getByDomainPrefix(domainPrefix);
        if (existingByDomainPrefix.isPresent() && !existingByDomainPrefix.get().getId().equals(providedOrganizationId)) {
            throw new BadRequestException("Domain prefix is already taken by another organization");
        }
    }
}
