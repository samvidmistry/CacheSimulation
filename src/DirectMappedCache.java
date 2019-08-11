import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DirectMappedCache implements Cache {
    // Map<Line, Tag>
    final Map<String, String> cache;
    private final int cachePower;
    private final int blockPower;

    public DirectMappedCache(final int cachePower, final int blockPower) {
        this.cachePower = cachePower;
        this.blockPower = blockPower;
        this.cache = new HashMap<>();
    }

    @Override
    public boolean load(String address) {
        final String addressWithoutBlock = address.substring(0, address.length() - blockPower);
        final String tag = addressWithoutBlock.substring(0, address.length() - cachePower);
        final String lineNo = addressWithoutBlock.substring(tag.length(), address.length() - blockPower);
        final String blockOffset = address.substring(address.length() - blockPower);
        if (cache.containsKey(lineNo) && cache.get(lineNo).equals(tag)) {
            return true;
        } else {
            cache.merge(lineNo, tag, (oldTag, newTag) -> newTag);
            return false;
        }
    }

    @Override
    public String toString() {
        return cache.entrySet().stream()
                .map(e -> String.format("line: %s -> tag: %s", e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));
    }
}
