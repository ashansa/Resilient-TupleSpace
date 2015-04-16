import net.sf.appia.jgcs.AppiaGroup;
import net.sf.appia.jgcs.AppiaProtocolFactory;
import net.sf.appia.jgcs.AppiaService;
import net.sf.jgcs.*;
import net.sf.jgcs.membership.BlockListener;
import net.sf.jgcs.membership.MembershipListener;

import java.net.SocketAddress;

/**
 * Created by ashansa on 4/15/15.
 */
public class AppiaManager implements ControlListener, ExceptionListener,
        MembershipListener, BlockListener {

    public static void main(String args[]) throws Exception {
        /* Create layers and put them in a array */
        ProtocolFactory pf = new AppiaProtocolFactory();
        AppiaGroup g = new AppiaGroup();
        g.setGroupName("group");
        g.setConfigFileName(args[0]);
        Protocol p = pf.createProtocol();
        DataSession session = p.openDataSession(g);
        ControlSession control = p.openControlSession(g);
        Service sc = new AppiaService("rrpc");
        Service sg = new AppiaService("rrpc_group");
        AppiaManager test = new AppiaManager(control, session, sc, sg);
        ////test.run();
    }

    public AppiaManager(ControlSession controlSession, DataSession dataSession, Service clientService,
                        Service groupService) {
        /*this.control = control;
        this.groupSession = grSession;
        this.clients = cl;
        this.group = gr;

        // set listeners
        GroupMessageListener l = new GroupMessageListener();
        groupSession.setMessageListener(l);
        groupSession.setServiceListener(l);
        control.setControlListener(this);
        control.setExceptionListener(this);
        if (control instanceof MembershipSession)
            ((MembershipSession) control).setMembershipListener(this);
        if (control instanceof BlockSession)
            ((BlockSession) control).setBlockListener(this);*/
    }

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

    }
}
