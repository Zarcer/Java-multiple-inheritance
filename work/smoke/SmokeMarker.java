package smoke;

import cyber.mi.annotations.UseMultipleInheritance;

@UseMultipleInheritance(root = SmokeRoot.class, targets = {SmokeA.class, SmokeB.class})
public class SmokeMarker {
}

