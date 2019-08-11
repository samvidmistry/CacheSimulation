/**
 * Abstract idea of an eviction policy. A policy can be notified when an address is loaded
 * and it can be asked for the address which must be evicted next.
 */
public interface EvictionPolicy {
    /**
     * A notification of an address being loaded in the cache.
     * @param address address that was loaded in cache.
     */
    void load(String address);

    /**
     * This method can be called when cache wants to evict a page.
     * @return address of the page to be evicted.
     */
    String evict();
}
