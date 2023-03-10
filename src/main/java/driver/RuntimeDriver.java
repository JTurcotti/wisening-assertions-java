package driver;

import core.codemodel.events.Assertion;
import core.codemodel.events.Pi;
import serializable.SerialFormulas;
import serializable.SerialResults;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.reference.CtTypeReference;
import supervisor.ComputationNetwork;
import util.Util;

import java.util.Optional;


@RuntimeDriver.IsRuntimeDriver
public class RuntimeDriver {
    public @interface IsRuntimeDriver {}

    private static ComputationNetwork supervisor;

    public @interface ReplaceFieldDuringProcessing {}
    @ReplaceFieldDuringProcessing
    static final String serialFormulasPath = "to be replaced by spoon Transformer";

    @ReplaceFieldDuringProcessing
    static final String precedentResultsPath = "to be replaced by spoon Transformer";
    @ReplaceFieldDuringProcessing
    static final boolean precedentResultsPresent = false;
    @ReplaceFieldDuringProcessing
    static final String outputPath = "to be replaced by spoon Transformer";
    @ReplaceFieldDuringProcessing
    static final boolean active = false;

    static boolean initialized = false;

    static {
        initializeSupervisor();
    }

    private static void initializeSupervisor() {
        if (!initialized) {
            if (active) {
                Optional<SerialResults> precedentResults = precedentResultsPresent?
                        Optional.of(Util.deserializeObject(precedentResultsPath)):
                        Optional.empty();
                SerialFormulas formulas = Util.deserializeObject(serialFormulasPath);
                supervisor = new ComputationNetwork(formulas, precedentResults);

                Runtime.getRuntime().addShutdownHook(new Thread(RuntimeDriver::serializeResults));
                supervisor.initializeAllAssertions(formulas.getAllAssertions());
                supervisor.start();
            } else {
                supervisor = null;
            }
            initialized = true;
        }
    }

    public static ComputationNetwork getSupervisor() {
        initializeSupervisor();
        return supervisor;
    }

    static void serializeResults() {
        initializeSupervisor();
        SerialResults results = supervisor.serializeResults();
        System.out.println(results);
        Util.serializeObject(outputPath, results);
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

    public static CtClass<?> getCtClass(AbstractProcessor<?> processor) {
        return processor.getFactory().Class().get(RuntimeDriver.class);
    }

    public static CtTypeReference<?> getCtTypeRef(AbstractProcessor<?> processor) {
        return getCtClass(processor).getReference();
    }

    public static CtTypeAccess<?> getCtTypeAccess(AbstractProcessor<?> processor) {
        return processor.getFactory().createTypeAccess(getCtTypeRef(processor));
    }


    public static CtInvocation<Boolean> getExecuteAssertionInvocation(AbstractProcessor<?> processor, int i) {
        return processor.getFactory().createInvocation(
                getCtTypeAccess(processor),

                processor.getFactory().Executable().createReference(
                        getCtTypeRef(processor),
                        true,
                        processor.getFactory().Type().BOOLEAN_PRIMITIVE, "executeAssertion",
                        processor.getFactory().Type().INTEGER_PRIMITIVE),

                processor.getFactory().createLiteral(i));
    }

    public static CtInvocation<?> getNotifyAssertionPassInvocation(AbstractProcessor<?> processor, int i) {
        return processor.getFactory().createInvocation(
                Util.getTypeAccess(processor, RuntimeDriver.class),

                processor.getFactory().Executable().createReference(
                        getCtTypeRef(processor),
                        true,
                        processor.getFactory().Type().VOID_PRIMITIVE, "notifyAssertionPass",
                        processor.getFactory().Type().INTEGER_PRIMITIVE),

                processor.getFactory().createLiteral(i));
    }
}
