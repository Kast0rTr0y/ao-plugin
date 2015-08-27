package com.atlassian.activeobjects.spi;

import com.atlassian.tenancy.api.Tenant;
import com.atlassian.tenancy.api.TenantAccessor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Products that can't yet provide {@link com.atlassian.tenancy.api.TenantContext#getCurrentTenant()} that works in _all_
 * threads should expose a {@link com.atlassian.activeobjects.spi.CompatibilityTenantContextImpl} in their SPI.
 *
 * @since 0.26.1
 */
public class CompatibilityTenantContextImpl implements CompatibilityTenantContext {
    private final TenantAccessor tenantAccessor;

    public CompatibilityTenantContextImpl(@Nonnull final TenantAccessor tenantAccessor) {
        this.tenantAccessor = checkNotNull(tenantAccessor);
    }

    /**
     * Retrieve the current tenant.
     *
     * Hacky as hell for now; it just retrieves the first of the available tenants.
     *
     * We don't really know, yet, how we're going to identify the current tenant when there are multiple available,
     * however it can be done in this one and only one place. Probably going to be a thread local...
     *
     * @return null if no tenant present
     */
    @Nullable
    @Override
    public Tenant getCurrentTenant() {
        // just get the the first (and only) tenant
        Iterator<Tenant> tenantIterator = tenantAccessor.getAvailableTenants().iterator();
        if (tenantIterator.hasNext()) {
            return tenantIterator.next();
        } else {
            return null;
        }
    }
}