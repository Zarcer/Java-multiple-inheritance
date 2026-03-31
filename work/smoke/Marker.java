package smoke;

import cyber.mi.annotations.UseMultipleInheritance;

@UseMultipleInheritance(root = A.class, targets = {D.class, B.class, C.class})
public class Marker {
}
