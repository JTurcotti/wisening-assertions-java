package core.codemodel.elements;

import analyzer.ProgramAnalyzer;
import spoon.reflect.reference.CtTypeReference;

import java.util.Optional;

/*
This represents anything that can be read by a call at the point the call is made
In theory, this is a simple concept. Here are the two weird parts:
Field vs Self:
a method tracks exactly which fields it reads, but if it's called from a method
of a different class that precision isn't possible to lift out to the analysis
of the caller; instead we have to treat all those reads as reads from self

Arg vs Variable:
let's say you're in a loop and read a parameter to the enclosing function,
you'll see it as a captured variable, but it's really an arg. So it could
show up represented both ways. This is weird and I should have changed it.

PhiInput.balanceArgsAndVars is written to handle most of this weirdness
 */
public sealed interface PhiInput permits Arg, Field, Self, Variable {
    static Optional<Mutable> asMutable(PhiInput in) {
        return switch (in) {
            case Field f -> Optional.of(f);
            case Variable v -> Optional.of(v);
            default -> Optional.empty();
        };
    }

    static Mutable assertMutable(PhiInput in) {
        if (asMutable(in).isPresent()) {
            return asMutable(in).get();
        }
        throw new IllegalArgumentException("Expected mutable");
    }

    static PhiInput mapToProc(PhiInput in, Procedure p) {
        return in instanceof Arg a ? new Arg(p, a.num()) : in;
    }

    /*
    If `in` is an argument to a procedure other than `p`, convert it to a variable.
    If `in` is a variable that is an argument to `p`, convert it to an arg.
     */
    static PhiInput balanceArgsAndVars(PhiInput in, Procedure p, ProgramAnalyzer analyzer) {
        if (in instanceof Arg arg) {
            Variable asParamVar = analyzer.lookupParamVar(arg);
            if (analyzer.asParamOf(arg, p).isEmpty()) {
                //`in` is an arg but not of this procedure, so convert to a variable
                return asParamVar;
            } else {
                //this takes care of converting some arguments to the right procedure
                return analyzer.asParamOf(asParamVar, p).get();
            }
        }
        if (in instanceof Variable v && analyzer.asParamOf(v, p).isPresent()) {
            //`in` is a variable corresponding to an arg to this procedure, so return as such
            return analyzer.asParamOf(v, p).get();
        }
        return in;
    }

    static PhiInput validateInProc(PhiInput in, Procedure p, ProgramAnalyzer analyzer) {
        //TODO: disable this once it has been passing for a while
        switch (in) {
            case Arg arg -> {
                if (arg.procedure().equals(p) && arg.num() < analyzer.lookupParamVars(p).size() && analyzer.lookupProcedure(p).isMethod()) {
                    return in;
                }
            }
            case Field f -> {
                CtTypeReference<?> fieldType = analyzer.lookupField(f).getDeclaringType().getReference();
                CtTypeReference<?> procType = analyzer.lookupProcedure(p).getDeclaringType().getReference();
                if (procType.isSubtypeOf(fieldType)) {
                    return in;
                }
            }
            case Self s -> {
                return in;
            }
            case Variable v -> {
                if (!analyzer.lookupProcedure(p).isMethod()) {
                    return in;
                }
            }
        }
        throw new IllegalArgumentException("Passed PhiInput " + in + " is not valid for procedure " + p);
    }
}
