package driver;

import core.codemodel.events.Assertion;
import core.codemodel.events.Pi;
import serializable.SerialResults;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtInvocation;
import supervisor.ComputationNetwork;
import util.Util;

import java.util.Optional;


public class RuntimeDriver {
    private static final ComputationNetwork supervisor;

    public @interface ReplaceDuringProcessing {}
    @ReplaceDuringProcessing
    static final String serialFormulasPath = "to be replaced by spoon Transformer";

    @ReplaceDuringProcessing
    static final String precedentResultsPath = "to be replaced by spoon Transformer";
    @ReplaceDuringProcessing
    static final boolean precedentResultsPresent = false;
    @ReplaceDuringProcessing
    static final String outputPath = "to be replaced by spoon Transformer";
    static final boolean active = false;

    static {
        if (active) {
            Optional<SerialResults> precedentResults = precedentResultsPresent?
                    Optional.of(Util.deserializeObject(precedentResultsPath)):
                    Optional.empty();
            supervisor = new ComputationNetwork(Util.deserializeObject(serialFormulasPath), precedentResults);

            Runtime.getRuntime().addShutdownHook(new Thread(RuntimeDriver::serializeResults));
        } else {
            supervisor = null;
        }
    }

    private static void assertSupervisorInit() {
        if (supervisor == null) {
            throw new IllegalStateException("Error: supervisor not statically initialized early enough");
        }
    }

    public static ComputationNetwork getSupervisor() {
        assertSupervisorInit();
        return supervisor;
    }

    static void serializeResults() {
        assertSupervisorInit();
        Util.serializeObject(outputPath, supervisor.serializeResults());
    }

    public static void notifyAssertionFailure(Assertion assertion) {
        System.out.println("Assertion failure triggered termination");
        serializeResults();
        Runtime.getRuntime().exit(0);
    }

    public static boolean executeAssertion(int i) {
        return getSupervisor().executeAssertion(new Assertion(i));
    }

    public static void notifyAssertionPass(int i) {
        getSupervisor().notifyAssertionPass(new Assertion(i));
    }

    public static void notifyBranchTaken(int i, boolean dir) {
        getSupervisor().notifyBranchTaken(new Pi(i), dir);
    }


    public static CtInvocation<Boolean> getExecuteAssertionInvocation(AbstractProcessor<?> processor, int i) {
        return processor.getFactory().createInvocation(
                Util.getTypeAccess(processor, RuntimeDriver.class),

                processor.getFactory().Executable().createReference(
                        Util.getTypeReference(processor, RuntimeDriver.class),
                        true,
                        processor.getFactory().Type().BOOLEAN_PRIMITIVE, "executeAssertion",
                        processor.getFactory().Type().INTEGER_PRIMITIVE),

                processor.getFactory().createLiteral(i));
    }

    public static CtInvocation<?> getNotifyAssertionPassInvocation(AbstractProcessor<?> processor, int i) {
        return processor.getFactory().createInvocation(
                Util.getTypeAccess(processor, RuntimeDriver.class),

                processor.getFactory().Executable().createReference(
                        Util.getTypeReference(processor, RuntimeDriver.class),
                        true,
                        processor.getFactory().Type().VOID_PRIMITIVE, "notifyAssertionPass",
                        processor.getFactory().Type().INTEGER_PRIMITIVE),

                processor.getFactory().createLiteral(i));
    }
}
