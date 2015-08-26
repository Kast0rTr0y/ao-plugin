package com.atlassian.activeobjects.external;

public final class NoDataSourceException extends IllegalStateException {
    public NoDataSourceException() {
        super("No data source is available, indicating that there is no tenant available to the "
                + "application. Your plugin is probably performing tenant specific operations too eagerly.\n"
                + "Use a com.atlassian.tenancy.api.helper.PerTenantInitialiser or wait for a "
                + "com.atlassian.tenancy.api.event.TenantArrivedEvent");
    }
}
