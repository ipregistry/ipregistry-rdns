package co.ipregistry.rdns;

/**
 * Represents a cached reverse DNS lookup result.
 */
public interface CacheEntry {

    /**
     * Returns the hostname for this cache entry.
     *
     * @return the hostname, or null if the lookup failed or returned no results
     */
    String getHostName();

}
