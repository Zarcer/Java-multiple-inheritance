package cyber.mi.processor;

import cyber.mi.annotations.RootHierarchy;
import cyber.mi.annotations.UseMultipleInheritance;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.HashSet;
import java.util.Set;

@SupportedAnnotationTypes({
        "cyber.mi.annotations.RootHierarchy",
        "cyber.mi.annotations.UseMultipleInheritance"
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class MultipleInheritanceProcessor extends AbstractProcessor {

    private RootGenerator rootGenerator;
    private UsedClassGenerator usedClassGenerator;
    private final Set<String> generatedInterfaces = new HashSet<>();
    private final Set<String> generatedUsedClasses = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.rootGenerator = new RootGenerator(processingEnv.getFiler(), processingEnv.getMessager());
        this.usedClassGenerator = new UsedClassGenerator(processingEnv.getFiler(), processingEnv.getMessager(), processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // We intentionally handle both annotations regardless of what's in the `annotations` parameter.
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(RootHierarchy.class)) {
            if (!(annotatedElement instanceof TypeElement typeElement)) {
                continue;
            }
            String key = typeElement.getQualifiedName().toString();
            if (generatedInterfaces.add(key)) {
                rootGenerator.generate(typeElement);
            }
        }

        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(UseMultipleInheritance.class)) {
            if (!(annotatedElement instanceof TypeElement typeElement)) {
                continue;
            }
            // Generate only for classes/interfaces; ignore accidental usage elsewhere.
            if (typeElement.getKind() != ElementKind.CLASS && typeElement.getKind() != ElementKind.INTERFACE) {
                continue;
            }
            String key = typeElement.getQualifiedName().toString();
            if (generatedUsedClasses.add(key)) {
                usedClassGenerator.generate(typeElement);
            }
        }

        return true;
    }
}
