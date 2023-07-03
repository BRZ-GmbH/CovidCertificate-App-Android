package at.gv.brz.sdk.repository

/**
 * Result class for update status of TrustlistRepository. refreshed is true when the status changed in a way
 * so that the certificate display should be refresh. failed is true when the data refresh failed for any of the
 * data sources (trust list, business rules, value sets)
 */
data class RefreshResult(val refreshed: Boolean, val failed: Boolean)