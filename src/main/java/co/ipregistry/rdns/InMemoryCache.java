package co.ipregistry.rdns;

import co.ipregistry.ineter.base.IpAddress;
import com.google.common.cache.CacheBuilder;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * An in-memory cache implementation for reverse DNS lookups using Guava cache.
 */
public final class InMemoryCache implements Cache {

    private final com.google.common.cache.Cache<IpAddress, CacheEntry> cache;


    /**
     * Constructs a new in-memory cache with default settings.
     * Default cache size is 1,048,576 entries, timeout is 24 hours,
     * and parallelism is 16 times the number of available processors.
     */
    public InMemoryCache() {
        this(
                1024 * 1024,
                Duration.of(24, ChronoUnit.HOURS),
                Runtime.getRuntime().availableProcessors() * 16
        );
    }

    /**
     * Constructs a new in-memory cache with custom settings.
     *
     * @param cacheSize the maximum number of entries to store in the cache
     * @param cacheTimeout the duration after which entries expire
     * @param parallelism the estimated number of concurrently updating threads
     */
    public InMemoryCache(
            final long cacheSize,
            final Duration cacheTimeout,
            final int parallelism) {

        this.cache =
                CacheBuilder.newBuilder()
                        .concurrencyLevel(parallelism)
                        .maximumSize(cacheSize)
                        .expireAfterWrite(cacheTimeout.getSeconds(), TimeUnit.SECONDS)
                        .build();
    }

    @Override
    public CacheEntry get(final IpAddress ip) {
        return cache.getIfPresent(ip);
    }

    @Override
    public void put(final IpAddress ip, final CacheEntry entry) {
        cache.put(ip, entry);
    }

}
