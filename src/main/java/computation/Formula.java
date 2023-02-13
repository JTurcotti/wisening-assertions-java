package computation;

import java.util.Set;
import java.util.function.Function;

interface Formula<Dep extends Dependency> {
    Set<Dep> getDeps();

    float compute(Function<Dep, Float> resolveDependencies);
}

interface FormulaProvider<Dep extends Dependency, Result extends Event> {
    Formula<Dep> get(Result event);
}
interface TotalFormulaProvider {
    FormulaProvider<None, Pi> piFormulaProvider();
    FormulaProvider<PiOrPhi, Phi> phiFormulaProvider();
    FormulaProvider<PiOrPhi, Beta> betaFormulaProvider();
    FormulaProvider<BetaOrEta, Eta> etaFormulaProvider();
    FormulaProvider<AlphaOrBeta, Alpha> alphaFormulaProvider();
    FormulaProvider<AlphaOrBetaOrEta, Omega> omegaFormulaProvider();

}