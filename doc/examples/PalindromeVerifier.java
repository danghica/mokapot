import xyz.acygn.mokapot.NonCopiable;

/**
 * @author Kelsey McKenna
 */
public class PalindromeVerifier implements NonCopiable {

    boolean verify(String s) {
        return s.equals(new StringBuilder(s).reverse().toString());
    }

}
