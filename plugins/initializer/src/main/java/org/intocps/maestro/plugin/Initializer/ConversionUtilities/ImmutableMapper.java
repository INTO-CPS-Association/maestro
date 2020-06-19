package org.intocps.maestro.plugin.Initializer.ConversionUtilities;

import scala.collection.JavaConverters$;
import scala.collection.immutable.Set;

public class ImmutableMapper {
    public static <E> Set<E> convertSet(java.util.Set<E> m) {
        return JavaConverters$.MODULE$.asScalaSetConverter(m).asScala().toSet();
    }
}
