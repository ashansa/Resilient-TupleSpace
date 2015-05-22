package org.ist.rsts.minorityview;

import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelInit;

public class CrashRecoverLayer extends Layer{

    public CrashRecoverLayer() {
        evProvide = new Class[0];

        evRequire = new Class[0];

        evAccept = new Class[2];
        evAccept[0] = Event.class;
        evAccept[1] = ChannelInit.class;
    }

    @Override
    public Session createSession() {
        return new CrashRecoverSession(this);
    }
}
