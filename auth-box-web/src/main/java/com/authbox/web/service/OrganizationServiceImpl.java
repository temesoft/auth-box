package com.authbox.web.service;

import com.authbox.base.dao.OrganizationDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.Organization;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Service
@AllArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private static final String DOMAIN_PREFIX_REGEX = "^[a-zA-Z0-9]+$";
    private static final Collection<String> DISALLOWED_DOMAIN_PREFIX = List.of("www");
    private static final Pattern DOMAIN_PREFIX_PATTERN = Pattern.compile(DOMAIN_PREFIX_REGEX);

    private final Clock defaultClock;
    private final OrganizationDao organizationDao;

    /**
     * Returns a map for verification of domain presence for provided organization
     * Example: {"exists": true}
     */
    @Override
    public Map<String, Object> checkAvailableDomainPrefix(final Organization organization, final String domainPrefix) {
        validateDomainPrefix(domainPrefix, organization.getId());
        return Map.of("exists", organizationDao.getByDomainPrefix(domainPrefix.trim()).isPresent());
    }

    /**
     * Updates organization using provided organization and updated OrganizationDto object
     */
    @Override
    public OrganizationDto update(final Organization organization, final OrganizationDto updatedOrganization) {
        if (!organization.getId().equals(updatedOrganization.getId())) {
            throw new BadRequestException("Invalid company id");
        }
        validateDomainPrefix(updatedOrganization.getDomainPrefix(), organization.getId());
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

    /**
     * Validates a domain prefix for provided organization id
     */
    @Override
    public void validateDomainPrefix(final String domainPrefix, final String providedOrganizationId) {
        if (!DOMAIN_PREFIX_PATTERN.matcher(domainPrefix).matches()) {
            throw new BadRequestException("Domain prefix can only contain letters and numbers");
        }
        if (DISALLOWED_DOMAIN_PREFIX.contains(domainPrefix.toLowerCase())) {
            throw new BadRequestException("Selected domain prefix is not allowed");
        }
        val existingByDomainPrefix = organizationDao.getByDomainPrefix(domainPrefix);
        if (existingByDomainPrefix.isPresent() && !existingByDomainPrefix.get().getId().equals(providedOrganizationId)) {
            throw new BadRequestException("Domain prefix is already taken by another organization");
        }
    }
}
