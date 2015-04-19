import net.sf.appia.jgcs.AppiaGroup;
import net.sf.appia.jgcs.AppiaProtocolFactory;
import net.sf.appia.jgcs.AppiaService;
import net.sf.jgcs.*;
import net.sf.jgcs.membership.BlockListener;
import net.sf.jgcs.membership.BlockSession;
import net.sf.jgcs.membership.MembershipListener;
import net.sf.jgcs.membership.MembershipSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketAddress;

/**
 * Created by ashansa on 4/15/15.
 */
public class AppiaManager implements ExceptionListener, BlockListener {

    ControlSession controlSession;
    DataSession dataSession;
    Service clientService;
    Service groupService;

    public static void main(String args[]) throws Exception {
        /* Create layers and put them in a array */
     /*   ProtocolFactory pf = new AppiaProtocolFactory();
        AppiaGroup g = new AppiaGroup();
        g.setGroupName("group");
        g.setConfigFileName(args[0]);
        Protocol p = pf.createProtocol();
        DataSession session = p.openDataSession(g);
        ControlSession control = p.openControlSession(g);
        Service sc = new AppiaService("rrpc");
        Service sg = new AppiaService("rrpc_group");
        AppiaManager appiaManager = new AppiaManager(control, session, sc, sg);
        appiaManager.run();*/
    }

    public AppiaManager(ControlSession controlSession, DataSession dataSession, Service clientService,
                        Service groupService) throws JGCSException {
        this.controlSession = controlSession;
        this.dataSession = dataSession;
        this.clientService = clientService;
        this.groupService = groupService;

        // set listeners
      /*  GroupMessageListener l = new GroupMessageListener();
        dataSession.setMessageListener(l);
        dataSession.setServiceListener(l);
        controlSession.setControlListener(this);
        controlSession.setExceptionListener(this);
        if (controlSession instanceof MembershipSession)
            ((MembershipSession) controlSession).setMembershipListener(this);
        if (controlSession instanceof BlockSession)
            ((BlockSession) controlSession).setBlockListener(this);*/
    }


    private void run() throws IOException {
        System.out.println("> \n");
        String line = null;

        while (true) {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    System.in));

            System.out.print("> ");

            line = br.readLine();

            if (line.contains("s")){
                System.out.println("Sending message");
                sendMessage();
            }

        }
    }

    private void sendMessage() throws UnsupportedServiceException, IOException {
       /* Message m = data.createMessage();
        ClientMessage climsg = new ClientMessage(id++);
        climsg.marshal();
        byte[] bytes = Constants.createMessageToSend(MessageType.CLIENT,
                climsg.getByteArray());
        m.setPayload(bytes);

        tInit = System.nanoTime();
        System.out.println("sending message with id: " + (id - 1));
        data.send(m, rpcService, null, null);*/
    }

    @Override
    public void onBlock() {

    }

    @Override
    public void onException(JGCSException e) {

    }

/*
    @Override
    public void onBlock() {

    }

    @Override
    public void onJoin(SocketAddress socketAddress) {

    }

    @Override
    public void onLeave(SocketAddress socketAddress) {

    }

    @Override
    public void onFailed(SocketAddress socketAddress) {

    }

    @Override
    public void onException(JGCSException e) {

    }

    @Override
    public void onMembershipChange() {

    }

    @Override
    public void onExcluded() {

    }*/
}
