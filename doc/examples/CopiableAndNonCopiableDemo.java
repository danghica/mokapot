
import java.io.IOException;
import java.net.InetAddress;
import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.Copiable;
import xyz.acygn.mokapot.DistributedCommunicator;
import static xyz.acygn.mokapot.DistributionUtils.isStoredRemotely;
import xyz.acygn.mokapot.NonCopiable;
import xyz.acygn.mokapot.TCPCommunicationAddress;

/**
 * @author Kelsey McKenna
 */
class CopiableAndNonCopiableDemo {

    static class ImmutableType implements Copiable {

        final int val;

        ImmutableType(int val) {
            this.val = val;
        }

        int getVal() {
            return val;
        }
    }

    static class MutableType implements NonCopiable {

        String nonPermanentVal;

        MutableType(String nonPermanentVal) {
            this.nonPermanentVal = nonPermanentVal;
        }

        String getNonPermanentVal() {
            return nonPermanentVal;
        }

        void setNonPermanentVal(String nonPermanentVal) {
            this.nonPermanentVal = nonPermanentVal;
        }

    }

    static class Mutator implements NonCopiable {

        void mutate(ImmutableType immutableType, MutableType mutableType) {
            final int val = immutableType.getVal();

            final String currentName = mutableType.getNonPermanentVal();
            mutableType.setNonPermanentVal("+" + currentName);
        }

    }

    public static void main(String[] args) throws IOException {
        DistributedCommunicator communicator
                = new DistributedCommunicator(
                        TCPCommunicationAddress.fromInetAddress(
                                InetAddress.getLoopbackAddress(), 15238));

        communicator.startCommunication();

        CommunicationAddress remoteAddress
                = TCPCommunicationAddress.fromInetAddress(InetAddress.getLoopbackAddress(), 15239);

        final Mutator remoteEngine
                = DistributedCommunicator.getCommunicator().runRemotely(Mutator::new, remoteAddress);

        assert isStoredRemotely(remoteEngine);

        final ImmutableType immutableType = new ImmutableType(5);

        final String originalName = "bob";
        final MutableType mutableType = new MutableType(originalName);

        remoteEngine.mutate(immutableType, mutableType);

        assert ("+" + originalName).equals(mutableType.getNonPermanentVal());

        DistributedCommunicator.getCommunicator().stopCommunication();
    }

}
