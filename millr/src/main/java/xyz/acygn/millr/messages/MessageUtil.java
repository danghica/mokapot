package xyz.acygn.millr.messages;

import javax.swing.plaf.basic.BasicTreeUI;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.*;

/**
 * Provides static methods for error handling. When any code within millr
 * catches an error and resolves to handle it rather than propagate it
 * upwards, the functionality of this class should be preferred.
 *
 * @author Marcello De Bernardi, Thomas Cuvillier
 */
public class MessageUtil {
    // whether messages printed to the user are verbose or not
    private static boolean verbose;
    private static boolean suppressWarnings;
    private static boolean suppressMessages;
    private static boolean suppressErrors;
    // logging utilities
    private static Logger logger;
    //    private static ConsoleHandler consoleHandler;
    private static FileHandler fileHandler;
    // sets for tracking emitted messages, warnings, etc
    private static Set<MillrMessageHandler> emittedMessages;
    private static Set<MillrWarningHandler> emittedWarnings;
    private static Set<MillrErrorHandler> emittedErrors;


    static {
        try {
            logger = Logger.getGlobal();
            System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");

            for (Handler handler : logger.getHandlers())
                logger.removeHandler(handler);

//            consoleHandler = new ConsoleHandler();
            fileHandler = new FileHandler("default.log", false);
            fileHandler.setLevel(Level.ALL);

//            logger.addHandler(consoleHandler);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            warning(e, "Unable to obtain logger. Logging is disabled.").emit();
        }

        emittedMessages = new HashSet<>();
        emittedWarnings = new HashSet<>();
        emittedErrors = new HashSet<>();

        setVerbose(true);
        suppressWarnings(false);
        suppressMessages(false);
    }


    /**
     * Returns whether millr has been set into verbose mode or not. In general, millr
     * should avoid printing anything to the user which isn't absolutely necessary
     * if this method returns false.
     *
     * @return true if verbose, false if terse
     */
    public static boolean isVerbose() {
        return verbose;
    }

    /**
     * Set the logging and printing mode of millr's error handling to verbose.
     * Messages printed to the user are longer and contain more details.
     */
    public static void setVerbose(boolean verbose) {
        MessageUtil.verbose = verbose;
        logger.setLevel(verbose ? Level.ALL : Level.SEVERE);
    }

    /**
     * Sets whether errors should be suppressed or not. Errors are logged even when
     * they are suppressed. This method should be used with care.
     *
     * @param suppressErrors true if errors should be suppressed, false otherwise
     */
    public static void suppressErrors(boolean suppressErrors) {
        MessageUtil.suppressErrors = suppressErrors;
    }

    /**
     * Sets whether the user has indicated that warnings should be suppressed or
     * not. Warnings are logged even when they are suppressed.
     *
     * @param suppressWarnings true if warnings should be suppressed, false otherwise
     */
    public static void suppressWarnings(boolean suppressWarnings) {
        MessageUtil.suppressWarnings = suppressWarnings;
    }

    /**
     * Sets whether the user has indicated that messages should be suppressed or
     * not. Messages are logged even when they are suppressed.
     *
     * @param suppressMessages true if messages should be suppressed, false otherwise
     */
    public static void suppressMessages(boolean suppressMessages) {
        MessageUtil.suppressMessages = suppressMessages;
    }

    /**
     * Emit a message to the user by returning a new instance of a {@link MillrMessageHandler}.
     * The returned object provides access to all the message emission facilities of millr.
     * The message to be emitted is considered important and will be emitted, unless the user
     * has explicitly suppressed messages.
     *
     * @param message message to deliver to user
     * @return new {@link MillrMessageHandler} instance
     */
    public static MillrMessageHandler message(String message) {
        return new MillrMessageHandler(message, false);
    }

    /**
     * Emit a message to the user by returning a new instance of {@link MillrMessageHandler}.
     * The returned object provides access to all the message emission facilities of millr.
     * The second parameter specifies whether the message is considered important or not;
     * if it is not, it will only be printed if the user has turned on the 'verbose' option.
     * <p>
     * The 'suppress messages' option overrides importance. An important message will not be
     * printed is messages are suppressed.
     *
     * @param message message to emit
     * @return new {@link MillrMessageHandler} instance
     */
    public static MillrMessageHandler message(String message, Level l) {
        if (l.equals(Level.INFO) || l.equals(Level.CONFIG) || l.equals(Level.FINE) || l.equals(Level.FINER) || l.equals(Level.FINEST) || l.equals(Level.OFF))
            return new MillrMessageHandler(message, false);
        else
            return new MillrMessageHandler(message, true);
    }

