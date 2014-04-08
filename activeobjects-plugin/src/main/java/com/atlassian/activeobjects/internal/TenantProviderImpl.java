package com.atlassian.activeobjects.internal;

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

    @Nullable
    @Override
    public Tenant getTenant()
    {
        // stubbed until this is available from {@link com.atlassian.tenancy.api.TenantAccessor}
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
