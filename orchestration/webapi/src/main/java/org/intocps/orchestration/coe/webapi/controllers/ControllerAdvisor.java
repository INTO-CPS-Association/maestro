package org.intocps.orchestration.coe.webapi.controllers;


import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.webapi.services.CoeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@ControllerAdvice("org.intocps.orchestration.coe.webapi")
@Order()
@RequestMapping(produces = "application/vnd.error+json")
public class ControllerAdvisor {

    final static Logger logger = LoggerFactory.getLogger(ControllerAdvisor.class);

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<VndErrors> missingRequestParameterException(final MissingServletRequestParameterException e) {
        final String guid = java.util.UUID.randomUUID().toString();
        logger.error("API ref: " + guid, e);
        return error(e, HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private ResponseEntity<VndErrors> error(final Exception exception, final HttpStatus httpStatus, final String logRef) {
        final String message = exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName();
        return new ResponseEntity<>(new VndErrors(logRef, message), httpStatus);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<VndErrors> assertionException(final IllegalArgumentException e) {
        return error(e, HttpStatus.NOT_FOUND, e.getLocalizedMessage());
    }

    @ExceptionHandler(SimulatorNotFoundException.class)
    public ResponseEntity<VndErrors> simulatorNotFoundException(final Exception e) {
        final String guid = java.util.UUID.randomUUID().toString();
        logger.error("Internal API Exception ref: " + guid, e);
        return error(e, HttpStatus.NOT_FOUND, guid);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<VndErrors> runtimeExceptionException(final Exception e) {
        final String guid = java.util.UUID.randomUUID().toString();
        logger.error("Internal API Exception ref: " + guid, e);
        return error(e, HttpStatus.INTERNAL_SERVER_ERROR, guid);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<VndErrors> runtimeExceptionException(final IOException e) {
        final String guid = java.util.UUID.randomUUID().toString();
        logger.error("Internal API Exception ref: " + guid, e);
        return error(e, HttpStatus.INTERNAL_SERVER_ERROR, guid);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<VndErrors> unsupportedOperationExceptionException(final Exception e) {
        final String guid = java.util.UUID.randomUUID().toString();
        logger.error("Internal API Exception ref: " + guid, e);
        return error(e, HttpStatus.UNPROCESSABLE_ENTITY, guid);
    }

    @ExceptionHandler(BeanCreationException.class)
    public ResponseEntity<VndErrors> beanCreationException(final BeanCreationException e) {
        final String guid = java.util.UUID.randomUUID().toString();
        logger.error("Internal API BeanCreationException ref: " + guid, e);
        return error(e, HttpStatus.INTERNAL_SERVER_ERROR, guid);
    }

    @ExceptionHandler(CoeService.SimulatorNotConfigured.class)
    public ResponseEntity<VndErrors> simulatorNotConfigured(final CoeService.SimulatorNotConfigured e) {
        final String guid = java.util.UUID.randomUUID().toString();
        logger.error("Internal API Exception ref: " + guid, e);
        return error(e, HttpStatus.BAD_REQUEST, guid);
    }

    @ExceptionHandler(CoeService.SimulatorInputNotRegonized.class)
    public ResponseEntity<VndErrors> simulatorNotConfigured(final CoeService.SimulatorInputNotRegonized e) {
        final String guid = java.util.UUID.randomUUID().toString();
        logger.error("Internal API Exception ref: " + guid, e);
        return error(e, HttpStatus.BAD_REQUEST, guid);
    }

    @ExceptionHandler(ModelConnection.InvalidConnectionException.class)
    public ResponseEntity<VndErrors> simulatorNotConfigured(final ModelConnection.InvalidConnectionException e) {
        final String guid = java.util.UUID.randomUUID().toString();
        logger.error("Internal API Exception ref: " + guid, e);
        return error(e, HttpStatus.BAD_REQUEST, guid);
    }
}
