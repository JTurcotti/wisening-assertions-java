package com.jturcotti;


import supervisor.ComputationNetwork;
import core.formula.ConstantAssertionFreqFormulaProvider;

public class Main {
    public static void main(String[] args) {
        ComputationNetwork net = new ComputationNetwork(new ConstantAssertionFreqFormulaProvider(1f));
        net.start();
        while (true) {
            System.out.println(net.executeAssertion(ComputationNetwork.testAssertion));
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