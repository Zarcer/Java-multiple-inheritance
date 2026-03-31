package smoke;

import cyber.mi.MultipleInheritanceFactory;

public class SmokeRun {
    public static void main(String[] args) {
        A instance = MultipleInheritanceFactory.create(
                Marker.class,
                A.class,
                new D(),
                new B(),
                new C()
        );
        System.out.println(instance.method());
    }
}
