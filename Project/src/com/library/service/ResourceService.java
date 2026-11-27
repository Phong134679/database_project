package com.library.service;

import com.library.repository.GrantedLicenseRepository;
import com.library.repository.ResourceRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Service class coordinating general resource catalog queries and shared license entitlement checks.
 */
public class ResourceService {

    private ResourceRepository resourceRepository;
    private GrantedLicenseRepository grantedLicenseRepository;

    public ResourceService() {
        this(new ResourceRepository(), new GrantedLicenseRepository());
    }

    public ResourceService(ResourceRepository resourceRepository) {
        this(resourceRepository, new GrantedLicenseRepository());
    }

    public ResourceService(ResourceRepository resourceRepository, GrantedLicenseRepository grantedLicenseRepository) {
        this.resourceRepository = resourceRepository;
        this.grantedLicenseRepository = grantedLicenseRepository;
    }

    public List<Map<String, Object>> getAllResourcesDetailed() {
        return resourceRepository.findAllResourcesDetailed();
    }

    /**
     * Checks whether at least one required license package is configured for the resource.
     */
    public boolean hasRequiredLicensesConfigured(Connection conn, int resourceId) throws SQLException {
        return resourceRepository.countRequiredLicenses(conn, resourceId) > 0;
    }

    /**
     * Shared Java equivalent of the former fn_has_all_required_licenses(person_id, resource_id):
     * returns true iff the resource requires at least one license AND the person holds all of them.
     */
    public boolean hasAllRequiredLicenses(Connection conn, int personId, int resourceId) throws SQLException {
        int requiredCount = resourceRepository.countRequiredLicenses(conn, resourceId);
        if (requiredCount == 0) {
            return false;
        }
        int grantedCount = grantedLicenseRepository.countGrantedRequiredLicenses(conn, personId, resourceId);
        return grantedCount == requiredCount;
    }

    public ResourceRepository getResourceRepository() {
        return resourceRepository;
    }

    public void setResourceRepository(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public GrantedLicenseRepository getGrantedLicenseRepository() {
        return grantedLicenseRepository;
    }

    public void setGrantedLicenseRepository(GrantedLicenseRepository grantedLicenseRepository) {
        this.grantedLicenseRepository = grantedLicenseRepository;
    }
}
