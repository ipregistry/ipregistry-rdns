package co.ipregistry.rdns;

import java.util.Objects;

/**
 * A cache entry that contains a hostname.
 */
public final class HostnameCacheEntry implements CacheEntry {

    private final String hostname;


    /**
     * Constructs a new hostname cache entry.
     *
     * @param hostname the hostname to cache
     */
    public HostnameCacheEntry(final String hostname) {
        this.hostname = hostname;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final HostnameCacheEntry that = (HostnameCacheEntry) o;
        return Objects.equals(hostname, that.hostname);
    }

    @Override
    public String getHostName() {
        return hostname;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname);
    }

}
