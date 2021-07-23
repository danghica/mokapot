package xyz.acygn.mokapot;

import java.time.Duration;

/**
 * A distributed message with no immediate effect. The only purpose of this is
 * for the sender and recipient to be able to verify the identity of each other.
 *
 * @author Alex Smith
 */
class IdentityMessage extends SynchronousMessage<Void> {

    @Override
    protected Void calculateReply() {
        return null;
    }

    @Override
    public Duration periodic() {
        return null;
    }
}
