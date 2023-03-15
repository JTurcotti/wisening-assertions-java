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

public class RuntimeDriver {
    static boolean initialized = false;
    private static ComputationNetwork supervisor;


    static {
        RuntimeDriver.initializeSupervisor();
    }

    static void initializeSupervisor() {
        if (!initialized) {
            if (RuntimeDriverParams.active) {
                Optional<SerialResults> precedentResults = RuntimeDriverParams.precedentResultsPresent?
                        Optional.of(Util.deserializeObject(RuntimeDriverParams.precedentResultsPath)):
                        Optional.empty();
                SerialFormulas formulas = Util.deserializeObject(RuntimeDriverParams.serialFormulasPath);
                supervisor = new ComputationNetwork(formulas, precedentResults, Optional.empty());

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
        if (supervisor == null) {
            System.out.println("WARNING: serializeResults called before supervisor initialized");
        }
        initializeSupervisor();
        SerialResults results = supervisor.serializeResults();
        //System.out.println(results);
        Util.serializeObject(RuntimeDriverParams.outputPath, results);
    }

    public static void notifyAssertionFailure(Assertion assertion) {
        System.out.println("Assertion failure triggered termination");
        serializeResults();
        Runtime.getRuntime().exit(0);
    }

    public static boolean executeAssertion(int i) {
        if (RuntimeDriverParams.active) {
            return getSupervisor().executeAssertion(new Assertion(i));
        } else {
            return true;
        }
    }

    public static void notifyAssertionPass(int i) {
        if (RuntimeDriverParams.active) {
            getSupervisor().notifyAssertionPass(new Assertion(i));
        }
    }

    public static void notifyBranchTaken(int i, boolean dir) {
        getSupervisor().notifyBranchTaken(new Pi(i), dir);
    }

    public static CtClass<?> getCtClass(AbstractProcessor<?> processor) {
        return processor.getFactory().Class().get(RuntimeDriverParams.class);
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
                Util.getTypeAccess(processor, RuntimeDriverParams.class),

                processor.getFactory().Executable().createReference(
                        getCtTypeRef(processor),
                        true,
                        processor.getFactory().Type().VOID_PRIMITIVE, "notifyAssertionPass",
                        processor.getFactory().Type().INTEGER_PRIMITIVE),

                processor.getFactory().createLiteral(i));
    }

    public static CtInvocation<?> getNotifyBranchTakenInvocation(AbstractProcessor<?> processor, int i, boolean dir) {
        return processor.getFactory().createInvocation(
                Util.getTypeAccess(processor, RuntimeDriverParams.class),

                processor.getFactory().Executable().createReference(
                        getCtTypeRef(processor),
                        true,
                        processor.getFactory().Type().VOID_PRIMITIVE, "notifyBranchTaken",
                        processor.getFactory().Type().INTEGER_PRIMITIVE,
                        processor.getFactory().Type().BOOLEAN_PRIMITIVE),

                processor.getFactory().createLiteral(i),
                processor.getFactory().createLiteral(dir));
    }
}
