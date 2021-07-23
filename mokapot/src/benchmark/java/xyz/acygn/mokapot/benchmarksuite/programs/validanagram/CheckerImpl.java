package xyz.acygn.mokapot.benchmarksuite.programs.validanagram;

import java.rmi.RemoteException;

/**
 * @author Kelsey McKenna
 */
public class CheckerImpl implements Checker {

    private Checker partner;
    private boolean isAnagram;

    @Override
    public void pair(Checker partner) throws RemoteException {
        this.partner = partner;
    }

    @Override
    public void unpair() throws RemoteException {
        this.partner = null;
    }

    @Override
    public void checkAnagram(final String a, final String b) throws RemoteException {
        // trivial input
        if (a.length() != b.length()) {
            isAnagram = false;
            return;
        }

        // set up buckets for counting characters
        int[] alphabet = new int[26];

        // add to buckets for first string, remove for second string
        for (char c : a.toCharArray()) alphabet[c - 97]++;
        for (char c : b.toCharArray()) {
            alphabet[c - 97]--;
            if (alphabet[c - 97] == -1) {
                isAnagram = false;
                return;
            }
        }

        // check that all buckets are empty
        for (int count : alphabet) {
            if (count != 0) {
                isAnagram = false;
                return;
            }
        }

        isAnagram = true;
    }

    @Override
    public boolean isAnagram() throws RemoteException {
        return isAnagram;
    }

    @Override
    public boolean verifyAgreement() throws RemoteException {
        return isAnagram() == partner.isAnagram();
    }

}
