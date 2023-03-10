package transformation;

import driver.RuntimeDriver;
import spoon.processing.AbstractAnnotationProcessor;
import spoon.reflect.declaration.CtField;

import java.util.Optional;

public class RuntimeDriverFieldProcessor extends AbstractAnnotationProcessor<RuntimeDriver.ReplaceFieldDuringProcessing, CtField<?>> {
    private final String serialFormulasPath;
    private final String precedentResultsPath;
    private final boolean precedentResultsPresent;
    private final String outputPath;

    public RuntimeDriverFieldProcessor(
            Optional<String> precedentResultsPath, String outputPath, String serialFormulasPath) {
        this.serialFormulasPath = serialFormulasPath;
        if (precedentResultsPath.isPresent()) {
            this.precedentResultsPath = precedentResultsPath.get();
            this.precedentResultsPresent = true;
        } else {
            this.precedentResultsPath = "";
            this.precedentResultsPresent = false;
        }
        this.outputPath = outputPath;
    }
    @Override
    public void process(RuntimeDriver.ReplaceFieldDuringProcessing annotation, CtField field) {
        switch (field.getSimpleName()) {
            case "serialFormulasPath" ->
                field.setAssignment(getFactory().createLiteral(this.serialFormulasPath));
            case "precedentResultsPath"->
                field.setAssignment(getFactory().createLiteral(this.precedentResultsPath));
            case "precedentResultsPresent"->
                field.setAssignment(getFactory().createLiteral(this.precedentResultsPresent));
            case "outputPath"->
                field.setAssignment(getFactory().createLiteral(this.outputPath));
            case "active"->
                field.setAssignment(getFactory().createLiteral(true));
        }
    }
}
