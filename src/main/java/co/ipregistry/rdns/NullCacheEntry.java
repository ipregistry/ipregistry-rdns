package co.ipregistry.rdns;

/**
 * A singleton cache entry representing a null or failed lookup result.
 */
public final class NullCacheEntry implements CacheEntry {

    private static final class Instance {

        private static final NullCacheEntry INSTANCE = new NullCacheEntry();

    }

    /**
     * Constructs a new null cache entry.
     */
    private NullCacheEntry() {
    }

    @Override
    public String getHostName() {
        return null;
    }

    /**
     * Returns the singleton instance of NullCacheEntry.
     *
     * @return the singleton instance
     */
    public static NullCacheEntry getInstance() {
        return Instance.INSTANCE;
    }

}
