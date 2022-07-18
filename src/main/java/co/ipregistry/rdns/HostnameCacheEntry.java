package co.ipregistry.rdns;

import java.util.Objects;

public final class HostnameCacheEntry implements CacheEntry {

    private final String hostname;


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
