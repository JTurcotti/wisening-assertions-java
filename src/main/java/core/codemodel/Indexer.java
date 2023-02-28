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
    private final Map<IndexingT, OutputT> index = new HashMap<>();
    private final Map<OutputT, AuxT> auxMap = new HashMap<>();

    private final Function<Integer, OutputT> constructor;

    public Indexer(Function<Integer, OutputT> constructor) {
        this.constructor = constructor;
    }
    private int next = 0;

    public OutputT next(IndexingT ind, AuxT aux) {
        OutputT n = next(ind);
        if (auxMap.values().stream().anyMatch((v -> v == aux))) {
            //TODO: delete
            throw new IllegalStateException("aux value duplicated: " + aux);
        }
        auxMap.put(n, aux);
        return n;
    }

    //warning: using this method fails to associate aux data
    public OutputT next(IndexingT ind) {
        OutputT n = constructor.apply(next++);
        index.put(ind, n);
        return n;
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

    //warning: using this constructo fails to associate aux data
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

        public BySourcePos(Function<Integer, OutputT> constructor) {
            super(constructor);
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
    }

    public Collection<OutputT> outputs() {
        return index.values();
    }
}

