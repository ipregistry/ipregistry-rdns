package co.ipregistry.rdns;

public final class NullCacheEntry implements CacheEntry {

    private static final class Instance {

        private static final NullCacheEntry INSTANCE = new NullCacheEntry();

    }

    @Override
    public String getHostName() {
        return null;
    }

    public static NullCacheEntry getInstance() {
        return Instance.INSTANCE;
    }

}
