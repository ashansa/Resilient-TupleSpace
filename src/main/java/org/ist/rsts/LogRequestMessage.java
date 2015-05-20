package org.ist.rsts;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class LogRequestMessage extends ProtocolMessage implements Serializable {

    private int viewId;

    public LogRequestMessage(int viewId){
        super();
        this.viewId = viewId;
    }

    public LogRequestMessage(byte[] buffer) throws IOException {
        super(buffer);
    }

    @Override
    public void writeUserData(ObjectOutputStream os) throws IOException {
        os.write(viewId);
    }

    @Override
    public void readUserData(ObjectInputStream is) throws IOException, ClassNotFoundException {
        viewId = is.readInt();
    }

    public int getViewId() {
        return viewId;
    }

    public void setViewId(int viewId) {
        this.viewId = viewId;
    }
}
