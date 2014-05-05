package com.atlassian.activeobjects.internal;

import com.atlassian.tenancy.api.Tenant;

import javax.annotation.Nullable;

/**
 * Provides the current tenant information.
 *
 * @since 0.26
 */
public interface TenantProvider
{
    /**
     * Retrieve the current tenant
     *
     * @return null for no tenant
     */
    @Nullable
    Tenant getTenant();
}
