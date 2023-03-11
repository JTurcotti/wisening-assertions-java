package driver;

import analyzer.ProgramAnalyzer;
import spoon.Launcher;
import transformation.AssertionProcessor;
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

        Util.serializeObject(formulasPath, analyzer.serializeFormulas());
        Util.serializeObject(labelsPath, analyzer.serializeLabels());

        Launcher launcher = new Launcher();
        launcher.addInputResource(srcPath);
        launcher.addInputResource("src/main/java/driver/RuntimeDriver.java");

        launcher.addProcessor(new RuntimeDriverFieldProcessor(precedentResultsPath, resultsPath, formulasPath));
        launcher.addProcessor(new AssertionProcessor(analyzer));

        launcher.setSourceOutputDirectory(tgtPath);
        launcher.run();
    }
}
