package co.ipregistry.rdns;

import co.ipregistry.ineter.base.Ipv4Address;
import co.ipregistry.ineter.base.Ipv6Address;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public final class ReverseDomainNameServiceTest {

    @Test
    void testGetIpv4ReverseDomain() {
        Assertions.assertEquals(
                "4.4.8.8.in-addr.arpa",
                ReverseDomainNameService.getIpv4ReverseDomain(Ipv4Address.of("8.8.4.4")));
    }

    @Test
    void testGetIpv6ReverseDomain() {
        Assertions.assertEquals(
                "b.a.9.8.7.6.5.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.d.0.1.0.0.2.ip6.arpa",
                ReverseDomainNameService.getIpv6ReverseDomain(Ipv6Address.of("2001:db8::567:89ab")));
    }

    @Test
    public void testIpv4ReverseLookupWithHostNameAvailable() throws ExecutionException, InterruptedException {
        final ReverseDomainNameService reverseDomainNameService = new ReverseDomainNameService();
        final CompletableFuture<String> response = reverseDomainNameService.lookup(Ipv4Address.of("8.8.8.8"));
        Assertions.assertEquals("dns.google", response.get());
    }

    @Test
    public void testIpv4ReverseLookupWithHostNameUnavailable() throws ExecutionException, InterruptedException {
        final ReverseDomainNameService reverseDomainNameService = new ReverseDomainNameService();
        final CompletableFuture<String> response = reverseDomainNameService.lookup(Ipv4Address.of("22.22.22.22"));
        Assertions.assertNull(response.get());
    }

    @Test
    public void testIpv6ReverseLookupWithHostNameAvailable() throws ExecutionException, InterruptedException {
        final ReverseDomainNameService reverseDomainNameService = new ReverseDomainNameService();
        final CompletableFuture<String> response = reverseDomainNameService.lookup(Ipv6Address.of("2001:4860:4860::8888"));
        Assertions.assertEquals("dns.google", response.get());
    }

    @Test
    public void testStringInterningForCachedEntriesWithSameValue() throws ExecutionException, InterruptedException {
        final ReverseDomainNameService reverseDomainNameService = new ReverseDomainNameService();
        final String responseOne = reverseDomainNameService.lookup(Ipv4Address.of("8.8.8.8")).get();
        final String responseTwo = reverseDomainNameService.lookup(Ipv6Address.of("2001:4860:4860::8888")).get();

        Assertions.assertEquals("dns.google", responseOne);
        Assertions.assertEquals("dns.google", responseTwo);
        Assertions.assertEquals(System.identityHashCode(responseOne), System.identityHashCode(responseTwo));
    }


    @Test
    public void testNullCachingDisabled() throws ExecutionException, InterruptedException {
        int parallelism = Runtime.getRuntime().availableProcessors() * 16;
        final ReverseDomainNameService reverseDomainNameService =
                new ReverseDomainNameService(
                        Executors.newVirtualThreadPerTaskExecutor(),
                        new InMemoryCache(
                                1024 * 1024,
                                Duration.of(24, ChronoUnit.HOURS),
                                parallelism
                        ),
                        parallelism,
                        1000,
                        -1
                );

        final Ipv4Address ipWithNoRdn = Ipv4Address.of("81.18.81.18");
        final String nullResponse = reverseDomainNameService.lookup(ipWithNoRdn).get();
        Assertions.assertNull(nullResponse);
        Assertions.assertNull(reverseDomainNameService.getCache().get(ipWithNoRdn));

        final Ipv4Address ipWithRdn = Ipv4Address.of("8.8.8.8");
        final String nonNullResponse = reverseDomainNameService.lookup(ipWithRdn).get();
        Assertions.assertNotNull(nonNullResponse);
        Assertions.assertNotNull(reverseDomainNameService.getCache().get(ipWithRdn));
    }

}
