package smoke;

public class SmokeB extends SmokeRootRoot {
    @Override
    public String ping(String value) {
        return "B:" + value;
    }
}

