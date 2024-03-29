/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2007 University of Lisbon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this logFile except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 *
 * Developer(s): Nuno Carvalho.
 */
package org.ist.rsts;

import org.ist.rsts.tuple.TupleMessage;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class Constants {

    enum MessageType {
        SERVER, TUPLE, LOG_REQUEST, LOG_RESPONSE, TAKE_RESPONSE;
        ProtocolMessage createMessage(byte[] buffer) throws IOException {
            switch (this) {
                case SERVER:
                    return new ServerMessage(buffer);
                case TUPLE:
                    return new TupleMessage(buffer);
                case LOG_REQUEST:
                    return new LogRequestMessage(buffer);
                case LOG_RESPONSE:
                    return new LogResponseMessage(buffer);
                case TAKE_RESPONSE:
                    return new TakeResponseMessage(buffer);
                default:
                    return null;
            }
        }
    }

    public static ProtocolMessage createMessageInstance(byte[] buffer) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        byte[] strBytes = new byte[bb.getInt()];
        bb.get(strBytes);
        byte[] msg = new byte[bb.remaining()];
        bb.get(msg);
        return MessageType.valueOf(new String(strBytes)).createMessage(msg);
    }

    public static byte[] createMessageToSend(MessageType type, byte[] msg) {
        byte[] msgName = type.name().getBytes();
        ByteBuffer bb = ByteBuffer.allocate(msgName.length + msg.length + 4);
        bb.putInt(msgName.length);
        bb.put(msgName);
        bb.put(msg);
        bb.flip();
        byte[] ret = bb.array();
        return ret;
    }


}
