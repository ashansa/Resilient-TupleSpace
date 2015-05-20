package org.ist.rsts;


/**
 * This will store and manage all the relevant information regarding State Machine replication for a server
 */
public class StateManager {

    private static StateManager stateManager;
    private String viewId ;

    protected StateManager() {
    }

    /**
     * Returns instance of the StateManger
     *
     * @return
     */
    public static StateManager getInstance() {
        if (stateManager == null) {
            stateManager = new StateManager();
        }
        return stateManager;
    }

    public void setViewNumber(String viewId) {
        this.viewId = viewId;
    }

    public String getCurrentViewId() {
        return  viewId;
    }


}
