package cyber.mi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation: triggers generation of a "used" class which simulates multiple inheritance
 * by delegating root-interface methods through a linearized MRO list.
 *
 * <p>Expected model:</p>
 * <ul>
 *     <li>{@code root()} is an interface annotated with {@link RootHierarchy}</li>
 *     <li>{@code targets()} are concrete classes (or adapters) that extend {@code root()Root}
 *     and implement the root interface methods</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface UseMultipleInheritance {
    /**
     * Root interface annotated with {@link RootHierarchy}.
     */
    Class<?> root();

    /**
     * MRO order (first element wins). Each class must extend the generated {@code root()Root}.
     */
    Class<?>[] targets();
}

