/**
 * Storage plugin system: discovery, registration, and lifecycle of pluggable storage backends.
 *
 * <p>Plugin exceptions ({@code PluginException}, {@code PluginNotFoundException}) belong here
 * because they are part of the plugin contract, not generic storage errors.</p>
 */
package fr.inria.corese.core.next.storagemanager.api.plugin;
