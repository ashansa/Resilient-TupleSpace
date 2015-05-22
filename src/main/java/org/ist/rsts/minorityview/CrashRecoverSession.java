package org.ist.rsts.minorityview;

import net.sf.appia.core.*;
import org.ist.rsts.ServerGroup;

public class CrashRecoverSession extends Session {

    public CrashRecoverSession(Layer layer) {
        super(layer);
    }

    public void handle(Event event) {

        if(!ServerGroup.isIsolated) {
            try {
                event.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
        }
    }
}
