package com.atlassian.activeobjects.spi;

import com.atlassian.tenancy.api.Tenant;
import com.atlassian.tenancy.api.TenantAccessor;

import java.util.Iterator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Generic tenant provider, operating via {@link com.atlassian.tenancy.api.TenantAccessor}.
 *
 * @since 0.26
 */
public class TenantProviderImpl implements TenantProvider
{
    private final TenantAccessor tenantAccessor;

    public TenantProviderImpl(@Nonnull final TenantAccessor tenantAccessor)
    {
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
    public Tenant getCurrentTenant()
    {
        // just get the the first (and only) tenant
        Iterator<Tenant> tenantIterator = tenantAccessor.getAvailableTenants().iterator();
        if (tenantIterator.hasNext())
        {
            return tenantIterator.next();
        }
        else
        {
            return null;
        }
    }
}