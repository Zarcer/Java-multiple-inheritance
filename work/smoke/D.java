package smoke;

public class D extends ARoot {
    @Override
    public String method() {
        return "D->" + nextMethod();
    }
}
