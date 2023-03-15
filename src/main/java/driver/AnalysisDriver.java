package driver;

import analyzer.ProgramAnalyzer;
import spoon.Launcher;
import transformation.AssertionProcessor;
import transformation.IfProcessor;
import transformation.RuntimeDriverFieldProcessor;
import util.Util;

import java.util.Optional;

public class AnalysisDriver {
    public static void run(String srcPath, Optional<String> precedentLabelsPath,
                    Optional<String> precedentResultsPath,
                    String tgtPath,
                    String resultsPath,
                    String formulasPath,
                    String labelsPath) {

        ProgramAnalyzer analyzer = new ProgramAnalyzer(srcPath, precedentLabelsPath.map(Util::deserializeObject));

        System.out.println("AnalysisDriver: done analyzing, beginning serialization");

        Util.serializeObject(formulasPath, analyzer.serializeFormulas());
        Util.serializeObject(labelsPath, analyzer.serializeLabels());

        System.out.println("AnalysisDriver: done serializing formulas and labels, beginning instrumentation");

        Launcher launcher = new Launcher();
        launcher.addInputResource(srcPath);
        launcher.addInputResource("src/main/java/driver/RuntimeDriver.java");

        launcher.addProcessor(new RuntimeDriverFieldProcessor(precedentResultsPath, resultsPath, formulasPath));
        launcher.addProcessor(new AssertionProcessor(analyzer));
        launcher.addProcessor(new IfProcessor(analyzer));
        //TODO: process loops, switches, and other control flow branchTaken notification

        launcher.setSourceOutputDirectory(tgtPath);
        launcher.run();

        System.out.println("AnalysisDriver: done instrumenting");
    }
}
