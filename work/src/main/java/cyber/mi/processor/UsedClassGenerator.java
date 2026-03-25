package cyber.mi.processor;

import cyber.mi.annotations.UseMultipleInheritance;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates an adapter class for a marker type annotated with {@link UseMultipleInheritance}.
 *
 * <p>The generated class:</p>
 * <ul>
 *     <li>extends the generated {@code root()Root} abstract class</li>
 *     <li>accepts {@code targets()} instances via a constructor and stores them into the MRO list</li>
 *     <li>overrides root interface abstract methods and delegates the call to the first target in MRO</li>
 * </ul>
 */
public final class UsedClassGenerator {
    private static final String USE_MI_FQN = "cyber.mi.annotations.UseMultipleInheritance";

    private final Filer filer;
    private final Messager messager;
    private final ProcessingEnvironment processingEnv;

    public UsedClassGenerator(Filer filer, Messager messager, ProcessingEnvironment processingEnv) {
        this.filer = filer;
        this.messager = messager;
        this.processingEnv = processingEnv;
    }

    public void generate(TypeElement markerType) {
        if (markerType.getKind() != ElementKind.CLASS && markerType.getKind() != ElementKind.INTERFACE) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@UseMultipleInheritance can only be applied to a class or interface",
                    markerType
            );
            return;
        }

        PackageElement packageElement = findPackage(markerType);
        String packageName = packageElement.isUnnamed() ? "" : packageElement.getQualifiedName().toString();

        Optional<UseMiValues> valuesOptional = parseUseMiValues(markerType);
        if (valuesOptional.isEmpty()) {
            return;
        }
        UseMiValues values = valuesOptional.get();

        Element rootElement = processingEnv.getTypeUtils().asElement(values.rootType);
        if (!(rootElement instanceof TypeElement rootInterface)) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Cannot resolve @UseMultipleInheritance.root() type",
                    markerType
            );
            return;
        }
        if (rootInterface.getKind() != ElementKind.INTERFACE) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "useMultipleInheritance.root() must be an interface",
                    rootInterface
            );
            return;
        }

        String rootInterfaceSimpleName = rootInterface.getSimpleName().toString();
        PackageElement rootPackageElement = findPackage(rootInterface);
        String rootPackageName = rootPackageElement.isUnnamed() ? "" : rootPackageElement.getQualifiedName().toString();

        String rootRootSimpleName = rootInterfaceSimpleName + "Root";
        String rootRootFqn = rootPackageName.isEmpty() ? rootRootSimpleName : rootPackageName + "." + rootRootSimpleName;

        if (values.targetTypes.isEmpty()) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@UseMultipleInheritance.targets() must not be empty",
                    markerType
            );
            return;
        }

        String markerSimpleName = markerType.getSimpleName().toString();
        String generatedSimpleName = markerSimpleName + "MI";
        String generatedFqn = packageName.isEmpty() ? generatedSimpleName : packageName + "." + generatedSimpleName;

        List<ExecutableElement> rootMethods = collectRootMethods(rootInterface);
        try {
            JavaFileObject sourceFile = filer.createSourceFile(generatedFqn, markerType);
            try (Writer writer = sourceFile.openWriter()) {
                writer.write(buildClassSource(
                        packageName,
                        generatedSimpleName,
                        rootRootFqn,
                        rootMethods,
                        values.targetTypes
                ));
            }
        } catch (IOException ex) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to generate used class " + generatedFqn + ": " + ex.getMessage(),
                    markerType
            );
        }
    }

    private Optional<UseMiValues> parseUseMiValues(TypeElement markerType) {
        AnnotationMirror useMiMirror = null;
        for (AnnotationMirror annotationMirror : markerType.getAnnotationMirrors()) {
            Element annotationElement = annotationMirror.getAnnotationType().asElement();
            if (!(annotationElement instanceof TypeElement annotationType)) {
                continue;
            }
            if (USE_MI_FQN.equals(annotationType.getQualifiedName().toString())) {
                useMiMirror = annotationMirror;
                break;
            }
        }
        if (useMiMirror == null) {
            return Optional.empty();
        }

        TypeMirror rootType = null;
        List<TypeMirror> targetTypes = new ArrayList<>();

        Map<? extends ExecutableElement, ? extends AnnotationValue> values =
                processingEnv.getElementUtils().getElementValuesWithDefaults(useMiMirror);

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
            String key = entry.getKey().getSimpleName().toString();
            Object rawValue = entry.getValue().getValue();
            if ("root".equals(key) && rawValue instanceof TypeMirror typeMirror) {
                rootType = typeMirror;
            } else if ("targets".equals(key) && rawValue instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof AnnotationValue annotationValue) {
                        Object nestedValue = annotationValue.getValue();
                        if (nestedValue instanceof TypeMirror targetTypeMirror) {
                            targetTypes.add(targetTypeMirror);
                        }
                    }
                }
            }
        }

        if (rootType == null) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Cannot read @UseMultipleInheritance.root()",
                    markerType
            );
            return Optional.empty();
        }
        return Optional.of(new UseMiValues(rootType, Collections.unmodifiableList(targetTypes)));
    }

    private PackageElement findPackage(Element element) {
        Element cursor = element;
        while (cursor != null && cursor.getKind() != ElementKind.PACKAGE) {
            cursor = cursor.getEnclosingElement();
        }
        return (PackageElement) cursor;
    }

    private List<ExecutableElement> collectRootMethods(TypeElement rootInterface) {
        List<ExecutableElement> methods = ElementFilter.methodsIn(rootInterface.getEnclosedElements());
        List<ExecutableElement> result = new ArrayList<>();
        for (ExecutableElement method : methods) {
            Set<Modifier> modifiers = method.getModifiers();
            if (modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.PRIVATE)) {
                continue;
            }
            if (!modifiers.contains(Modifier.ABSTRACT)) {
                continue;
            }
            result.add(method);
        }
        return result;
    }

    private String buildClassSource(
            String packageName,
            String generatedSimpleName,
            String rootRootFqn,
            List<ExecutableElement> rootMethods,
            List<TypeMirror> targets
    ) {
        StringBuilder sb = new StringBuilder(2048);
        if (!packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }

        sb.append("public class ").append(generatedSimpleName)
                .append(" extends ").append(rootRootFqn).append(" {\n\n");

        // Constructor: set the MRO list in the provided order.
        sb.append("    public ").append(generatedSimpleName).append("(");

        for (int i = 0; i < targets.size(); i++) {
            String typeName = Objects.requireNonNull(targets.get(i)).toString();
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(typeName).append(" target").append(i);
        }
        sb.append(") {\n");
        sb.append("        this.setMro(java.util.List.<").append(rootRootFqn).append(">of(");
        for (int i = 0; i < targets.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("target").append(i);
        }
        sb.append("));\n");
        sb.append("    }\n\n");

        // Delegate each root abstract method to the first target in MRO.
        for (ExecutableElement method : rootMethods) {
            List<? extends VariableElement> params = method.getParameters();

            String typeParameters = buildTypeParameters(method.getTypeParameters());
            sb.append("    @Override\n");
            sb.append("    public ");
            if (!typeParameters.isEmpty()) {
                sb.append(typeParameters).append(" ");
            }
            sb.append(method.getReturnType().toString()).append(" ")
                    .append(method.getSimpleName()).append("(").append(buildParamsSignature(params)).append(")");

            if (!method.getThrownTypes().isEmpty()) {
                sb.append(" throws ");
                sb.append(method.getThrownTypes().stream().map(TypeMirror::toString).collect(Collectors.joining(", ")));
            }

            sb.append(" {\n");
            sb.append("        this.resetNextCursor();\n");
            sb.append("        final ").append(rootRootFqn).append(" target = this.currentNextTarget();\n");
            sb.append("        if (target == null) {\n");
            sb.append("            throw new IllegalStateException(\"MRO exhausted for ").append(method.getSimpleName()).append("\");\n");
            sb.append("        }\n");

            String invocationArgs = params.stream()
                    .map(VariableElement::getSimpleName)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));

            if ("void".equals(method.getReturnType().toString())) {
                sb.append("        target.").append(method.getSimpleName()).append("(").append(invocationArgs).append(");\n");
            } else {
                sb.append("        return target.").append(method.getSimpleName()).append("(").append(invocationArgs).append(");\n");
            }

            sb.append("    }\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private String buildTypeParameters(List<? extends TypeParameterElement> typeParameters) {
        if (typeParameters.isEmpty()) {
            return "";
        }
        return "<" + typeParameters.stream().map(this::typeParameterToString).collect(Collectors.joining(", ")) + ">";
    }

    private String typeParameterToString(TypeParameterElement typeParameter) {
        String name = typeParameter.getSimpleName().toString();
        List<? extends TypeMirror> bounds = typeParameter.getBounds();
        if (bounds.size() == 1 && "java.lang.Object".equals(bounds.get(0).toString())) {
            return name;
        }
        return name + " extends " + bounds.stream().map(TypeMirror::toString).collect(Collectors.joining(" & "));
    }

    private String buildParamsSignature(List<? extends VariableElement> params) {
        return params.stream()
                .map(p -> p.asType().toString() + " " + p.getSimpleName())
                .collect(Collectors.joining(", "));
    }

    private static final class UseMiValues {
        private final TypeMirror rootType;
        private final List<TypeMirror> targetTypes;

        private UseMiValues(TypeMirror rootType, List<TypeMirror> targetTypes) {
            this.rootType = rootType;
            this.targetTypes = targetTypes;
        }
    }
}

