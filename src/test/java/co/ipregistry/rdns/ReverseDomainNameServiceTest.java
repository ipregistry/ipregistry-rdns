package co.ipregistry.rdns;

import co.ipregistry.ineter.base.Ipv4Address;
import co.ipregistry.ineter.base.Ipv6Address;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class ReverseDomainNameServiceTest {

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

}
