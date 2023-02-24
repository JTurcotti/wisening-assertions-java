package main;


import analyzer.ProgramAnalyzer;

public class Main {
    public static void main(String[] args) {
        ProgramAnalyzer analyzer = new ProgramAnalyzer("src/test/java/a5");
        /*ComputationNetwork net = new ComputationNetwork(new ConstantAssertionFreqFormulaProvider(1f));
        net.start();
        while (true) {
            System.out.println(net.executeAssertion(new Assertion(0)));
        }*/
    }
}