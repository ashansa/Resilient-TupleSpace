/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2007 University of Lisbon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

package tfsd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketAddress;

import rsts.Tuple;
import rsts.TupleManager;
import rsts.TupleMessage;
import tfsd.Constants.MessageType;

import net.sf.appia.jgcs.AppiaGroup;
import net.sf.appia.jgcs.AppiaProtocolFactory;
import net.sf.appia.jgcs.AppiaService;
import net.sf.jgcs.ControlSession;
import net.sf.jgcs.DataSession;
import net.sf.jgcs.ExceptionListener;
import net.sf.jgcs.JGCSException;
import net.sf.jgcs.Message;
import net.sf.jgcs.MessageListener;
import net.sf.jgcs.Protocol;
import net.sf.jgcs.ProtocolFactory;
import net.sf.jgcs.Service;

/**
 * 
 * This class defines a ClientOpenGroupTest This example shows how to use and
 * configure Appia with jGCS using an open group, where there is a group of
 * servers that accept Messages from external members. This is the (external)
 * client part.
 * 
 * The example only shows how to configure and use, and it only sends dummy
 * messages. It does not intend to implement any algorithm.
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class ClientGroup implements MessageListener, ExceptionListener {

	private static final int MAX_MESSAGES = 2;

	// only the data session is used
	private DataSession data;
	private ControlSession control;
	private Service rpcService;
	private int id = 0;
	private int lastReceivedMessage = -1;

	public ClientGroup(DataSession data, ControlSession control,
                       Service serviceVSC) {
		this.data = data;
		this.rpcService = serviceVSC;
		this.control = control;
	}

	// messages are received here.
	public Object onMessage(Message msg) {
        try {
			TupleMessage tupleMsg = (TupleMessage) Constants
					.createMessageInstance(msg.getPayload());
			tupleMsg.unmarshal();
			if (tupleMsg.getId() > lastReceivedMessage) {
				lastReceivedMessage = tupleMsg.getId();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("Received message from " + msg.getSenderAddress());

		return null;
	}

	public void onJoin(SocketAddress peer) {
		System.out.println("-- JOIN: " + peer);
	}

	public void onLeave(SocketAddress peer) {
		System.out.println("-- LEAVE: " + peer);
	}

	public void onFailed(SocketAddress peer) {
		System.out.println("-- FAILED: " + peer);
	}

	public void onException(JGCSException arg0) {
		System.out.println("-- EXCEPTION: " + arg0.getMessage());
		arg0.printStackTrace();
	}

    private void sendMessage(String value1, String value2, String value3, TupleManager.QueryType type) throws IOException {
        Message msg = data.createMessage();
        TupleMessage tupleMessage = new TupleMessage(id++, type, new Tuple(value1, value2, value3));
        tupleMessage.marshal();
        byte[] bytes = Constants.createMessageToSend(MessageType.TUPLE, tupleMessage.getByteArray());
        msg.setPayload(bytes);
        data.send(msg, rpcService, null, null);
    }

    public void sendWriteMessage(String value1, String value2, String value3) throws IOException {
        sendMessage(value1, value2, value3, TupleManager.QueryType.WRITE);
    }

	public void run() throws Exception {

		System.out.println("> \n");
		String line = null;
		
		while (true) {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			
			System.out.print("> ");
			line = br.readLine();
			if (line.contains(",")){
				System.out.println("Sending message");
                String[] values = line.split(",");
                sendWriteMessage(values[0], values[1], values[2]);
			}

		}
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Must put the xml file name as an argument.");			
			/*
			 * java -classpath '/home/workspace/tfsd/labs/lab2/code/build:/home/workspace/tfsd/labs/lab2/code/lib/appia-core-4.1.2.jar:/home/workspace/tfsd/labs/lab2/code/lib/appia-groupcomm-4.1.2.jar:/home/workspace/tfsd/labs/lab2/code/lib/appia-project-4.1.2.jar:/home/workspace/tfsd/labs/lab2/code/lib/appia-test-4.1.2.jar:/home/workspace/tfsd/labs/lab2/code/lib/flanagan.jar:/home/workspace/tfsd/labs/lab2/code/lib/jgcs-0.6.1.jar:/home/workspace/tfsd/labs/lab2/code/lib/log4j-1.2.14.jar' tfsd.ClientOpenGroupTest 'config/client1.xml'
			 * 
			 */
			System.exit(1);
		}
		try {
			ProtocolFactory pf = new AppiaProtocolFactory();
			AppiaGroup g = new AppiaGroup();

			g.setConfigFileName(args[0]);
			g.setGroupName("group");
			g.setManagementMBeanID("id1");

			Protocol p = pf.createProtocol();
			DataSession session = p.openDataSession(g);
			ControlSession control = p.openControlSession(g);
			Service service = new AppiaService("rrpc");
			ClientGroup test = new ClientGroup(session,
					control, service);

			session.setMessageListener(test);
			session.setExceptionListener(test);

			test.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
