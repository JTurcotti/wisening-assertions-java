package transformation;

import analyzer.ProgramAnalyzer;
import core.codemodel.events.Pi;
import driver.RuntimeDriver;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtStatement;
import util.Util;

public class IfProcessor extends AbstractProcessor<CtIf> {
    private final ProgramAnalyzer parentAnalyzer;

    public IfProcessor(ProgramAnalyzer parentAnalyzer) {
        this.parentAnalyzer = parentAnalyzer;
    }

    private void addInvToBlock(CtBlock<?> block, int i, boolean dir) {
        block.addStatement(0, RuntimeDriver.getNotifyBranchTakenInvocation(this, i, dir));
    }

    @Override
    public boolean isToBeProcessed(CtIf candidate) {
        //TODO: get rid of this when Catch is handled
        return Util.inSupportedContext(candidate);
    }

    @Override
    public void process(CtIf ifStmt) {
        Pi pi = parentAnalyzer.indexOfIfBranch(ifStmt);
        if (ifStmt.getThenStatement() == null) {
            throw new IllegalStateException("Unexepected lack of 'then' statement");
        }

        if (ifStmt.getThenStatement() instanceof CtBlock<?> block) {
            addInvToBlock(block, pi.num(), true);
        } else {
            CtStatement thenStmt = ifStmt.getThenStatement();
            thenStmt.delete();
            CtBlock block = getFactory().createCtBlock(thenStmt);
            addInvToBlock(block, pi.num(), true);
            ifStmt.setThenStatement(block);
        }

        if (ifStmt.getElseStatement() == null) {
            CtBlock block = getFactory().createBlock();
            addInvToBlock(block, pi.num(), false);
            ifStmt.setElseStatement(block);
        } else if (ifStmt.getElseStatement() instanceof CtBlock<?> block) {
            addInvToBlock(block, pi.num(), false);
        } else {
            CtStatement elseStmt = ifStmt.getElseStatement();
            elseStmt.delete();
            CtBlock block = getFactory().createCtBlock(elseStmt);
            addInvToBlock(block, pi.num(), false);
            ifStmt.setElseStatement(block);
        }
    }

}
