package xyz.acygn.mokapot.examples.readyreminder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import xyz.acygn.mokapot.util.Pair;

/**
 * An <code>HTTPHandler</code> that handles the x-www-form-urlencoded format. It
 * also handles exceptions, and provides an API based on <code>String</code>
 * rather than <code>InputStream</code> and <code>OutputStream</code>.
 *
 * @author Alex Smith
 */
@FunctionalInterface
public interface XWWWFormUrlencodedHTTPHandler extends HttpHandler {

    /**
     * Handles an HTTP request encoded as <code>x-www-form-urlencoded</code>.
     * This format takes a set of key-value pairs as its input. This method does
     * not place requirements on what format is used for the output.
     *
     * @param params The parameters given in the request, a set of key/value
     * pairs.
     * @param method The method used by the request, most commonly
     * <code>GET</code> or <code>POST</code>.
     * @return A pair of {output, HTTP status code}. The output is an arbitrary
     * string of bytes, which is in turn given a format.
     * @throws Exception
     */
    public Pair<Pair<byte[], String>, Integer> process(
            Parameters params, String method) throws Exception;

    @Override
    public default void handle(HttpExchange exchange) throws IOException {
        try {
            Parameters params = new Parameters();
            Pair<Pair<byte[], String>, Integer> response;

            try {
                try (InputStream body = exchange.getRequestBody()) {
                    Scanner s = new Scanner(body).useDelimiter("\\&");

                    s.forEachRemaining((pair) -> {
                        Scanner s2 = new Scanner(pair).useDelimiter("=");
                        String key = s2.next();
                        try {
                            String value = URLDecoder.decode(s2.next(), "UTF-8");
                            params.put(key, value);
                        } catch (UnsupportedEncodingException ex) {
                            throw new HTTPResponseException(500,
                                    "Could not decode value", ex);
                        }
                    });
                }

                response = process(params, exchange.getRequestMethod());
            } catch (HTTPResponseException ex) {
                Throwable cause = ex.getCause();
                response = stringResponse(ex.getMessage() + (cause == null ? ""
                        : ": " + cause.getMessage()), ex.getResponseCode());
            } catch (Exception ex) {
                /* rethrow IOExceptions, as the HttpHandler interface expects */
                if (ex instanceof IOException) {
                    throw (IOException) ex;
                }

                /* something else went wrong = HTTP 500 */
                response = stringResponse(ex.getMessage(), 500);
            }

            if (response.getFirst().getSecond() != null) {
                /* specify the format of the reply, unless there isn't one */
                exchange.getResponseHeaders().set(
                        "Content-Type", response.getFirst().getSecond());
            }
            byte[] responseBody = response.getFirst().getFirst();
            exchange.sendResponseHeaders(response.getSecond(), responseBody.length);
            exchange.getResponseBody().write(responseBody);
        } finally {
            exchange.close();
        }
    }

    /**
     * Produces a return value for <code>process</code> indicating success, but
     * with no payload data.
     *
     * @return A suitable return value for use by <code>process</code>.
     */
    public static Pair<Pair<byte[], String>, Integer> voidResponse() {
        /* 204 = "success, no data" */
        return new Pair<>(new Pair<>(new byte[0], null), 204);
    }

