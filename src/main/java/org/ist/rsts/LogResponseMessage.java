package org.ist.rsts;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

public class LogResponseMessage extends ProtocolMessage implements Serializable {

    private String log;
    private HashMap<Integer, String> logs = new HashMap<Integer, String>();

    public LogResponseMessage(String log) {
        super();
        this.log = log;
    }

    public LogResponseMessage(byte[] buffer) throws IOException {
        super(buffer);
    }

    @Override
    public void writeUserData(ObjectOutputStream os) throws IOException {
        os.writeUTF(log);
    }

    @Override
    public void readUserData(ObjectInputStream is) throws IOException, ClassNotFoundException {
        log = is.readUTF();
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public HashMap<Integer, String> getLogs() {
        return logs;
    }
}
