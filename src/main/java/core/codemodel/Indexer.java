package core.codemodel;

import core.codemodel.elements.Procedure;
import core.codemodel.events.Line;
import spoon.reflect.code.*;
import spoon.reflect.cu.SourcePositionHolder;
import spoon.reflect.declaration.CtElement;
import util.Pair;

import java.util.*;
import java.util.function.Function;

/**
 * An IndexingGenerator facilitates the creation of fresh instances
 * of OutputT. At each generation, some unique information of type
 * IndexingT may be provided and then that information can be used to
 * look up the generated object number. Information of type AuxT can also
 * be provided to be associated with the object
 *
 * An AuxT is not needed to lookup an OutputT - only an IndexingT is
 *
 * @param <IndexingT>
 */
public class Indexer<OutputT, IndexingT, AuxT> {
    private final HashMap<IndexingT, OutputT> index = new HashMap<>();
    private final Map<OutputT, AuxT> auxMap = new HashMap<>();

    private final Function<Integer, OutputT> constructor;
    private int next;

    //used if a serialized label map from a prior round was passed to this indexer at construction time
    private final Optional<Map<IndexingT, OutputT>> precedentMap;

    public Indexer(Function<Integer, OutputT> constructor) {
        this.constructor = constructor;
        this.precedentMap = Optional.empty();
        this.next = 0;
    }

    public Indexer(Function<Integer, OutputT> constructor, Map<IndexingT, OutputT> precedentMap, int next) {
        this.constructor = constructor;
        this.precedentMap = Optional.of(precedentMap);
        this.next = next;
    }

    public OutputT next(IndexingT ind, AuxT aux) {
        OutputT n = next(ind);
        if (auxMap.values().stream().anyMatch((v -> v == aux))) {
            //TODO: delete once its been passing for a while
            throw new IllegalStateException("aux value duplicated: " + aux);
        }
        auxMap.put(n, aux);
        return n;
    }

    //warning: using this method fails to associate aux data
    public OutputT next(IndexingT ind) {
        OutputT out;
        //consult precedent map before creating new
        if (precedentMap.map(m -> m.containsKey(ind)).orElse(false)) {
            out = precedentMap.get().get(ind);
        } else {
            out = constructor.apply(next++);
        }
        index.put(ind, out);
        return out;
    }

    public Optional<OutputT> lookup(IndexingT i) {
        return Optional.ofNullable(index.get(i));
    }

    public Optional<AuxT> lookupAux(OutputT o) {
        return Optional.ofNullable(auxMap.get(o));
    }

    public Optional<AuxT> lookupAuxByIndex(IndexingT i) {
        return lookup(i).flatMap(this::lookupAux);
    }

    //warning: using this constructor fails to associate aux data
    public OutputT lookupOrCreate(IndexingT i) {
        if (index.containsKey(i)) {
            return index.get(i);
        }
        return index.getOrDefault(i, next(i));
    }

    public OutputT lookupOrCreate(IndexingT i, AuxT aux) {
        if (index.containsKey(i)) {
            return index.get(i);
        }
        return index.getOrDefault(i, next(i, aux));
    }

    public static class BySourcePos<OutputT, AuxT extends SourcePositionHolder> extends Indexer<OutputT, SourcePos, AuxT> {
        public OutputT lookupOrCreate(AuxT aux) {
            return super.lookupOrCreate(SourcePos.fromSpoon(aux.getPosition()), aux);
        }

        public OutputT lookupExisting(AuxT aux) {
            return super.lookup(SourcePos.fromSpoon(aux.getPosition())).orElseThrow(() ->
                    new IllegalStateException("Expected auxiliary input to already be indexed: " + aux));
        }

        public BySourcePos(Function<Integer, OutputT> constructor) {
            super(constructor);
        }
        public BySourcePos(Function<Integer, OutputT> constructor, Map<SourcePos, OutputT> precedentMap, int next) {
            super(constructor, precedentMap, next);
        }
    }

    //warning: values in aux not necessarily very informative - could be too big (see unop case)
    public static class BySourceLine extends Indexer<Line, SourcePos, Pair<Procedure, Set<CtElement>>> {
        private Line lookupOrCreateSingleton(SourcePos sp, Procedure procedure, CtElement elem) {
            return super.lookupOrCreate(sp, new Pair<>(procedure, Set.of(elem)));
        }

        public Line lookupOrCreateField(CtFieldAccess<?> fa, Procedure procedure) {
            return lookupOrCreateSingleton(SourcePos.fromFieldAccess(fa), procedure, fa.getVariable());
        }

        public Line lookupOrCreateUnop(CtUnaryOperator<?> unop, Procedure procedure) {
            return lookupOrCreateSingleton(SourcePos.fromUnop(unop), procedure, unop);
        }

        public Line lookupOrCreateBinop(CtBinaryOperator<?> binop, Procedure procedure) {
            return lookupOrCreateSingleton(SourcePos.fromBinop(binop), procedure, binop);
        }

        public Line lookupOrCreateConstr(CtConstructorCall<?> constr, Procedure procedure) {
            return lookupOrCreateSingleton(SourcePos.fromConstr(constr), procedure, constr);
        }

        public Line lookupOrCreateInv(CtInvocation<?> inv, Procedure procedure) {
            return lookupOrCreateSingleton(SourcePos.fromInvocation(inv), procedure, inv);
        }

        /*
        Get a new line whose source position is the entire source position of the passed element
         */
        public Line lookupOrCreateEntire(CtElement elem, Procedure procedure) {
            if (elem.getPosition().isValidPosition()) {
                return lookupOrCreateSingleton(SourcePos.fromSpoon(elem.getPosition()), procedure, elem);
            }
            throw new IllegalArgumentException("Position not defined for passed CtElement: " + elem);
        }

        public BySourceLine() {
            super(Line::new);
        }

        public BySourceLine(Map<SourcePos, Line> precedentMap, int next) {
            super(Line::new, precedentMap, next);
        }
    }

    public Collection<OutputT> outputs() {
        return index.values();
    }

    public Collection<AuxT> auxOutputs() {
        return auxMap.values();
    }

    public Collection<IndexingT> inputs() {
        return index.keySet();
    }

    public HashMap<IndexingT, OutputT> getIndex() {
        //this would normally be unsafe to return without copying but it's only called when analyzers are serializing
        return index;
    }

    public int nextIndex() {
        return next;
    }
}

