package co.ipregistry.rdns;

import co.ipregistry.ineter.base.IpAddress;

/**
 * A cache interface for storing reverse DNS lookup results.
 */
public interface Cache {

    /**
     * Retrieves a cached entry for the given IP address.
     *
     * @param ip the IP address to look up
     * @return the cached entry, or null if not found
     */
    CacheEntry get(IpAddress ip);

    /**
     * Stores a cache entry for the given IP address.
     *
     * @param ip the IP address to cache
     * @param entry the cache entry to store
     */
    void put(IpAddress ip, CacheEntry entry);

}
