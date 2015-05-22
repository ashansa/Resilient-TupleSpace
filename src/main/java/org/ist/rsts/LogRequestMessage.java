package org.ist.rsts;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class LogRequestMessage extends ProtocolMessage implements Serializable {

    private int viewId = 0;
    private StringBuffer sender;

    public LogRequestMessage(int viewId, StringBuffer sender){
        super();
        this.viewId = viewId;
        this.sender = sender;
    }

    public LogRequestMessage(byte[] buffer) throws IOException {
        super(buffer);
    }

    @Override
    public void writeUserData(ObjectOutputStream os) throws IOException {
        os.writeObject(viewId);
        os.writeObject(sender);
    }

    @Override
    public void readUserData(ObjectInputStream is) throws IOException, ClassNotFoundException {
        viewId = (Integer) is.readObject();
        sender = (StringBuffer) is.readObject();
    }

    public int getViewId() {
        return viewId;
    }

    public StringBuffer getSenderAddressString() {
        return sender;
    }

    public void setViewId(int viewId) {
        this.viewId = viewId;
    }
}
