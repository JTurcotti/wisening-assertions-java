package main;


import analyzer.ProgramAnalyzer;
import analyzer.formulaproviders.TotalProvider;
import core.codemodel.events.Assertion;
import supervisor.ComputationNetwork;

public class Main {
    public static void main(String[] args) {
        ProgramAnalyzer analyzer = new ProgramAnalyzer("src/test/java/a5");
        TotalProvider provider = new TotalProvider(analyzer);
        ComputationNetwork supervisor = new ComputationNetwork(provider, analyzer);
        supervisor.initializeAssertions(analyzer.getAllAssertions().size());

        int i = 4;

        while (true) {
            System.out.println(supervisor);
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

/*
Supervisor[10 assertions: {5.344878E-6, 0.019227445, 3.4435618E-25, 0.010850694, 0.99042076, 0.007562376, 0.49521038, 4.461556E-4, 2.0551997E-8, 0.123802595, }
line coverages: [0.0: 1610 | 0.1: 79 | 0.2: 80 | 0.3: 29 | 0.4: 43 | 0.5: 19 | 0.6: 8 | 0.7: 6 | 0.8: 31 | 0.9: 111 | ]
line correctnesses: [0.0: 0 | 0.1: 0 | 0.2: 0 | 0.3: 0 | 0.4: 1915 | 0.5: 1915 | 0.6: 0 | 0.7: 0 | 0.8: 1 | 0.9: 95 | ]


 */