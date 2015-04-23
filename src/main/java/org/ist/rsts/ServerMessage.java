package org.ist.rsts;

import org.ist.rsts.tuple.TupleMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketAddress;

public class ServerMessage extends ProtocolMessage {

    int id;
    SocketAddress addr;
    TupleMessage tupleMessage;
    
    public ServerMessage(byte[] buf) throws IOException{
        super(buf);
    }
    
    public ServerMessage(int id, SocketAddress addr, TupleMessage tupleMessage){
        this.id = id;
        this.addr = addr;
        this.tupleMessage = tupleMessage;
    }
    
    @Override
    public void readUserData(ObjectInputStream is) throws IOException,
            ClassNotFoundException {
        id = is.readInt();
        addr = (SocketAddress) is.readObject();
        tupleMessage = (TupleMessage)is.readObject();
    }

    @Override
    public void writeUserData(ObjectOutputStream os) throws IOException {
        os.writeInt(id);
        os.writeObject(addr);
        os.writeObject(tupleMessage);
    }

}
