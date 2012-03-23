/**
 * <p>This is the SPI package for the Active Objects plugin. Application that want to embed the Active Object plugin will
 * have to implement this SPI and expose those implementation as services. Here is the list of necessary services:<p>
 * <dl>
 * <dt>{@link com.atlassian.activeobjects.spi.DataSourceProvider}</dt>
 * <dd>This is a <strong>required</strong> service that the host application must provide. It allows the Active Objects
 * capable plugin to access the database.</dd>
 * <dt>{@link com.atlassian.activeobjects.spi.ActiveObjectsPluginConfiguration}</dt>
 * <dd>This is an <strong>optional</strong> service that can be provided by the host application. This service defines
 * various configurable options for the Active Objects plugin to use. Convenient defaults will be used if the service
 * is not defined.</dd>
 * <dt>{@link com.atlassian.activeobjects.spi.BackupRegistry}</dt>
 * <dd>This is an <strong>optional</strong> service that can be provided by the host application. When this service is
 * available, the Active Objects plugin will register itself as a {@link com.atlassian.activeobjects.spi.Backup} and it
 * makes it possible to backup all Active Objects plugins alongside the usual host application backup.</dd>
 * </dl>
 * <p>For more information on each service, refer to their respective documentation.</p>
 */
package com.atlassian.activeobjects.spi;