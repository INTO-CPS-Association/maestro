package org.intocps.maestro.webapi.maestro2.interpreter;

import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.csv.CsvFileValue;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

class WebSocketSenderValue extends ExternalModuleValue<WebsocketValueConverter> {


    public WebSocketSenderValue(WebSocketSession ws) {
        super(createMembers(new WebsocketValueConverter(ws)), null);

    }

    static private Map<String, Value> createMembers(WebsocketValueConverter ws) {
        Map<String, Value> members = new HashMap<>();

        members.put("writeHeader", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 1);

            List<StringValue> headers = CsvFileValue.getArrayValue(fcargs.get(0), StringValue.class);


            ws.configure(headers.stream().map(StringValue::getValue).collect(Collectors.toList()));

            return new VoidValue();
        }));
        members.put("writeRow", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 2);

            double time = CsvFileValue.getDouble(fcargs.get(0));

            ArrayValue<Value> arrayValue = (ArrayValue<Value>) fcargs.get(1).deref();

            List<Object> data = new Vector<>();

            for (Value d : arrayValue.getValues()) {
                if (d instanceof IntegerValue) {
                    data.add(((IntegerValue) d).intValue());
                }
                if (d instanceof RealValue) {
                    data.add(((RealValue) d).realValue());
                }
                if (d instanceof BooleanValue) {
                    data.add(Boolean.valueOf(((BooleanValue) d).getValue()).toString());
                }
            }

            ws.update(time, data);
            ws.send();

            return new VoidValue();
        }));

        return members;
    }


}
