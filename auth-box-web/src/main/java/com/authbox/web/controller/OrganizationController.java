package com.authbox.web.controller;

import com.authbox.web.config.Constants;
import com.authbox.web.service.OrganizationService;
import com.authbox.web.service.OrganizationService.OrganizationDto;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(Constants.API_PREFIX + "/organization")
@AllArgsConstructor
public class OrganizationController extends BaseController {

    private final OrganizationService organizationService;

    @GetMapping
    public OrganizationDto getOrganizationDetails() {
        return OrganizationDto.fromEntity(getOrganization());
    }

    @GetMapping("/available-domain-prefix/{domainPrefix}")
    public Map<String, Object> checkAvailableDomainPrefix(@PathVariable("domainPrefix") final String domainPrefix) {
        return organizationService.checkAvailableDomainPrefix(getOrganization(), domainPrefix);
    }

    @PostMapping
    @Transactional
    public OrganizationDto update(@RequestBody final OrganizationDto updatedOrganization) {
        return organizationService.update(getOrganization(), updatedOrganization);
    }
}
