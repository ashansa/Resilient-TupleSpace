import java.util.Vector;

public class Tuple {

    private Vector<String> values;

    public Tuple(String value1, String value2, String value3) {
        values = new Vector<String>();
        values.add(value1);
        values.add(value2);
        values.add(value3);
    }

    public Vector<String> getValues() {
        return values;
    }
}
