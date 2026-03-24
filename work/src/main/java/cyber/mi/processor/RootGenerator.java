package cyber.mi.processor;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class RootGenerator {

    private final Filer filer;
    private final Messager messager;

    public RootGenerator(Filer filer, Messager messager) {
        this.filer = filer;
        this.messager = messager;
    }

    public void generate(TypeElement rootInterface) {
        if (rootInterface.getKind() != ElementKind.INTERFACE) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@RootHierarchy can only be applied to an interface",
                    rootInterface
            );
            return;
        }

        PackageElement packageElement = findPackage(rootInterface);
        String packageName = packageElement.isUnnamed() ? "" : packageElement.getQualifiedName().toString();
        String interfaceName = rootInterface.getSimpleName().toString();
        String generatedSimpleName = interfaceName + "Root";
        String generatedFqn = packageName.isEmpty() ? generatedSimpleName : packageName + "." + generatedSimpleName;

        List<ExecutableElement> rootMethods = collectRootMethods(rootInterface);
        try {
            JavaFileObject sourceFile = filer.createSourceFile(generatedFqn, rootInterface);
            try (Writer writer = sourceFile.openWriter()) {
                writer.write(buildClassSource(packageName, generatedSimpleName, interfaceName, rootMethods));
            }
        } catch (IOException ex) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to generate root class " + generatedFqn + ": " + ex.getMessage(),
                    rootInterface
            );
        }
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
            String interfaceName,
            List<ExecutableElement> rootMethods
    ) {
        StringBuilder sb = new StringBuilder(2048);
        if (!packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }

        sb.append("public abstract class ").append(generatedSimpleName)
                .append(" implements ").append(interfaceName).append(" {\n\n");

        sb.append("    protected final java.util.List<").append(generatedSimpleName)
                .append("> __mro = new java.util.ArrayList<>();\n");
        sb.append("    protected int __mroCursor = -1;\n\n");

        sb.append("    protected final void setMro(java.util.List<").append(generatedSimpleName).append("> mro) {\n");
        sb.append("        this.__mro.clear();\n");
        sb.append("        this.__mro.addAll(mro);\n");
        sb.append("        this.__mroCursor = -1;\n");
        sb.append("    }\n\n");

        sb.append("    protected final ").append(generatedSimpleName).append(" currentNextTarget() {\n");
        sb.append("        int nextIndex = this.__mroCursor + 1;\n");
        sb.append("        if (nextIndex < 0 || nextIndex >= this.__mro.size()) {\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("        this.__mroCursor = nextIndex;\n");
        sb.append("        return this.__mro.get(nextIndex);\n");
        sb.append("    }\n\n");

        sb.append("    protected final void resetNextCursor() {\n");
        sb.append("        this.__mroCursor = -1;\n");
        sb.append("    }\n\n");
        sb.append("}\n");
        return sb.toString();
    }
}
