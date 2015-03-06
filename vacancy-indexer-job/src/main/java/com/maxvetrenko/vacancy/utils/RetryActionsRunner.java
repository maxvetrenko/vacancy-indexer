package com.maxvetrenko.vacancy.utils;

import com.google.common.base.Predicate;

public final class RetryActionsRunner {

    private RetryActionsRunner() {}

    public static void executeWithRetryAttempts(ExceptionThrowingAction action, int intermediateRetryAttemptsCount,
            long timeToWaitBeforeRetryInMs, ExceptionMatcher throwableMatcher) {
        for (int i = 0; i < intermediateRetryAttemptsCount; i++) {
            try {
                action.run();
                break;
            } catch (Exception e) {
                if (throwableMatcher.apply(e)) {
                    if (i == intermediateRetryAttemptsCount - 1) {
                        action.onLastRetry(e, i + 1);
                    } else {
                        action.onIntermediateRetry(e, i + 1);
                        if (timeToWaitBeforeRetryInMs > 0) {
                            waitFor(timeToWaitBeforeRetryInMs);
                        }
                        continue;
                    }
                } else {
                    throw new RuntimeException("Launch with retry attempts failed with unexpected exception:", e);
                }
            }
        }
    }

    private static void waitFor(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException("Exception occured when waiting with Thread.sleep():", e);
        }
    }

    public interface ExceptionThrowingAction {

        void run() throws Exception;

        void onIntermediateRetry(Exception e, int currentRetryNumber);

        void onLastRetry(Exception e, int currentRetryNumber);
    }

    public static class ExceptionMatcher implements Predicate<Exception> {

        @Override
        public boolean apply(Exception e) {
            return true;
        }
    }
}
