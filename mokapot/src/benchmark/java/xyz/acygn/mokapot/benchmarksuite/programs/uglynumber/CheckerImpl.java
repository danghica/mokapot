package xyz.acygn.mokapot.benchmarksuite.programs.uglynumber;

/**
 * @author Kelsey McKenna
 */
public class CheckerImpl implements Checker {

    public boolean isUgly(int num) {
        if (num < 1) return false;

        num = removeFactor(removeFactor(removeFactor(num, 2), 3), 5);

        return num == 1;
    }

    /* Removes the given factor from the number's prime factorization */
    private int removeFactor(int number, int factor) {
        while (number % factor == 0) number /= factor;
        return number;
    }

}
