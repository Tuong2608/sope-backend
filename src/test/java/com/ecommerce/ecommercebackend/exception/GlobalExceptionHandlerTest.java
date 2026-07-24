package com.ecommerce.ecommercebackend.exception;

import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void clientAbortHandlersDoNotAttemptToCreateAnotherResponse() {
        assertThatCode(() -> handler.handleClientDisconnect(
                new ClientAbortException("Broken pipe"))).doesNotThrowAnyException();
        assertThatCode(() -> handler.handleClientDisconnect(
                new AsyncRequestNotUsableException("response already unusable")))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleIOException(
                new IOException("Connection reset by peer"))).doesNotThrowAnyException();
        assertThat(handler.handleMessageNotWritable(
                new HttpMessageNotWritableException(
                        "ServletOutputStream failed to write",
                        new ClientAbortException("Broken pipe"))))
                .isNull();
    }

    @Test
    void unrelatedIoExceptionIsNotSilentlySwallowed() {
        IOException exception = new IOException("disk error");
        assertThatThrownBy(() -> handler.handleIOException(exception))
                .isSameAs(exception);
    }
}
