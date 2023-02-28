package main;


import analyzer.ProgramAnalyzer;
import analyzer.formulaproviders.TotalProvider;
import core.codemodel.events.Assertion;
import supervisor.ComputationNetwork;

public class Main {
    public static void main(String[] args) {
        ProgramAnalyzer analyzer = new ProgramAnalyzer("src/test/java/simple/controlflow");
        TotalProvider provider = new TotalProvider(analyzer);
        ComputationNetwork supervisor = new ComputationNetwork(provider);
        supervisor.executeAssertion(new Assertion(0));
        while (true) {
            supervisor.performCycle();
        }
        /*ComputationNetwork net = new ComputationNetwork(new ConstantAssertionFreqFormulaProvider(1f));
        net.start();
        while (true) {
            System.out.println(net.executeAssertion(new Assertion(0)));
        }*/
    }
}