    /**
     * Emit a message to the user by returning a new instance of {@link MillrMessageHandler}.
     * The returned object provides access to all the message emission facilities of millr.
     * The second parameter specifies whether the message is considered important or not;
     * if it is not, it will only be printed if the user has turned on the 'verbose' option.
     * <p>
     * The 'suppress messages' option overrides importance. An important message will not be
     * printed is messages are suppressed.
     *
     * @param message   message to emit
     * @param important true if message is important, false otherwise
     * @return new {@link MillrMessageHandler} instance
     */
    public static MillrMessageHandler message(String message, boolean important) {
        return new MillrMessageHandler(message, important);
    }


    /**
     * Emit a message to the user by returning a new instance of a {@link MillrWarningHandler}.
     * The returned object provides access to all the message emission facilities of millr.
     * Use of this method must be restricted for messages coming from outside processes. For internal warnings,
     * use of warning(Exception e) must be preferred.
     *
     * @param warning message to emit to user
     * @return new {@link MillrWarningHandler} instance
     */
    public static MillrWarningHandler warning(String warning) {
        return new MillrWarningHandler(warning);
    }

    /**
     * Emit a message to the user by returning a new instance of a {@link MillrWarningHandler}.
     * The returned object provides access to all the message emission facilities of millr.
     * Use of this method must be restricted for messages coming from outside processes. For internal warnings,
     * use of warning(Exception e) must be preferred.
     *
     * @param warning message to emit to user
     * @return new {@link MillrWarningHandler} instance
     */
    public static MillrWarningHandler warning(Throwable e, String warning) {
        return new MillrWarningHandler(e, warning);
    }

    /**
     * Emit a warning to the user by returning a new instance of a {@link MillrWarningHandler}.
     * The returned object provides access to all the message emission facilities of millr.
     *
     * @param exception an exception containing a warning for the user
     * @return new {@link MillrWarningHandler} instance
     */
    public static <T extends Throwable> MillrWarningHandler warning(T exception) {
        return new MillrWarningHandler(exception);
    }


    /**
     * Entry point for millr's error handling. Returns a {@link MillrErrorHandler}
     * constructed from the exception being handled.
     *
     * @param exception exception to handle
     * @param <T>       type of the exception
     * @return a {@link MillrErrorHandler} to handle the exception
     */
    public static <T extends Throwable> MillrErrorHandler<T> error(T exception) {
        return new MillrErrorHandler<>(exception);
    }

        /**
         * Entry point for millr's error handling. Returns a {@link MillrErrorHandler}
         * constructed from the exception being handled.
         *
         * @param exception exception to handle
         * @param <T>       type of the exception
         * @return a {@link MillrErrorHandler} to handle the exception
         */
        public static <T extends Throwable> MillrErrorHandler<T> error(T exception, String message) {
            return new MillrErrorHandler<>(exception, message);
        }


    /**
     * A handler object providing access to messaging functionality. By default messages
     * are printed to STDOUT.
     *
     * @author Marcello De Bernardi
     */
    public static class MillrMessageHandler {
        private String message;
        private boolean important;


        private MillrMessageHandler(String message, boolean important) {
            this.message = message;
            this.important = important;
        }


        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MillrMessageHandler) {
                return this.message.equals(((MillrMessageHandler) obj).message);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return message.hashCode();
        }

