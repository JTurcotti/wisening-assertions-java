package core.codestructure;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * An IndexingGenerator facilitates the creation of fresh instances
 * of OutputT. At each generation, some unique information of type
 * IndexingT may be provided and then that information can be used to
 * look up the generated object number.
 * @param <IndexingT>
 */
public class Indexer<OutputT, IndexingT> {
    private final Map<IndexingT, OutputT> index = new HashMap<>();

    private final Function<Integer, OutputT> constructor;

    public Indexer(Function<Integer, OutputT> constructor) {
        this.constructor = constructor;
    }

    private int next = 0;

    public OutputT next(IndexingT ind) {
        OutputT n = constructor.apply(next++);
        index.put(ind, n);
        return n;
    }

    public Optional<OutputT> lookup(IndexingT i) {
        return Optional.ofNullable(index.get(i));
    }
}
