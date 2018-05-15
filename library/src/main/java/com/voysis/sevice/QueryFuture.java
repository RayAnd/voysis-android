package com.voysis.sevice;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class QueryFuture implements Future<String> {

    private final Holder<Exception> exceptionHolder = new Holder<>();
    private final Holder<String> responseHolder = new Holder<>();
    private volatile boolean cancelled;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized (responseHolder) {
            cancelled = true;
            setException(new CancellationException());
            return cancelled;
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        synchronized (responseHolder) {
            return cancelled || responseHolder.hasValue() || exceptionHolder.hasValue();
        }
    }

    @Override
    public String get() throws InterruptedException, ExecutionException, CancellationException {
        return get(0);
    }

    @Override
    public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return get(unit.toMillis(timeout));
    }

    private String get(long timeoutInMills) throws InterruptedException, ExecutionException {
        synchronized (responseHolder) {
            if (!isDone()) {
                responseHolder.wait(timeoutInMills);
            }
            if (exceptionHolder.hasValue()) {
                throw new ExecutionException(exceptionHolder.getValue());
            }
        }
        return responseHolder.getValue();
    }

    /**
     * Sets response and notifies users waiting on response.
     * @param response json response
     */
    public void setResponse(String response) {
        synchronized (responseHolder) {
            responseHolder.setValue(response);
            responseHolder.notifyAll();
        }
    }

    /**
     * Sets exception and notifies users.
     * @param exception exception
     */
    public void setException(Exception exception) {
        synchronized (responseHolder) {
            exceptionHolder.setValue(exception);
            responseHolder.notifyAll();
        }
    }

    private static class Holder<T> {

        private T value;

        T getValue() {
            return value;
        }

        void setValue(T value) {
            this.value = value;
        }

        boolean hasValue() {
            return value != null;
        }
    }
}
