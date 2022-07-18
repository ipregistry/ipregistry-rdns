# ipregistry-rdns

Java library to perform reverse Domain Name Service (rDNS) lookups with ease.

The library is async, thread-safe and has built-in support for caching.

```java
import co.ipregistry.rdns.ReverseDomainNameService;
import java.net.InetAddress;

final ReverseDomainNameService reverseDomainNameService = new ReverseDomainNameService();
final CompletableFuture<String> response = reverseDomainNameService.lookup(InetAddress.getByName("8.8.8.8"));
Assertions.assertEquals("dns.google", response.get());
```
