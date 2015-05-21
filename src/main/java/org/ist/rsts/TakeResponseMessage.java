package org.ist.rsts;

import org.ist.rsts.tuple.Tuple;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.UUID;
import java.util.Vector;

/**
 * Created by ashansa on 5/21/15.
 */
public class TakeResponseMessage  extends ProtocolMessage implements Serializable {

    Vector<Tuple> matchingTuples;
    UUID uuidOfTake;

    public TakeResponseMessage(byte[] buffer) throws IOException {
        super(buffer);
    }

    public TakeResponseMessage(Vector<Tuple> matchingTuples, UUID uuidOfTakeRequest) {
        this.matchingTuples = matchingTuples;
        this.uuidOfTake = uuidOfTakeRequest;
    }

    public Vector<Tuple> getMatchingTuples() {
        return matchingTuples;
    }

    public UUID getUuidOfTake() {
        return uuidOfTake;
    }

    @Override
    public void writeUserData(ObjectOutputStream os) throws IOException {
        os.writeObject(matchingTuples);
        os.writeObject(uuidOfTake);
    }

    @Override
    public void readUserData(ObjectInputStream is) throws IOException, ClassNotFoundException {
        matchingTuples = (Vector<Tuple>) is.readObject();
        uuidOfTake = (UUID) is.readObject();
    }
}
