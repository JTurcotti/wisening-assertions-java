package supervisor;

import analyzer.ProgramAnalyzer;
import core.codemodel.events.*;
import core.dependencies.*;
import core.formula.ErrorFormulaProvider;
import core.formula.FormulaProvider;
import core.formula.TotalFormulaProvider;
import org.jetbrains.annotations.NotNull;

import java.nio.channels.FileLock;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import static supervisor.Config.*;

interface ComputationRow<Dep extends Dependency, Result extends Event, MsgT> {
    float getVal();
    void notifyDependeesUpdated();
    void notifyNoLongerDependee(ComputationRow<? super Result, ?, ?> formerDependerRow);
    void passMessage(MsgT msg);
}

interface RowProvider<Dep extends Dependency, Result extends Event, MsgT> {
    float get(Result event);
    ComputationRow<Dep, Result, MsgT> getRow(Result event, ComputationRow<? super Result, ?, ?> requester);
    void passMessage(Result event, MsgT msg);
    void passMessageToAll(MsgT msg);
}

public class ComputationNetwork extends Thread implements ExecutionSupervisor {

    private final ComputationCellGroup<None, Pi, BranchTaken> piComputationCells;
    private final ComputationCellGroup<PiOrPhi, Phi, None> phiComputationCells;
    private final ComputationCellGroup<PiOrPhi, Beta, None> betaComputationCells;
    private final ComputationCellGroup<BetaOrEta, Eta, None> etaComputationCells;
    private final ComputationCellGroup<AlphaOrBeta, Alpha, None> alphaComputationCells;
    private final ComputationCellGroup<AlphaOrBetaOrEta, Omega, None> omegaComputationCells;
    private final ComputationCellGroup<None, Line, AssertionPass> lineComputationCells;
    private final ComputationCellGroup<OmegaOrLine, Assertion, None> assertionComputationCells;

    private final ProgramAnalyzer analyzer;

    private void forEach(Consumer<ComputationCellGroup<?, ?, ?>> action) {
        action.accept(piComputationCells);
        action.accept(phiComputationCells);
        action.accept(betaComputationCells);
        action.accept(etaComputationCells);
        action.accept(alphaComputationCells);
        action.accept(omegaComputationCells);
        action.accept(lineComputationCells);
        action.accept(assertionComputationCells);
    }

    private final FormulaProvider<Assertion, Assertion> assertionCorrectnessToFrequencyProvider;

    private final Random randomness = new Random();


    public ComputationNetwork(@NotNull TotalFormulaProvider formulaProvider, ProgramAnalyzer analyzer) {
        piComputationCells =
                new ComputationCellGroup<>(this, PI_COLD_VALUE,
                        true, new ErrorFormulaProvider<>("pi formula"), BranchMessageProcessor::new);
        phiComputationCells =
                new ComputationCellGroup<>(this, PHI_COLD_VALUE,
                        false, formulaProvider.phiFormulaProvider(), NoopMessageProcessor::new);
        betaComputationCells =
                new ComputationCellGroup<>(this, BETA_COLD_VALUE,
                        false, formulaProvider.betaFormulaProvider(), NoopMessageProcessor::new);
        etaComputationCells =
                new ComputationCellGroup<>(this, ETA_COLD_VALUE,
                        false, formulaProvider.etaFormulaProvider(), NoopMessageProcessor::new);
        alphaComputationCells =
                new ComputationCellGroup<>(this, ALPHA_COLD_VALUE,
                        false, formulaProvider.alphaFormulaProvider(), NoopMessageProcessor::new);
        omegaComputationCells =
                new ComputationCellGroup<>(this, OMEGA_COLD_VALUE,
                        false, formulaProvider.omegaFormulaProvider(), NoopMessageProcessor::new);
        lineComputationCells =
                new ComputationCellGroup<>(this, LINE_CORRECTNESS_COLD_VALUE,
                        true, new ErrorFormulaProvider<>("line formula"),
                        line -> new AssertionPassMessageProcessor(
                                formulaProvider.lineUpdateFormulaProvider(), this, line));
        assertionComputationCells =
                new ComputationCellGroup<>(this, ASSERTION_CORRECTNESS_COLD_VALUE,
                        false, formulaProvider.assertionFormulaProvider(), NoopMessageProcessor::new);
        assertionCorrectnessToFrequencyProvider = formulaProvider.assertionCorrectnessToFrequencyProvider();

        this.analyzer = analyzer;
    }

