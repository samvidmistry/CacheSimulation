/**
 * The most abstract idea of Cache. A cache can just be asked to load an item.
 */
public interface Cache {
    /**
     * Load or fetch the provided address.
     * @param address address to be fetched
     * @return true if address was present in cache, false otherwise
     */
    boolean load(final String address);
}
