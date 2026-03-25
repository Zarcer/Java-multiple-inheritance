package smoke;

public class SmokeA extends SmokeRootRoot {
    @Override
    public String ping(String value) {
        return "A:" + value;
    }
}

