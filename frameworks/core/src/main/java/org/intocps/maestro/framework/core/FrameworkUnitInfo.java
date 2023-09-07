package org.intocps.maestro.framework.core;

import java.util.Optional;

public interface FrameworkUnitInfo {

     String getOwnerIdentifier();
     Optional<FaultInject> getFaultInject();
     void setFaultInject(String constraintId);


}
