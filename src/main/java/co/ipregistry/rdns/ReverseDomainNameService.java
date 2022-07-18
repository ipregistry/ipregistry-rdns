package co.ipregistry.rdns;

import co.ipregistry.ineter.base.IpAddress;

import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public final class ReverseDomainNameService {

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
        this(
                new InMemoryCache(),
                Runtime.getRuntime().availableProcessors() * 16,
                1000,
                2
        );
    }

    private ReverseDomainNameService(
            final Cache cache,
            final int parallelism,
            final int attemptReferenceCounterInitialCapacity,
            final int attemptCountBeforeCachingEmptyResponse) {

        this.attemptCountBeforeCachingEmptyResponse = attemptCountBeforeCachingEmptyResponse;
        this.attemptReferenceCounter = new ConcurrentHashMap<>(attemptReferenceCounterInitialCapacity, 0.75f, parallelism);
        this.cache = cache;
        this.threadPool = Executors.newWorkStealingPool(parallelism);
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

                return null;
            } else {
                cache.put(ip, new HostnameCacheEntry(hostname));
            }

            return hostname;
        }, threadPool);
    }

    private String doLookup(final IpAddress ip) {
        try {
            /*
             * Use a newly InitialDirContext every time, since JNDI specifies
             * that concurrent access to the same Context instance is not
             * guaranteed to be thread-safe
             */
            final Hashtable<String, String> env = new Hashtable<>(1, 1f);
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            final DirContext context = new InitialDirContext(env);

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
            context.close();
            return hostname.substring(0, hostname.length() - 1).intern();
        } catch (final javax.naming.NamingException e) {
            return null;
        }
    }

    private static String getIpv4ReverseDomain(final IpAddress inetAddress) {
        final String ip = inetAddress.toString();
        final String[] chunks = ip.split("\\.");
        return chunks[3] + "." + chunks[2] + "." + chunks[1] + "." + chunks[0] + "." + REVERSE_DNS_DOMAIN_IPV4;
    }

    private static String getIpv6ReverseDomain(final IpAddress inetAddress) {
        final byte[] bytes = inetAddress.toBigEndianArray();
        return reverseNibblesName(bytes) + REVERSE_DNS_DOMAIN_IPV6;
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
