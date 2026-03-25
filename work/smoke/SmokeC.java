package smoke;

public class SmokeC extends SmokeRootRoot {
    @Override
    public String ping(String value) {
        return "C:" + value;
    }
}
