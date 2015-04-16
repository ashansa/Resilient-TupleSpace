import java.util.Vector;

/**
 * Created by ashansa on 4/16/15.
 */
public class TupleManager {

    TupleSpace tupleSpace = new TupleSpace();

    public boolean write(Tuple tuple) {
        return tupleSpace.write(tuple);
    }

    public Vector<Tuple> read(Tuple template) {
        return tupleSpace.read(template);
    }

    public Tuple take(Tuple template) {
        Tuple match = tupleSpace.take(template);
        if(match == null) {
            //add to wait list
        }
        return match;
    }
}
