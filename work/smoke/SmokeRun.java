package smoke;

public class SmokeRun {
    public static void main(String[] args) {
        SmokeMarkerMI mi = new SmokeMarkerMI(new SmokeA(), new SmokeB());
        System.out.println(mi.ping("hello"));
    }
}

