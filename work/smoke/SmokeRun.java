package smoke;

public class SmokeRun {
    public static void main(String[] args) {
        SmokeMarkerMI mi = new SmokeMarkerMI(new SmokeA(), new SmokeB(), new SmokeC());
        System.out.println(mi.ping("hello"));
    }
}

