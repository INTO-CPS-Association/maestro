package org.intocps.maestro.webapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.webapi.maestro2.dto.InitializationData;
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
public class InitializationDataConverter extends AbstractHttpMessageConverter<InitializationData> {

    @Autowired
    private MappingJackson2HttpMessageConverter springMvcJacksonConverter;

    public InitializationDataConverter() {
        super(new MediaType("text", "plain"));
    }

    @Override
    protected boolean supports(Class<?> aClass) {
        return InitializationData.class.isAssignableFrom(aClass);
    }

    @Override
    protected InitializationData readInternal(Class<? extends InitializationData> aClass,
            HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        InputStream body = httpInputMessage.getBody();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(body, InitializationData.class);
    }

    @Override
    protected void writeInternal(InitializationData initializationData,
            HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {

    }
}