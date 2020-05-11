/*
 * This file is part of the INTO-CPS toolchain.
 *
 * Copyright (c) 2017-CurrentYear, INTO-CPS Association,
 * c/o Professor Peter Gorm Larsen, Department of Engineering
 * Finlandsgade 22, 8200 Aarhus N.
 *
 * All rights reserved.
 *
 * THIS PROGRAM IS PROVIDED UNDER THE TERMS OF GPL VERSION 3 LICENSE OR
 * THIS INTO-CPS ASSOCIATION PUBLIC LICENSE VERSION 1.0.
 * ANY USE, REPRODUCTION OR DISTRIBUTION OF THIS PROGRAM CONSTITUTES
 * RECIPIENT'S ACCEPTANCE OF THE OSMC PUBLIC LICENSE OR THE GPL
 * VERSION 3, ACCORDING TO RECIPIENTS CHOICE.
 *
 * The INTO-CPS toolchain  and the INTO-CPS Association Public License
 * are obtained from the INTO-CPS Association, either from the above address,
 * from the URLs: http://www.into-cps.org, and in the INTO-CPS toolchain distribution.
 * GNU version 3 is obtained from: http://www.gnu.org/copyleft/gpl.html.
 *
 * This program is distributed WITHOUT ANY WARRANTY; without
 * even the implied warranty of  MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE, EXCEPT AS EXPRESSLY SET FORTH IN THE
 * BY RECIPIENT SELECTED SUBSIDIARY LICENSE CONDITIONS OF
 * THE INTO-CPS ASSOCIATION.
 *
 * See the full INTO-CPS Association Public License conditions for more details.
 */

/*
 * Author:
 *		Kenneth Lausdahl
 *		Casper Thule
 */
package org.intocps.maestro.multimodelparser;

import java.util.Objects;

public class ModelConnection {
    public final Variable from;
    public final Variable to;

    public ModelConnection(Variable from, Variable to) {
        this.from = from;
        this.to = to;
    }

    /**
     * Parses a connection like {key}.modelname.x={key}.modelname.y where x is output and y is input
     *
     * @param connection
     * @return
     * @throws Exception
     */
    public static ModelConnection parse(String connection) throws Exception {
        String[] parts = connection.split("=");

        return new ModelConnection(Variable.parse(parts[0]), Variable.parse(parts[1]));
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }

    public static class InvalidConnectionException extends Exception {
        public InvalidConnectionException(String message) {
            super(message);
        }
    }

    public static class ModelInstance {
        public final String key;
        public final String instanceName;

        public ModelInstance(String key, String instanceName) {
            this.key = key;
            this.instanceName = instanceName;
        }

        public static ModelInstance parse(String data) throws InvalidConnectionException {
            String string = new String(data);
            // String[] parts = string.split("\\.");

            String guid = null;
            String instance = null;
            final char splitter = '.';

            int index = string.indexOf(splitter);
            if (index > 0) {
                guid = string.substring(0, index);
                instance = string.substring(index + 1);
                //				index = string.indexOf(splitter);
                //				if (index == -1)
                //				{
                //					instance = string.substring(0, index);
                //					string = string.substring(index + 1);
                //				}
                if (guid != null && instance != null) {
                    return new ModelInstance(guid, instance);
                }
            }

            throw new InvalidConnectionException("Invalid connection: " + data + " must be of the form key.instance");
        }

        @Override
        public String toString() {
            return key + "." + instanceName;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ModelInstance) {
                return key.equals(((ModelInstance) obj).key) && instanceName.equals(((ModelInstance) obj).instanceName);
            }
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            return (key + instanceName).hashCode();
        }
    }

    public static class Variable {
        public final ModelInstance instance;
        public final String variable;// this may need to be the valuereference

        public Variable(ModelInstance instance, String variable) {
            this.instance = instance;
            this.variable = variable;
        }

        public static Variable parse(String data) throws InvalidVariableStringException {
            String string = new String(data);
            // String[] parts = string.split("\\.");

            String key = null;
            String instance = null;
            final char splitter = '.';

            int index = string.indexOf(splitter);
            if (index > 0) {
                key = string.substring(0, index);
                string = string.substring(index + 1);
                index = string.indexOf(splitter);
                if (index != -1) {
                    instance = string.substring(0, index);
                    string = string.substring(index + 1);
                }
                if (key != null && instance != null && string.length() > 0) {
                    return new Variable(new ModelInstance(key, instance), string);
                }
            }

            throw new InvalidVariableStringException("Invalid connection: " + data + " must be of the form key.instance.variableName");
        }

        @Override
        public String toString() {
            return instance + "." + variable;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Variable variable1 = (Variable) o;
            return Objects.equals(instance, variable1.instance) && Objects.equals(variable, variable1.variable);
        }

        @Override
        public int hashCode() {
            return Objects.hash(instance, variable);
        }
    }
}
