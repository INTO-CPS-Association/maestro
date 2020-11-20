package org.intocps.maestro.webapi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.webapi.maestro2.dto.SimulateRequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class SimulateRequestBodyConverter extends AbstractHttpMessageConverter<SimulateRequestBody> {

    @Autowired
    private MappingJackson2HttpMessageConverter springMvcJacksonConverter;

    public SimulateRequestBodyConverter() {
        super(new MediaType("text", "plain"));
    }

    @Override
    protected boolean supports(Class<?> aClass) {
        return SimulateRequestBody.class.isAssignableFrom(aClass);
    }

    @Override
    protected SimulateRequestBody readInternal(Class<? extends SimulateRequestBody> aClass,
            HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        InputStream body = httpInputMessage.getBody();
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        SimulateRequestBody simulateRequestBody = mapper.readValue(body, SimulateRequestBody.class);
        return simulateRequestBody;
    }

    @Override
    protected void writeInternal(SimulateRequestBody simulateRequestBody,
            HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {

    }
}
