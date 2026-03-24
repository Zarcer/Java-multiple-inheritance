package cyber.mi.processor;

import cyber.mi.annotations.RootHierarchy;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.HashSet;
import java.util.Set;

@SupportedAnnotationTypes("cyber.mi.annotations.RootHierarchy")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class MultipleInheritanceProcessor extends AbstractProcessor {

    private RootGenerator rootGenerator;
    private final Set<String> generatedInterfaces = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.rootGenerator = new RootGenerator(processingEnv.getFiler(), processingEnv.getMessager());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(RootHierarchy.class)) {
            if (!(annotatedElement instanceof TypeElement typeElement)) {
                continue;
            }
            String key = typeElement.getQualifiedName().toString();
            if (generatedInterfaces.add(key)) {
                rootGenerator.generate(typeElement);
            }
        }

        return true;
    }
}
