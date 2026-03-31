package cyber.mi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public final class MultipleInheritanceFactory {
    private static final String GENERATED_SUFFIX = "MI";

    private MultipleInheritanceFactory() {
    }

    public static <T> T create(Class<?> markerClass, Class<T> rootType, Object... targets) {
        if (markerClass == null) {
            throw new IllegalArgumentException("markerClass must not be null");
        }
        if (rootType == null) {
            throw new IllegalArgumentException("rootType must not be null");
        }
        Object instance = create(markerClass, targets);
        if (!rootType.isInstance(instance)) {
            throw new IllegalStateException(
                    "Generated class for marker " + markerClass.getName()
                            + " is not assignable to " + rootType.getName()
            );
        }
        return rootType.cast(instance);
    }

    public static Object create(Class<?> markerClass, Object... targets) {
        if (markerClass == null) {
            throw new IllegalArgumentException("markerClass must not be null");
        }
        Object[] args = targets == null ? new Object[0] : targets;
        String generatedClassName = markerClass.getName() + GENERATED_SUFFIX;
        try {
            Class<?> generatedClass = Class.forName(generatedClassName);
            Constructor<?> constructor = findMatchingConstructor(generatedClass, args);
            return constructor.newInstance(args);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Generated class not found: " + generatedClassName
                            + ". Ensure annotation processing has been run.",
                    e
            );
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to instantiate generated class: " + generatedClassName, e);
        }
    }

    private static Constructor<?> findMatchingConstructor(Class<?> generatedClass, Object[] args) {
        Constructor<?>[] constructors = generatedClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length != args.length) {
                continue;
            }
            if (isCompatible(paramTypes, args)) {
                return constructor;
            }
        }
        throw new IllegalStateException(
                "No matching constructor in " + generatedClass.getName()
                        + " for arguments: " + Arrays.toString(args)
        );
    }

    private static boolean isCompatible(Class<?>[] paramTypes, Object[] args) {
        for (int i = 0; i < paramTypes.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                return false;
            }
            if (!paramTypes[i].isAssignableFrom(arg.getClass())) {
                return false;
            }
        }
        return true;
    }
}
