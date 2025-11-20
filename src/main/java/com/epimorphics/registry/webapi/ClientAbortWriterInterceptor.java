package com.epimorphics.registry.webapi;

import jakarta.annotation.Priority;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Provider @Priority(11)
public class ClientAbortWriterInterceptor implements WriterInterceptor {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        try {
            context.proceed();
        } catch (Exception e) {
            if (e.getCause() instanceof ClientAbortException) {
                log.warn("Client interrupted response writer.", e);
                context.getOutputStream().close();
                context.setOutputStream(new ByteArrayOutputStream());
            } else {
                throw e;
            }
        }
    }
}
