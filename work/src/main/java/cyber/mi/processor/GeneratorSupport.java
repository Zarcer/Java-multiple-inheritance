package cyber.mi.processor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class GeneratorSupport {
    private GeneratorSupport() {
    }

    static List<ExecutableElement> collectRootMethods(TypeElement rootInterface) {
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

    static String buildTypeParameters(List<? extends TypeParameterElement> typeParameters) {
        if (typeParameters.isEmpty()) {
            return "";
        }
        return "<" + typeParameters.stream().map(GeneratorSupport::typeParameterToString).collect(Collectors.joining(", ")) + ">";
    }

    static String buildParamsSignature(List<? extends VariableElement> params) {
        return params.stream()
                .map(p -> p.asType().toString() + " " + p.getSimpleName())
                .collect(Collectors.joining(", "));
    }

    static String buildThrowsClause(ExecutableElement method) {
        if (method.getThrownTypes().isEmpty()) {
            return "";
        }
        return "throws " + method.getThrownTypes().stream().map(TypeMirror::toString).collect(Collectors.joining(", "));
    }

    static String buildInvocationArgs(List<? extends VariableElement> params) {
        return params.stream()
                .map(VariableElement::getSimpleName)
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }

    static String toNextMethodName(String methodName) {
        if (methodName.isEmpty()) {
            return "next";
        }
        return "next" + Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);
    }

    private static String typeParameterToString(TypeParameterElement typeParameter) {
        String name = typeParameter.getSimpleName().toString();
        List<? extends TypeMirror> bounds = typeParameter.getBounds();
        if (bounds.size() == 1 && "java.lang.Object".equals(bounds.get(0).toString())) {
            return name;
        }
        return name + " extends " + bounds.stream().map(TypeMirror::toString).collect(Collectors.joining(" & "));
    }
}
