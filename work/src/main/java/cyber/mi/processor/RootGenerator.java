package cyber.mi.processor;

import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

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

        List<ExecutableElement> rootMethods = GeneratorSupport.collectRootMethods(rootInterface);
        try {
            JavaFileObject sourceFile = filer.createSourceFile(generatedFqn, rootInterface);
            try (Writer writer = sourceFile.openWriter()) {
                writer.write(buildClassSource(packageName, generatedSimpleName, interfaceName, rootMethods));
            }
        } catch (FilerException ex) {
            // In incremental/isolated compilation pipelines, the same source can be requested twice.
            // If the type already exists in the current round pipeline, keep compilation moving.
            if (ex.getMessage() != null && ex.getMessage().contains("Attempt to recreate a file")) {
                return;
            }
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to generate root class " + generatedFqn + ": " + ex.getMessage(),
                    rootInterface
            );
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

        sb.append("    private static final class DispatchContext {\n");
        sb.append("        private final java.util.List<").append(generatedSimpleName)
                .append("> mro = new java.util.ArrayList<>();\n");
        sb.append("        private int cursor = -1;\n");
        sb.append("    }\n\n");
        sb.append("    private DispatchContext __dispatchContext = new DispatchContext();\n\n");

        sb.append("    protected final void setMro(java.util.List<").append(generatedSimpleName).append("> mro) {\n");
        sb.append("        final DispatchContext context = new DispatchContext();\n");
        sb.append("        context.mro.addAll(mro);\n");
        sb.append("        context.cursor = -1;\n");
        sb.append("        for (").append(generatedSimpleName).append(" element : context.mro) {\n");
        sb.append("            element.__dispatchContext = context;\n");
        sb.append("        }\n");
        sb.append("        this.__dispatchContext = context;\n");
        sb.append("    }\n\n");

        sb.append("    protected final ").append(generatedSimpleName).append(" currentNextTarget() {\n");
        sb.append("        final int nextIndex = this.__dispatchContext.cursor + 1;\n");
        sb.append("        if (nextIndex < 0 || nextIndex >= this.__dispatchContext.mro.size()) {\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("        this.__dispatchContext.cursor = nextIndex;\n");
        sb.append("        return this.__dispatchContext.mro.get(nextIndex);\n");
        sb.append("    }\n\n");

        sb.append("    protected final void resetNextCursor() {\n");
        sb.append("        this.__dispatchContext.cursor = -1;\n");
        sb.append("    }\n\n");

        for (ExecutableElement method : rootMethods) {
            List<? extends javax.lang.model.element.VariableElement> params = method.getParameters();
            String methodName = method.getSimpleName().toString();
            String nextMethodName = GeneratorSupport.toNextMethodName(methodName);

            sb.append("    public ");
            String typeParameters = GeneratorSupport.buildTypeParameters(method.getTypeParameters());
            if (!typeParameters.isEmpty()) {
                sb.append(typeParameters).append(" ");
            }
            sb.append(method.getReturnType().toString()).append(" ").append(nextMethodName)
                    .append("(").append(GeneratorSupport.buildParamsSignature(params)).append(")");
            String throwsClause = GeneratorSupport.buildThrowsClause(method);
            if (!throwsClause.isEmpty()) {
                sb.append(" ").append(throwsClause);
            }
            sb.append(" {\n");
            sb.append("        final ").append(generatedSimpleName).append(" target = this.currentNextTarget();\n");
            sb.append("        if (target == null) {\n");
            sb.append("            throw new IllegalStateException(\"MRO exhausted for ").append(methodName).append("\");\n");
            sb.append("        }\n");
            String invocationArgs = GeneratorSupport.buildInvocationArgs(params);
            if ("void".equals(method.getReturnType().toString())) {
                sb.append("        target.").append(methodName).append("(").append(invocationArgs).append(");\n");
            } else {
                sb.append("        return target.").append(methodName).append("(").append(invocationArgs).append(");\n");
            }
            sb.append("    }\n\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