        public void emit() {
            // only print if not emitted before and messages are not suppressed
            if (emittedMessages.add(this) && !suppressMessages && (verbose || important))
                logger.log(important? Level.SEVERE : Level.INFO, this.message);
        }
    }


    /**
     * A handler object providing access to message functionality. By default warnings
     * are printed to STDERR.
     */
    public static class MillrWarningHandler<T extends Throwable> {
        private String warning;
        private Throwable exception;


        private MillrWarningHandler(String warning) {
            this.warning = warning;
            this.exception = new Exception();
        }

        private MillrWarningHandler( T exception, String warning) {
            this.warning = warning;
            this.exception = exception;
        }

        private MillrWarningHandler(T exception) {
            this.exception = exception;
            this.warning = "";
        }


        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MillrWarningHandler) {
                return this.warning.equals(((MillrWarningHandler) obj).warning) && this.exception.equals(((MillrWarningHandler) obj).exception);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return ((warning==null)?  0 : warning.hashCode()) + ((exception==null)? 0 : exception.hashCode());
        }

        public void emit() {
            // only print if not emitted before and warnings are not suppressed
            if (emittedWarnings.add(this) && !suppressWarnings) {
                synchronized (MessageUtil.class) {
                    logger.warning(warning);
                    if (verbose) {
                        this.report();
                    }
                }
            }
        }

        private MillrWarningHandler<T> report() {
            synchronized (MessageUtil.class) {
                exception.printStackTrace();
            }
                //if (exception.getCause() != null) exception.getCause().printStackTrace();
                //System.err.print("\n");

                // pass stacktrace to logger at fine level
                // so it doesn't get printed again
                StringBuilder stacktrace = new StringBuilder();
                for (StackTraceElement elem : exception.getStackTrace())
                    stacktrace.append(elem.toString()).append("\n");
                logger.fine(stacktrace.toString());
            return this;

        }
    }


    /**
     * An error handler wrapping the exception being handled. Error handlers by default
     * print to the standard error stream.
     */
    public static class MillrErrorHandler<T extends Throwable> {
        private T cause;
        private String message;


        /**
         * Build an error handler for the given exception.
         *
         * @param exception the exception to handle
         */
        private MillrErrorHandler(T exception) {
            cause = exception;
            this.message = "";
        }

        /**
         * Build an error handler for the given exception.
         *
         * @param exception the exception to handle
         */
        private MillrErrorHandler(T exception,String message) {
            cause = exception;
            this.message = message;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof MillrErrorHandler
                    && this.cause.getMessage() != null
                    && this.cause.getMessage().equals(((MillrErrorHandler) obj).cause.getMessage());
        }

        @Override
        public int hashCode() {
            return message.hashCode();
        }

        /**
         * Provides the user with a message on the error. The message printed is the caught
         * exception's message plus its stack trace.
         *
         * @return this ErrorHandler
         */
        public MillrErrorHandler<T> report() {
            if (emittedErrors.add(this)) {
                synchronized (MessageUtil.class) {
                    logger.severe(message);
                    Throwable tempCause = cause;
                    do {
                        tempCause.printStackTrace();
                        System.err.print("\n");
                        tempCause = tempCause.getCause();
                    }while (tempCause != null);


                    // pass stacktrace to logger at fine level
                    // so it doesn't get printed again
                    StringBuilder stacktrace = new StringBuilder();
                    tempCause = cause;
                    while (tempCause != null) {
                        for (StackTraceElement elem : tempCause.getStackTrace()) {
                            stacktrace.append(elem.toString()).append("\n");
                        }
                        tempCause = tempCause.getCause();
                    }
                    logger.fine(stacktrace.toString());
                }
            }

            return this;
        }

        /**
         * Provides the user with a message on the error. The message printed is the caught
         * exception's message plus its stacktrace, as well as the additional message passed
         * as an argument to this method.
         *
         * @return this ErrorHandler
         */
        public MillrErrorHandler<T> report(String additionalMessage) {
            message = additionalMessage;
            report();

            return this;
        }

        /**
         * Executes arbitrary error-handling code provided by the user on the exception
         * object encapsulated by this ErrorHandler. The code is of the form
         * {@code (error) -> { ... }}
         *
         * @param function function to execute on the exception
         * @return this ErrorHandler
         */
        public MillrErrorHandler<T> execute(ErrorHandlingFunction<T> function) {
            function.apply(cause);
            logger.info("executing error handling function " + function.toString());

            return this;
        }

        /**
         * Conditional termination operation, for cases where the exception is
         * considered fatal if some predicate is true.
         */
        public void terminateIf(Predicate<T> predicate) {
            logger.info("testing error fatality at terminateIf() " + predicate.toString());

            if (predicate.test(cause)) terminate();
            else resume();
        }

        /**
         * Terminal error-handling operation, for cases where the exception is
         * considered fatal to the running of millr. Information on the error is
         * logged, and a stack trace is printed to the user.
         */
        public void terminate() {
            logger.severe("terminating millr.\n");
            System.exit(1);
        }

        /**
         * Indicates to the error handling framework that the exception has been dealt
         * with in such a manner that millr may continue operating. Information on the
         * exception is logged.
         */
        public void resume() {
            logger.info("resuming after non-fatal error.\n");
        }
    }
}