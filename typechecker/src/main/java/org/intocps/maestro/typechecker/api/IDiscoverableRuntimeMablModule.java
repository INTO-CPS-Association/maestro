package org.intocps.maestro.typechecker.api;

import java.io.InputStream;

public interface IDiscoverableRuntimeMablModule {
    /**
     * Optional mable import module describing the instantiated module type
     *
     * @return null or an input stream to the mable specification of the returned module
     */
    InputStream getMablModule();
}
