package transformation;

import analyzer.ProgramAnalyzer;
import core.codemodel.Indexer;
import core.codemodel.events.Assertion;
import core.dependencies.None;
import driver.RuntimeDriver;
import serializable.SerialLabels;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtStatement;

public class AssertionProcessor extends AbstractProcessor<CtAssert<?>> {
    private final ProgramAnalyzer parentAnalyzer;

    public AssertionProcessor(ProgramAnalyzer parentAnalyzer) {
        this.parentAnalyzer = parentAnalyzer;
    }
    @Override
    public void process(CtAssert<?> ctAssert) {
        Assertion assertion = parentAnalyzer.indexOfAssertion(ctAssert);
        CtIf replacement = getFactory().createIf();
        ctAssert.replace(replacement);
        replacement.setCondition(RuntimeDriver.getExecuteAssertionInvocation(this, assertion.num()));
        replacement.setThenStatement(ctAssert);
    }
}
