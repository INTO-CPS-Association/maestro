package org.intocps.maestro.framework.fmi2;

import org.intocps.maestro.framework.core.EnvironmentException;

public class KeyInstance {
    public String key;
    public String instance;
    public String variable;

    public KeyInstance(String key, String instance) {
        this.key = key;
        this.instance = instance;
    }

    public KeyInstance(String key, String instance, String variable) {
        this(key, instance);
        this.variable = variable;
    }

    public static KeyInstance ofKeyInstance(String keyInstance) throws EnvironmentException {
        int indexOfKeyEnd = keyInstance.indexOf("}"); // {fmukey}.instancename
        if (indexOfKeyEnd == -1) {
            throw new EnvironmentException("Failed to replace instance in variable: " + keyInstance + " due to missing index of '}'");
        }
        String key = keyInstance.substring(1, indexOfKeyEnd);
        String instance = keyInstance.substring(indexOfKeyEnd + 2); // +2 due to "}."
        if (instance.length() == 0) {
            throw new EnvironmentException("Failed to replace instance in variable: " + keyInstance + " due to missing instance name'");
        }
        return new KeyInstance(key, instance);
    }

    public static KeyInstance ofVariable(String variableString) throws EnvironmentException {
        int indexOfKeyEnd = variableString.indexOf("}"); // {fmukey}.instancename.variable
        if (indexOfKeyEnd == -1) {
            throw new EnvironmentException("Failed to replace instance in variable: " + variableString + " due to missing index of '}'");
        }
        String key = variableString.substring(1, indexOfKeyEnd);
        String variableStringWithoutKey = variableString.substring(indexOfKeyEnd + 2);
        int indexOfEndInstance = variableStringWithoutKey.indexOf('.');
        if (indexOfEndInstance == -1) {
            throw new EnvironmentException("Failed to replace instance in variable: " + variableString + " due to missing index of '.'");
        }
        String instance = variableStringWithoutKey.substring(0, indexOfEndInstance);
        String variable = variableStringWithoutKey.substring(indexOfEndInstance + 1); // +1 due to .
        return new KeyInstance(key, instance, variable);
    }

    public String toKeyInstance() {
        return "{" + key + "}." + instance;
    }

    public String toVariable() {
        return toKeyInstance() + "." + this.variable;
    }

    @Override
    public int hashCode() {
        return key.hashCode() ^ instance.hashCode(); //https://stackoverflow.com/questions/6187294/java-set-collection-override-equals-method
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof KeyInstance) && (((KeyInstance) obj).key.equals(this.key)) && (((KeyInstance) obj).instance.equals(this.instance));
    }
}

