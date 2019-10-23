package org.intocps.orchestration.coe.webapi.controllers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class VndErrors implements Iterable<VndErrors.VndError> {
    private final List<VndError> vndErrors;

    public VndErrors(String logref, String message) {
        this(new VndErrors.VndError(logref, message));
    }

    public VndErrors(VndErrors.VndError error, VndErrors.VndError... errors) {
        this.vndErrors = new ArrayList(errors.length + 1);
        this.vndErrors.add(error);
        this.vndErrors.addAll(Arrays.asList(errors));
    }

    @JsonCreator
    public VndErrors(List<VndError> errors) {
        this.vndErrors = errors;
    }

    protected VndErrors() {
        this.vndErrors = new ArrayList();
    }

    public VndErrors add(VndErrors.VndError error) {
        this.vndErrors.add(error);
        return this;
    }

    @JsonValue
    private List<VndError> getErrors() {
        return this.vndErrors;
    }

    @Override
    public Iterator<VndError> iterator() {
        return this.vndErrors.iterator();
    }

    @Override
    public int hashCode() {
        return this.vndErrors.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof VndErrors)) {
            return false;
        } else {
            VndErrors that = (VndErrors) obj;
            return this.vndErrors.equals(that.vndErrors);
        }
    }

    @Override
    public String toString() {
        return String.format("VndErrors[%s]", this.vndErrors.stream().map(Object::toString).collect(Collectors.joining(",")));
    }

    public static class VndError {
        @JsonProperty
        private final String logref;
        @JsonProperty
        private final String message;

        public VndError(String logref, String message) {
            this.logref = logref;
            this.message = message;
        }

        public VndError() {
            this.logref = null;
            this.message = null;
        }

        public String getLogref() {
            return this.logref;
        }

        public String getMessage() {
            return this.message;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = result + 31 * this.logref.hashCode();
            result += 31 * this.message.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (!(obj instanceof VndErrors.VndError)) {
                return false;
            } else {
                VndErrors.VndError that = (VndErrors.VndError) obj;
                return this.message.equals(that.message);
            }
        }

        @Override
        public String toString() {
            return String.format("VndError[logref: %s, message: %s]", this.logref, this.message);
        }
    }


}
