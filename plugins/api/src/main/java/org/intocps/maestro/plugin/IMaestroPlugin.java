package org.intocps.maestro.plugin;

public interface IMaestroPlugin {

    /**
     * Global boolean identifier which indicates if execution can continue. This must be set to false by plugins if they encounter an error
     */
    String GLOBAL_EXECUTION_CONTINUE = "global_execution_continue";

    String getName();

    String getVersion();


}
