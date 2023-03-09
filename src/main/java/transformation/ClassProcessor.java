package transformation;

import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.reference.CtTypeReference;

public class ClassProcessor extends AbstractProcessor<CtClass<?>> {
    @Override
    public void process(CtClass<?> element) {
        final CtTypeReference<Integer> fieldType = getFactory().Code().createCtTypeReference(Integer.class);
        final CtField<Integer> intField = getFactory().Core().createField();
        intField.setSimpleName("foo");
        intField.setType(fieldType);

        element.addField(intField);
    }
}
