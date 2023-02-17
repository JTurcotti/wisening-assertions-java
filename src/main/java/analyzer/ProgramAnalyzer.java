package analyzer;

import spoon.Launcher;
import spoon.reflect.CtModel;

public class ProgramAnalyzer {
    private final CtModel model;

    public ProgramAnalyzer(String sourcePath) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(sourcePath);
        launcher.getEnvironment().setComplianceLevel(18);
        launcher.buildModel();
        model = launcher.getModel();
    }

}
