import java.util.Vector;

/**
 * Created by ashansa on 4/15/15.
 */
public class TupleSpace {

    Vector<Tuple> tuples = new Vector<Tuple>();

    public boolean write(Tuple tuple) {
        tuples.add(tuple);
        return true;
    }

    public Vector<Tuple> read(Tuple template) {
        Vector<Tuple> matches = new Vector<Tuple>();
        //add matches to this
        return matches;
    }

    public Tuple take(Tuple template) {
        Tuple match = null;
        if(tuples.contains(template)) {
            match = template;
            tuples.remove(template);
        }
        return match;
    }
}