    /**
     * Formats a set of key/value pairs as an appropriate return value for
     * <code>process</code>. This will be returned in the
     * <code>x-www-form-urlencoded</code> format.
     *
     * @param response The set of key/value pairs to respond with.
     * @param code The HTTP response code to use (e.g. 200 for "OK").
     * @return A suitable return value for use by <code>process</code>.
     * @throws java.io.IOException If one of the strings cannot be encoded (e.g.
     * due to containing mismatched surrogate pairs)
     */
    public static Pair<Pair<byte[], String>, Integer> keyValuePairsResponse(
            Map<String, String> response, int code) throws IOException {
        StringBuilder builtResponse = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : response.entrySet()) {
            if (first) {
                first = false;
            } else {
                builtResponse.append('&');
            }
            builtResponse.append(URLEncoder.encode(e.getKey(), "UTF-8"));
            builtResponse.append('=');
            builtResponse.append(URLEncoder.encode(e.getValue(), "UTF-8"));
        }
        return new Pair<>(new Pair<>(builtResponse.toString().getBytes("UTF-8"),
                "text/x-www-form-urlencoded; charset=UTF-8"), code);
    }

    /**
     * Formats a string as an appropriate return value for <code>process</code>.
     * This will be returned as plaintext.
     *
     * @param response The string to respond with. A newline will be appended to
     * it.
     * @param code The HTTP response code to use (e.g. 404 for "not found").
     * @return A suitable return value for use by <code>process</code>.
     * @throws java.io.IOException If the string cannot be encoded (e.g. due to
     * containing mismatched surrogate pairs)
     */
    public static Pair<Pair<byte[], String>, Integer> stringResponse(
            String response, int code) throws IOException {
        return new Pair<>(new Pair<>((response + "\r\n").getBytes("UTF-8"),
                "text/plain; charset=UTF-8"), code);
    }

    /**
     * An exception that, if thrown, causes a particular HTTP response. This can
     * be used for exceptions that are the client's fault, not the server's
     * fault, to override the default 500 response code.
     */
    public static class HTTPResponseException extends RuntimeException {

        /**
         * The HTTP code that will be used.
         */
        private final int responseCode;

        /**
         * Returns the HTTP response code to use.
         *
         * @return The response code.
         */
        public int getResponseCode() {
            return responseCode;
        }

        /**
         * Creates an HTTP response exception with a given message and response
         * code.
         *
         * @param responseCode The HTTP status code to respond with.
         * @param message The plaintext message to send. A newline will be
         * appended to it.
         */
        public HTTPResponseException(int responseCode, String message) {
            super(message);
            this.responseCode = responseCode;
        }

        /**
         * Creates an HTTP response exception wrapping a given exception, with a
         * given response code.
         *
         * @param responseCode The HTTP status code to respond with.
         * @param message The message to prefix to the exception's message.
         * @param cause The exception to wrap.
         */
        public HTTPResponseException(int responseCode,
                String message, Throwable cause) {
            super(message, cause);
            this.responseCode = responseCode;
        }
    }

    /**
     * A set of key-value pairs. This is just a
     * <code>Map&lt;String, String&gt;</code>, but contains convenience methods
     * to automatically report errors if something goes wrong.
     */
    public static class Parameters extends HashMap<String, String> {

        /**
         * Returns the value corresponding to the given key. If the key is
         * missing, this is considered an error by the user, and an HTTP 400
         * response is returned explaining this.
         *
         * @param key The key whose value should be returned.
         * @return The corresponding value, as a string.
         * @throws HTTPResponseException If the parameter is missing
         */
        public String parameter(String key) throws HTTPResponseException {
            if (containsKey(key)) {
                return get(key);
            } else {
                throw new HTTPResponseException(
                        400, "Required parameter '" + key + "' missing");
            }
        }

        /**
         * Returns the value corresponding to the given key, using a given
         * function to decode it from a string. If the key is missing, or the
         * decoding function fails, this is considered an error by the user, and
         * an HTTP 400 response is returned explaining this.
         *
         * @param <T> The type of value expected.
         * @param <X> The exception that's thrown if decoding failed. (If
         * failure does not produce an exception, this method is unnecessary;
         * just call <code>parameter</code>, then decode the result.)
         * @param key The key whose value should be returned.
         * @param decoder The method that decodes the value from a string into a
         * value of type <code>T</code>.
         * @param exception The exception thrown when decoding fails. This must
         * be given explicitly due to Java's type erasure rules.
         * @return The corresponding value, as a string.
         * @throws HTTPResponseException If the parameter is missing
         */
        public <T, X extends Exception> T decodeParameter(
                String key, ExceptionFunction<String, T, X> decoder,
                Class<X> exception) {
            String param = parameter(key);
            try {
                return decoder.apply(param);
            } catch (RuntimeException ex) {
                if (exception.isAssignableFrom(ex.getClass())) {
                    throw new HTTPResponseException(
                            400, "Could not decode parameter '" + key + "'", ex);
                }
                throw ex;
            } catch (Exception ex) {
                throw new HTTPResponseException(
                        400, "Could not decode parameter '" + key + "'", ex);
            }
        }
    }

    /**
     * Generic interface for a function that can throw an exception.
     *
     * @param <A> The function's argument.
     * @param <R> The function's return type.
     * @param <X> The type of exception the function can throw. This must be a
     * checked exception (not a <code>RuntimeException</code> or
     * <code>Error</code>).
     */
    @FunctionalInterface
    public interface ExceptionFunction<A, R, X extends Exception> {

        /**
         * decoder Applies the function to a given argument.
         *
         * @param arg The function's argument.
         * @return The function's return value, if it succeeds.
         * @throws X The exception thrown by the function, if it fails.
         */
        R apply(A arg) throws X;
    }
}
