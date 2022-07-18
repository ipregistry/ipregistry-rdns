package co.ipregistry.rdns;

import co.ipregistry.ineter.base.IpAddress;

public interface Cache {

    CacheEntry get(IpAddress ip);

    void put(IpAddress ip, CacheEntry entry);

}
