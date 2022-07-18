package co.ipregistry.rdns;

import co.ipregistry.ineter.base.IpAddress;

public final class NoCache implements Cache {

    private NoCache() {

    }

    @Override
    public CacheEntry get(final IpAddress ip) {
        return null;
    }

    @Override
    public void put(final IpAddress ip, final CacheEntry entry) {
        // do nothing
    }

}
