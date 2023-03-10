package supervisor;

import analyzer.ProgramAnalyzer;
import analyzer.formulaproviders.TotalProvider;
import core.codemodel.elements.Procedure;
import core.codemodel.events.*;
import core.dependencies.*;
import core.formula.ErrorFormulaProvider;
import core.formula.FormulaProvider;
import core.formula.TotalFormulaProvider;
import org.jetbrains.annotations.NotNull;
import serializable.SerialResults;
import spoon.reflect.declaration.CtElement;
import util.Pair;
import util.Util;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

    //only used for pretty printing
    public ProgramAnalyzer analyzer;

    private Stream<ComputationCellGroup<? extends Dependency, ? extends Event, ?>> streamCellGroups() {
        return Stream.of(
                piComputationCells,
                phiComputationCells,
                betaComputationCells,
                etaComputationCells,
                alphaComputationCells,
                omegaComputationCells,
                lineComputationCells,
                assertionComputationCells);
    }

    private final FormulaProvider<Assertion, Assertion> assertionCorrectnessToFrequencyProvider;

    private final Random randomness = new Random();


    public ComputationNetwork(@NotNull TotalFormulaProvider formulaProvider) {
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
    }

    public void initializeAssertions(int numAssertions) {
        for (int i = 0; i < numAssertions; i++) {
            get(new Assertion(i));
        }
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            performCycle();
        }
    }

    public void initializeAllAssertions(ProgramAnalyzer analyzer) {
        initializeAssertions(analyzer.numAssertions());
    }

    public static ComputationNetwork generateFromSourcePath(String sourcePath) {
        ProgramAnalyzer analyzer = new ProgramAnalyzer(sourcePath);
        TotalProvider provider = new TotalProvider(analyzer);
        ComputationNetwork network = new ComputationNetwork(provider);
        network.analyzer = analyzer;
        network.initializeAssertions(analyzer.getAllAssertions().size());
        return network;
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
        streamCellGroups().forEach(Thread::start);
        while (!isInterrupted()) {}
        streamCellGroups().forEach(Thread::interrupt);
    }

    //mostly for debugging purposes
    public void performCycle() {
        long start = System.currentTimeMillis();
        streamCellGroups().forEach(ComputationCellGroup::performCycle);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Elapsed: " + elapsed);
    }

    public List<Pair<Pair<Procedure, Set<CtElement>>, Float>> topBlamedLines(Assertion a, int n, ProgramAnalyzer analyzer) {
        return analyzer.getAllLines().stream()
                .sorted(Comparator.comparingDouble(line -> -get(new Omega(a, line))))
                .limit(n)
                .map(line -> new Pair<>(analyzer.lineIndexer.lookupAux(line).get(), get(new Omega(a, line))))
                .toList();
    }

    public List<Pair<Pair<Procedure, Set<CtElement>>, Float>> getBlamedLines(Assertion a, ProgramAnalyzer analyzer) {
        return analyzer.getAllLines().stream()
                .sorted(Comparator.comparingDouble(line -> -get(new Omega(a, line))))
                .filter(line -> get(new Omega(a, line)) > COMPUTATION_CELL_FRESH_VAL_TRESHOLD)
                .map(line -> new Pair<>(analyzer.lineIndexer.lookupAux(line).get(), get(new Omega(a, line))))
                .toList();
    }

    public long countBlamedLines(Assertion a, ProgramAnalyzer analyzer) {
        return analyzer.getAllLines().stream()
                .filter(line -> get(new Omega(a, line)) > COMPUTATION_CELL_FRESH_VAL_TRESHOLD)
                .count();
    }

    public float getCoverageForLine(Line l) {
        return assertionComputationCells.streamRows()
                .map(entry -> get(new Omega(entry.getKey(), l)))
                .reduce(0f, Float::max);
    }

    public long numActive() {
        return streamCellGroups().mapToLong(ComputationCellGroup::numActive).sum();
    }

    public boolean isStable() {
        //could be made more performant by early termination
        return numActive() == 0;
    }

    public SerialResults serializeResults() {
        //we cast to hashmap because the underlying implementation of Collectors.toMap uses it and I want to ensure
        //serialization goes right so SerialResults only accepts HashMaps
        HashMap<Event, Float> data = (HashMap<Event, Float>) streamCellGroups()
                .flatMap(ComputationCellGroup::streamRows)
                .collect(Collectors.toMap(entry -> (Event) entry.getKey(), entry -> entry.getValue().getVal()));
        return new SerialResults(data);
    }

    @Override
    public String toString() {
        if (analyzer != null) {
            List<Float> assertionCorrectness = IntStream.range(0, analyzer.getAllAssertions().size())
                    .mapToObj(i -> get(new Assertion(i))).toList();
            String repr = "Supervisor[" + numActive() + " active, " + assertionCorrectness.size() + " assertions: {";
            for (Float f : assertionCorrectness) {
                repr = repr + f + ", ";
            }
            return repr + "}, line coverages: " +
                    Util.binStatisticString(BINS_FOR_DISPLAY, this::getCoverageForLine, analyzer.getAllLines()) + ", line correctnesses: " +
                    Util.binStatisticString(BINS_FOR_DISPLAY, this::get, analyzer.getAllLines());
        }
        return "Supervisor";
    }
}
