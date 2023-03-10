package transformation;

import driver.RuntimeDriver;
import spoon.processing.AbstractProcessor;
import spoon.reflect.CtModelImpl;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtPackage;

public class PackageProcessor extends AbstractProcessor<CtPackage> {

    @Override
    public boolean isToBeProcessed(CtPackage ctPackage) {
        return (ctPackage instanceof CtModelImpl.CtRootPackage);
    }

    @Override
    public void process(CtPackage ctPackage) {
        CtClass<RuntimeDriver> driverClass = getFactory().Class().get(RuntimeDriver.class);
        driverClass.delete();
        ctPackage.addType(driverClass);
    }
}
