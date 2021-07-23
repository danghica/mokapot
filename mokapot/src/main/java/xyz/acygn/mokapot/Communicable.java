package xyz.acygn.mokapot;

/**
 * Something that it's possible to send messages to. This includes both
 * communication addresses, and less secure identifiers that are used
 * temporarily to refer to systems whose communication address is not yet known
 * (e.g. an IP address/port pair).
 * <p>
 * This interface has no code of its own. Rather, each type of communication
 * endpoint will recognise a certain set of <code>Communicable</code> instances,
 * and use methods in those. (Or in other words, endpoints can only send via
 * means of communication that they've implemented.)
 *
 * @author Alex Smith
 */
public interface Communicable {
}
