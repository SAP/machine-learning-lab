package org.mltooling.core.service.utils;

import org.mltooling.core.api.format.parser.JsonFormatParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class GsonBodyReader implements MessageBodyReader<Object> {

    private static final Logger log = LoggerFactory.getLogger(GsonBodyReader.class);

    private static final String UTF_8 = "UTF-8";

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
                              java.lang.annotation.Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType,
                           Annotation[] annotations, MediaType mediaType,
                           MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException {
        InputStreamReader streamReader = new InputStreamReader(entityStream, UTF_8);
        try {
            return JsonFormatParser.INSTANCE.getGson().fromJson(streamReader, genericType);
        } catch (com.google.gson.JsonSyntaxException e) {
            log.error("Json syntax error while reading json body", e);
        } finally {
            streamReader.close();
        }
        return null;
    }
}