import net.sf.appia.core.*;
import net.sf.appia.protocols.fifo.FifoLayer;
import net.sf.appia.protocols.group.sync.VSyncLayer;

/**
 * Created by ashansa on 4/15/15.
 */
public class AppiaManager {

    public void initAppia() {
        /* Create layers and put them in a array */
        Layer[] qos = { new VSyncLayer(), new FifoLayer()};

		/* Create a QoS */
        QoS myQoS = null;
        try {
            myQoS = new QoS("Print stack", qos);
        } catch (AppiaInvalidQoSException ex) {
            System.err.println("Invalid QoS");
            System.err.println(ex.getMessage());
            System.exit(1);
        }

		/* Create a channel. Uses default event scheduler. */
        Channel channel = myQoS.createUnboundChannel("Print Channel");
        try {
            channel.start();
        } catch (AppiaDuplicatedSessionsException ex) {
            System.err.println("Error in starting the channel");
            System.exit(1);
        }

		/* All set. Appia main class will handle the rest */
        System.out.println("Starting Appia...");
        Appia.run();
    }
}
