package transformation;

import analyzer.ProgramAnalyzer;
import core.codemodel.events.Assertion;
import driver.RuntimeDriver;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtIf;

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
        CtBlock<?> thenBlock = getFactory().createBlock();
        thenBlock.addStatement(ctAssert);
        thenBlock.addStatement(RuntimeDriver.getNotifyAssertionPassInvocation(this, assertion.num()));
        replacement.setThenStatement(thenBlock);
    }
}
