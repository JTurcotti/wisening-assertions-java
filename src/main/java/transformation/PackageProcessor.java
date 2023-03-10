package transformation;

import spoon.processing.AbstractProcessor;
import spoon.reflect.CtModelImpl;
import spoon.reflect.declaration.CtPackage;

public class PackageProcessor extends AbstractProcessor<CtPackage> {


    @Override
    public void process(CtPackage ctPackage) {
        if (ctPackage instanceof CtModelImpl.CtRootPackage) {
            ctPackage.addType()
        }
    }
}