    public void initializeAssertions(int numAssertions) {
        for (int i = 0; i < numAssertions; i++) {
            get(new Assertion(i));
        }
    }

    @SuppressWarnings("unchecked")
    private <Result extends Event> RowProvider<?, Result, ?> getCellGroup(Result event) {
        return switch (event) {
            case Pi ignored -> ((RowProvider<?, Result, ?>) piComputationCells);
            case Phi ignored -> ((RowProvider<?, Result, ?>) phiComputationCells);
            case Beta ignored -> ((RowProvider<?, Result, ?>) betaComputationCells);
            case Eta ignored -> ((RowProvider<?, Result, ?>) etaComputationCells);
            case Alpha ignored -> ((RowProvider<?, Result, ?>) alphaComputationCells);
            case Omega ignored -> ((RowProvider<?, Result, ?>) omegaComputationCells);
            case Line ignored -> ((RowProvider<?, Result, ?>) lineComputationCells);
            case Assertion ignored -> ((RowProvider<?, Result, ?>) assertionComputationCells);
            default -> throw new IllegalStateException("Unexpected Event value: " + event);
        };
    }

    <Result extends Event> ComputationRow<?, Result, ?> getRow(Result event, ComputationRow<? super Result, ?, ?>requester) {
        return getCellGroup(event).getRow(event, requester);
    }

    public float get(Event event) {
        return getCellGroup(event).get(event);
    }

    @Override
    public boolean executeAssertion(Assertion assertion) {
        float assertion_freq = assertionCorrectnessToFrequencyProvider.get(assertion).compute(this::get);
        return randomness.nextFloat() <= assertion_freq;
    }

    @Override
    public void notifyAssertionPass(Assertion assertion) {
        lineComputationCells.passMessageToAll(new AssertionPass(assertion));
    }

    @Override
    public void notifyBranchTaken(Pi branch, boolean direction) {
        piComputationCells.passMessage(branch, new BranchTaken(direction));
    }

    @Override
    public void run() {
        forEach(Thread::start);
        while (!isInterrupted()) {}
        forEach(Thread::interrupt);
    }

    //mostly for debugging purposes
    public void performCycle() {
        forEach(ComputationCellGroup::performCycle);
    }

    public List<Line> topBlamedLines(Assertion a, int n) {
        return analyzer.getAllLines().stream()
                .sorted(Comparator.comparingDouble(line -> -get(new Omega(a, line))))
                .limit(n).toList();
    }

    public float getCoverageForLine(Line l) {
        return assertionComputationCells.streamRows()
                .map(entry -> get(new Omega(entry.getKey(), l)))
                .reduce(0f, Float::max);
    }

    public List<Long> binLineStatistic(int numBins, Function<Line, Float> stat) {
        return IntStream.range(0, numBins).mapToObj(i ->
                analyzer.getAllLines().stream().filter(line ->
                        stat.apply(line) >= (1f * i) / numBins && stat.apply(line) <= (1f * i + 1f) / numBins).count()).toList();
    }

    public String binLineStatString(int numBins, Function<Line, Float> stat) {
        String repr = "[";
        List<Long> stats = binLineStatistic(numBins, stat);
        for (int i = 0; i < numBins; i++) {
            repr += (1f * i) / numBins + ": " + stats.get(i) + " | ";
        }
        return repr + "]";
    }

    @Override
    public String toString() {
        List<Float> assertionCorrectness = IntStream.range(0, analyzer.getAllAssertions().size())
                .mapToObj(i -> get(new Assertion(i))).toList();
        String repr = "Supervisor[" + assertionCorrectness.size() + " assertions: {";
        for (Float f: assertionCorrectness) {
            repr = repr + f + ", ";
        }
        int numBins = 10;
        return repr + "}, line coverages: " +
                binLineStatString(numBins, this::getCoverageForLine) + ", line correctnesses: " +
                binLineStatString(numBins, this::get);
    }
}
