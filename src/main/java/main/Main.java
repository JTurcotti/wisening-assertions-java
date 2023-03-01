package main;


import analyzer.ProgramAnalyzer;
import analyzer.formulaproviders.TotalProvider;
import core.codemodel.events.Assertion;
import supervisor.ComputationNetwork;

public class Main {
    public static void main(String[] args) {
        ProgramAnalyzer analyzer = new ProgramAnalyzer("src/test/java/a5");
        TotalProvider provider = new TotalProvider(analyzer);
        ComputationNetwork supervisor = new ComputationNetwork(provider);
        supervisor.initializeAssertions(analyzer.getAllAssertions().size());

        int i = 4;

        supervisor.performCycle();
        supervisor.performCycle();
        supervisor.performCycle();
        while (true) {
            System.out.println(supervisor.get(new Assertion(i)));
            supervisor.notifyAssertionPass(new Assertion(i));
            supervisor.performCycle();
        }
        /*ComputationNetwork net = new ComputationNetwork(new ConstantAssertionFreqFormulaProvider(1f));
        net.start();
        while (true) {
            System.out.println(net.executeAssertion(new Assertion(0)));
        }*/
    }
}