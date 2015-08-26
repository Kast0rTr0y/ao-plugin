package com.atlassian.activeobjects.spi;

import com.atlassian.tenancy.api.TenantContext;

/**
 * Products that can't yet provide {@link com.atlassian.tenancy.api.TenantContext#getCurrentTenant()} that works in _all_
 * threads should expose a {@link com.atlassian.activeobjects.spi.CompatibilityTenantContextImpl} in their SPI.
 *
 * @since 0.26.1
 */
public interface CompatibilityTenantContext extends TenantContext {
}
