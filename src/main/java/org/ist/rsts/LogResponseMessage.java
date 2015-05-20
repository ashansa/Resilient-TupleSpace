package org.ist.rsts;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

public class LogResponseMessage extends ProtocolMessage implements Serializable {

    private HashMap<Integer, String> logs = new HashMap<Integer, String>();

    public LogResponseMessage(HashMap<Integer,String> logs) {
        super();
        this.logs = logs;
    }

    public LogResponseMessage(byte[] buffer) throws IOException {
        super(buffer);
    }

    @Override
    public void writeUserData(ObjectOutputStream os) throws IOException {
        os.writeObject(logs);
    }

    @Override
    public void readUserData(ObjectInputStream is) throws IOException, ClassNotFoundException {
        logs = (HashMap<Integer, String>) is.readObject();
    }

    public HashMap<Integer, String> getLogs() {
        return logs;
    }
}
