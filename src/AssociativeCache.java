/**
 * Abstract idea of an Associative Cache. Every associative cache, whether it be Fully
 * associative or Set associative, needs an eviction policy to decide which page to
 * evict next. So it forces every subclass to define an {@link EvictionPolicy}.
 */
public abstract class AssociativeCache implements Cache {
    final protected EvictionPolicy evictionPolicy;

    protected AssociativeCache(EvictionPolicy evictionPolicy) {
        this.evictionPolicy = evictionPolicy;
    }

    /**
     * Evict an address from cache
     * @return the address of evicted block.
     */
    protected abstract String evict();
}
