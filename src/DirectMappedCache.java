import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of a direct mapped cache.
 */
public class DirectMappedCache implements Cache {
    // Map<Line, Tag>
    final Map<String, String> cache;
    private final int cachePower;
    private final int blockPower;

    /**
     * Constructs a DirectMappedCache
     * @param cachePower log_2 (size of cache)
     * @param blockPower log_2 (size of a single cache block/line)
     */
    public DirectMappedCache(final int cachePower, final int blockPower) {
        this.cachePower = cachePower;
        this.blockPower = blockPower;
        this.cache = new HashMap<>();
    }

    /**
     * Loads an address from the cache
     * @param address address to be fetched
     * @return true if item was found in cache, false otherwise
     */
    @Override
    public boolean load(String address) {
        final String tag = address.substring(0, address.length() - cachePower);
        final String lineNo = address.substring(tag.length(), address.length() - blockPower);
        final String blockOffset = address.substring(address.length() - blockPower);
        if (cache.containsKey(lineNo) && cache.get(lineNo).equals(tag)) {
            return true;
        } else {
            cache.merge(lineNo, tag, (oldTag, newTag) -> newTag);
            return false;
        }
    }

    /**
     * Prints the contents of the cache.
     * @return cache contents
     */
    @Override
    public String toString() {
        if (cache.size() == 0) {
            return "Empty Cache";
        }
        return cache.entrySet().stream()
                .map(e -> String.format("line: %s -> tag: %s", e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));
    }
}
