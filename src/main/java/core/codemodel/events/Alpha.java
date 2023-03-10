package core.codemodel.events;

import analyzer.ProgramAnalyzer;
import core.codemodel.elements.PhiInput;
import core.codemodel.elements.Procedure;
import core.dependencies.AlphaOrBeta;
import core.dependencies.AlphaOrBetaOrEta;

public record Alpha(Line line, Procedure procedure, PhiInput input) implements Event, AlphaOrBeta, AlphaOrBetaOrEta, ComputedEvent {
    public Alpha(Line line, Procedure procedure, PhiInput input) {
        ProgramAnalyzer.availableInstance.ifPresent(analyzer -> PhiInput.validateInProc(input, procedure, analyzer));
        this.line = line;
        this.procedure = procedure;
        this.input = input;
    }
}
