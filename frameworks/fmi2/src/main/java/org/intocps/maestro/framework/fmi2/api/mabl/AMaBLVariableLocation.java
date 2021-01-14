package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.framework.fmi2.api.mabl.scoping.AMaBLScope;

public class AMaBLVariableLocation {
    AMaBLScope scope;
    String name;
    IValuePosition position;

    public interface IValuePosition {
    }

    public static class ArrayPosition implements IValuePosition {
        int index;
        private IValuePosition nestedPosition;

        public ArrayPosition(int index) {
            this.index = index;
        }

        public ArrayPosition(int index, IValuePosition nestedPosition) {
            this.index = index;
            this.nestedPosition = nestedPosition;
        }

        public IValuePosition getNestedPosition() {
            return this.nestedPosition;
        }

        public void setNestedPosition(IValuePosition nestedPosition) {
            this.nestedPosition = nestedPosition;
        }
    }

    public static class BasicPosition implements IValuePosition {
    }


}
