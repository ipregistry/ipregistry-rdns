package co.ipregistry.rdns;

import co.ipregistry.ineter.base.IpAddress;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public final class ReverseDomainNameService implements AutoCloseable {

    private final static char[] HEXADECIMAL_CHARACTERS =
            new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static final String[] LOOKUP_ATTRIBUTE_IDS = {"PTR"};

    private static final String REVERSE_DNS_DOMAIN_IPV4 = "in-addr.arpa";

    private static final String REVERSE_DNS_DOMAIN_IPV6 = "ip6.arpa";

    private final int attemptCountBeforeCachingEmptyResponse;

    private final Map<IpAddress, AtomicInteger> attemptReferenceCounter;

    private final Cache cache;

    private final ExecutorService threadPool;


    public ReverseDomainNameService() {
        this(Runtime.getRuntime().availableProcessors() * 16);
    }

    public ReverseDomainNameService(int concurrency) {
        this(
                Executors.newVirtualThreadPerTaskExecutor(),
                new InMemoryCache(),
                concurrency,
                1000,
                2
        );
    }

    public ReverseDomainNameService(
            final ExecutorService threadPool,
            final Cache cache,
            final int concurrency,
            final int attemptReferenceCounterInitialCapacity,
            final int attemptCountBeforeCachingEmptyResponse) {

        this.attemptCountBeforeCachingEmptyResponse = attemptCountBeforeCachingEmptyResponse;
        this.attemptReferenceCounter = new ConcurrentHashMap<>(attemptReferenceCounterInitialCapacity, 0.75f, concurrency);
        this.cache = cache;
        this.threadPool = threadPool;
    }

    @Override
    public void close() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(1, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
                threadPool.awaitTermination(1, TimeUnit.SECONDS);
            }
        } catch (InterruptedException ie) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public Cache getCache() {
        return cache;
    }

    public CompletableFuture<String> lookup(final InetAddress ip) {
        return lookup(IpAddress.of(ip));
    }

    public CompletableFuture<String> lookup(final IpAddress ip) {
        final CacheEntry cachedHostname = cache.get(ip);

        if (cachedHostname != null) {
            return CompletableFuture.supplyAsync(cachedHostname::getHostName);
        }

        return CompletableFuture.supplyAsync(() -> {
            final String hostname = doLookup(ip);

            if (hostname == null) {
                if (attemptCountBeforeCachingEmptyResponse > -1) {
                    final AtomicInteger attemptCount = attemptReferenceCounter.compute(ip, (ipAddress, counter) -> {
                        if (counter == null) {
                            return new AtomicInteger(1);
                        }

                        if (counter.incrementAndGet() == attemptCountBeforeCachingEmptyResponse) {
                            return null;
                        }

                        return counter;
                    });
                    if (attemptCount == null) {
                        cache.put(ip, NullCacheEntry.getInstance());
                        return null;
                    } else {
                        attemptReferenceCounter.put(ip, new AtomicInteger(1));
                    }
                }

                return null;
            } else {
                cache.put(ip, new HostnameCacheEntry(hostname));
            }

            return hostname;
        }, threadPool);
    }

    private String doLookup(final IpAddress ip) {
        final Hashtable<String, String> env = new Hashtable<>(1, 1f);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

        DirContext context = null;
        try {
            /*
             * Use a newly InitialDirContext every time, since JNDI specifies
             * that concurrent access to the same Context instance is not
             * guaranteed to be thread-safe
             */
            context = new InitialDirContext(env);

            final Attributes attrs =
                    context.getAttributes(
                            ip.version() == 4 ?
                                    getIpv4ReverseDomain(ip) :
                                    getIpv6ReverseDomain(ip),
                            LOOKUP_ATTRIBUTE_IDS);

            final Attribute ptrs = attrs.get("PTR");

            if (ptrs == null || ptrs.size() == 0) {
                return null;
            }

            final String hostname = (String) ptrs.get(0);
            return hostname.substring(0, hostname.length() - 1).intern();
        } catch (final NamingException e) {
            return null;
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException e) {
                    // ignore
                }
            }
        }
    }

    static String getIpv4ReverseDomain(final IpAddress inetAddress) {
        final String ip = inetAddress.toString();
        final String[] chunks = ip.split("\\.");
        return chunks[3] + "." + chunks[2] + "." + chunks[1] + "." + chunks[0] + "." + REVERSE_DNS_DOMAIN_IPV4;
    }

    static String getIpv6ReverseDomain(final IpAddress inetAddress) {
        final byte[] bytes = inetAddress.toBigEndianArray();
        return reverseNibblesName(bytes) + REVERSE_DNS_DOMAIN_IPV6;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public static String reverseNibblesName(final byte[] bytes) {
        final int inputLength = bytes.length;
        final StringBuilder result = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            final int b = bytes[inputLength - i - 1] & 0xff;
            final char c2 = HEXADECIMAL_CHARACTERS[b >>> 4];
            final char c1 = HEXADECIMAL_CHARACTERS[b & 0x0f];

            result.append(c1);
            result.append('.');
            result.append(c2);
            result.append('.');
        }

        return result.toString();
    }

}
