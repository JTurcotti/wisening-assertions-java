package main;


import analyzer.ProgramAnalyzer;
import core.codestructure.events.Assertion;
import supervisor.ComputationNetwork;
import core.formula.ConstantAssertionFreqFormulaProvider;

public class Main {
    public static void main(String[] args) {
        ProgramAnalyzer analyzer = new ProgramAnalyzer("src/test/java/a5");
        ComputationNetwork net = new ComputationNetwork(new ConstantAssertionFreqFormulaProvider(1f));
        net.start();
        while (true) {
            System.out.println(net.executeAssertion(new Assertion(0)));
        }
    }
}

record IntPrinter(int i) implements Runnable {
    static Thread asThread(int i) {
        return new Thread(new IntPrinter(i));
    }
    public void run() {
        while (true) {
            System.out.println(i);
        }
    }
}