package co.ipregistry.rdns;

import co.ipregistry.ineter.base.IpAddress;
import com.google.common.cache.CacheBuilder;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public final class InMemoryCache implements Cache {

    private final com.google.common.cache.Cache<IpAddress, CacheEntry> cache;


    public InMemoryCache() {
        this(
                1024 * 1024,
                Duration.of(24, ChronoUnit.HOURS),
                Runtime.getRuntime().availableProcessors() * 16
        );
    }

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
