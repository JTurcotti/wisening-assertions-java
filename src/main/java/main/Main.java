package main;


import core.codemodel.events.Assertion;
import driver.AnalysisDriver;
import supervisor.ComputationNetwork;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

public class Main {
    static String simplePath = "src/test/java/simple";
    static String andrewPath = "src/test/java/andrew";
    static String srcPath = simplePath;
    public static void testSupervisorCycling() throws InterruptedException {
        ComputationNetwork supervisor = ComputationNetwork.generateFromSourcePath(srcPath);
        supervisor.start();

        Assertion assertion = new Assertion(4);
        Random rand = new Random();

        Thread.sleep(2000);

        new Thread(() -> {
            try {
                long start_t = System.currentTimeMillis();
                long t = start_t;
                int i1 = 0;
                while (true) {
                    if (supervisor.isStable()) {
                        long elapsed = System.currentTimeMillis() - t;
                        System.out.println("Thread round " + i1++ + ": " + elapsed + " elapsed. Total: " + (t - start_t));

                        Thread.sleep(500);

                        t = System.currentTimeMillis();
                        supervisor.notifyAssertionPass(assertion);
                    }
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException("interrupted");
            }
        }).start();
        /*ComputationNetwork net = new ComputationNetwork(new ConstantAssertionFreqFormulaProvider(1f));
        net.start();
        while (true) {
            System.out.println(net.executeAssertion(new Assertion(0)));
        }*/
    }
    static class Box<T> {
        volatile T val;
    }

    public static void countIterations(long time, int others, int otherPriority, int mainPriority) throws InterruptedException {
        Box<Boolean> run = new Box<>();
        run.val = true;
        List<Thread> otherThreads = IntStream.range(0, others).mapToObj(ignored -> new Thread(() -> {
            while (run.val) {}
        })).toList();
        otherThreads.forEach(t -> t.setPriority(otherPriority));
        final Box<Integer> count = new Box();
        count.val = 0;
        Thread main = new Thread(() -> {
            while (run.val) {count.val++;}
        });
        main.setPriority(mainPriority);

        otherThreads.forEach(Thread::start);
        main.start();

        Thread.sleep(time);

        run.val = false;

        System.out.println(others + " other threads at priority " + otherPriority + " allowed main thread at priority " + mainPriority + " to execute " + count.val + " times");
    }

    public static void testThreadPriority() throws InterruptedException {
        long time = 1;
        int others = 0;
        countIterations(time, others, Thread.NORM_PRIORITY, Thread.NORM_PRIORITY);
        countIterations(time, others, Thread.MIN_PRIORITY, Thread.NORM_PRIORITY);
        countIterations(time, others, Thread.NORM_PRIORITY, Thread.MAX_PRIORITY);
        countIterations(time, others, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);

        others = 10;
        countIterations(time, others, Thread.NORM_PRIORITY, Thread.NORM_PRIORITY);
        countIterations(time, others, Thread.MIN_PRIORITY, Thread.NORM_PRIORITY);
        countIterations(time, others, Thread.NORM_PRIORITY, Thread.MAX_PRIORITY);
        countIterations(time, others, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);

        others = 100;
        countIterations(time, others, Thread.NORM_PRIORITY, Thread.NORM_PRIORITY);
        countIterations(time, others, Thread.MIN_PRIORITY, Thread.NORM_PRIORITY);
        countIterations(time, others, Thread.NORM_PRIORITY, Thread.MAX_PRIORITY);
        countIterations(time, others, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);

        others = 1000;
        countIterations(time, others, Thread.NORM_PRIORITY, Thread.NORM_PRIORITY);
        countIterations(time, others, Thread.MIN_PRIORITY, Thread.NORM_PRIORITY);
        countIterations(time, others, Thread.NORM_PRIORITY, Thread.MAX_PRIORITY);
        countIterations(time, others, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);
    }

    public static void testProcessor() {
        AnalysisDriver.run("src/test/java/andrew",
                Optional.empty(),
                Optional.empty(),
                "src/main/java",
                "target/aux/results",
                "target/aux/formulas",
                "target/aux/labels");
    }
    public static void main(String[] args) throws InterruptedException {
        //testSupervisorCycling();
        testProcessor();
        //testThreadPriority();
    }
}

/*
Supervisor[10 assertions: {5.344878E-6, 0.019227445, 3.4435618E-25, 0.010850694, 0.99042076, 0.007562376, 0.49521038, 4.461556E-4, 2.0551997E-8, 0.123802595, }
line coverages: [0.0: 1610 | 0.1: 79 | 0.2: 80 | 0.3: 29 | 0.4: 43 | 0.5: 19 | 0.6: 8 | 0.7: 6 | 0.8: 31 | 0.9: 111 | ]
line correctnesses: [0.0: 0 | 0.1: 0 | 0.2: 0 | 0.3: 0 | 0.4: 1915 | 0.5: 1915 | 0.6: 0 | 0.7: 0 | 0.8: 1 | 0.9: 95 | ]
 */

/*
Alpha on, run mode:
Elapsed: 231
Elapsed: 567
Elapsed: 17751
Elapsed: 3216
Elapsed: 1202
Elapsed: 1909
Elapsed: 350
Elapsed: 197
Elapsed: 110
Elapsed: 80
Elapsed: 31
Elapsed: 42
Elapsed: 27
Elapsed: 29
Elapsed: 29
Elapsed: 27
Elapsed: 26
Elapsed: 25
Elapsed: 21
Supervisor[10 assertions: {3.7021782E-6, 0.015821781, 2.8014662E-25, 0.010850694, 0.99879754, 0.007562376, 0.49939877, 2.782877E-4, 4.7447823E-9, 0.12484969, },
line coverages: [0.0: 1609 | 0.1: 80 | 0.2: 72 | 0.3: 37 | 0.4: 43 | 0.5: 19 | 0.6: 8 | 0.7: 6 | 0.8: 31 | 0.9: 111 | ],
line correctnesses: [0.0: 0 | 0.1: 0 | 0.2: 0 | 0.3: 0 | 0.4: 1915 | 0.5: 1915 | 0.6: 0 | 0.7: 0 | 0.8: 0 | 0.9: 96 | ]


Alpha off, run mode:
Elapsed: 234
Elapsed: 545
Elapsed: 134
Elapsed: 106
Elapsed: 143
Elapsed: 185
Elapsed: 163
Elapsed: 300
Elapsed: 41
Elapsed: 30
Elapsed: 23
Elapsed: 22
Elapsed: 23
Elapsed: 20
Elapsed: 16
Elapsed: 15
Elapsed: 19
Elapsed: 19
Elapsed: 16
Elapsed: 16
Elapsed: 15
Elapsed: 12
Elapsed: 13
Elapsed: 12
Elapsed: 13
Elapsed: 11
Elapsed: 10
Elapsed: 12
Supervisor[10 assertions: {2.4414062E-4, 0.125, 1.8081222E-23, 0.010850694, 0.99879754, 0.015455961, 0.49939877, 9.765625E-4, 1.1920929E-7, 0.12484969, },
line coverages: [0.0: 1650 | 0.1: 86 | 0.2: 44 | 0.3: 41 | 0.4: 26 | 0.5: 20 | 0.6: 4 | 0.7: 15 | 0.8: 22 | 0.9: 111 | ],
line correctnesses: [0.0: 0 | 0.1: 0 | 0.2: 0 | 0.3: 0 | 0.4: 1915 | 0.5: 1915 | 0.6: 0 | 0.7: 0 | 0.8: 0 | 0.9: 96 | ]

 */