package smoke;

public class B extends ARoot {
    @Override
    public String method() {
        return "B->" + nextMethod();
    }
}
