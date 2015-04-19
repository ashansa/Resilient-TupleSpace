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
import net.sf.jgcs.UnsupportedServiceException;

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
public class ClientOpenGroupTest implements MessageListener, ExceptionListener {

	private static final int MAX_MESSAGES = 2;

	// only the data session is used
	private DataSession data;
	private ControlSession control;
	private Service rpcService;
	private long tInit = 0;
	private int id = 0;
	private int lastReceivedMessage = -1;

	public ClientOpenGroupTest(DataSession data, ControlSession control,
			Service serviceVSC) {
		this.data = data;
		this.rpcService = serviceVSC;
		this.control = control;
	}

	// messages are received here.
	public Object onMessage(Message msg) {
		boolean canSend = false;
		/*try {
			ClientMessage cliMsg = (ClientMessage) Constants
					.createMessageInstance(msg.getPayload());
			cliMsg.unmarshal();
			if (cliMsg.id > lastReceivedMessage) {
				lastReceivedMessage = cliMsg.id;
				canSend = true;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
        */
        try {
			rsts.TupleMessage tupleMsg = (rsts.TupleMessage) Constants
					.createMessageInstance(msg.getPayload());
			tupleMsg.unmarshal();
			if (tupleMsg.getId() > lastReceivedMessage) {
				lastReceivedMessage = tupleMsg.getId();
				canSend = true;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		//long deltaT = System.nanoTime() - tInit;
		//System.out.println("Received message from " + msg.getSenderAddress());

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

	private void sendMessage() throws UnsupportedServiceException, IOException {
		Message m = data.createMessage();
		ClientMessage climsg = new ClientMessage(id++);
		climsg.marshal();
		byte[] bytes = Constants.createMessageToSend(MessageType.CLIENT,
				climsg.getByteArray());
		m.setPayload(bytes);

		tInit = System.nanoTime();
		System.out.println("sending message with id: " + (id - 1));
		data.send(m, rpcService, null, null);
	}

    private void sendMessage(String value1, String value2, String value3) throws IOException {
        Message msg = data.createMessage();
        //rsts.TupleMessage tupleMessage = new rsts.TupleMessage(new Tuple(value1, value2, value3), id++);
        TupleMessage tupleMessage = new TupleMessage(value1, value2, value3, id++);
        tupleMessage.marshal();
        byte[] bytes = Constants.createMessageToSend(MessageType.TUPLE, tupleMessage.getByteArray());
        msg.setPayload(bytes);
        data.send(msg, rpcService, null, null);
    }

	public void run() throws Exception {

		System.out.println("> \n");
		String line = null;
		
		while (true) {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			
			System.out.print("> ");
			
			line = br.readLine();
//line = "aa,sd,aew";
			if (line.contains(",")){
				System.out.println("Sending message");

                String[] values = line.split(",");
//                sendMessage();
                sendMessage(values[0], values[1], values[2]);
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
			ClientOpenGroupTest test = new ClientOpenGroupTest(session,
					control, service);

			session.setMessageListener(test);
			session.setExceptionListener(test);

			test.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
