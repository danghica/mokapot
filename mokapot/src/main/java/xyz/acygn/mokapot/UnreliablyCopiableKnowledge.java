package xyz.acygn.mokapot;

/**
 * Knowledge about how to attempt to make a copy of objects of a given class.
 * The resulting copy is, in marshalled form, an <code>ObjectCopy</code> object.
 * It becomes a copy of the original object upon being unmarshalled. However,
 * when this class is used, there's reason to believe that at least one of the
 * fields will not unmarshal correctly.
 * <p>
 * This is just a thin wrapper around <code>CopiableKnowledge</code>, marking
 * the copy as unreliable.
 *
 * @author Alex Smith
 * @param <T> The class that this knowledge is about.
 */
class UnreliablyCopiableKnowledge<T> extends CopiableKnowledge<T> {

    /**
     * Create knowledge about the given class, on the assumption that it's
     * marshalled unreliably by copying.
     *
     * @param about The class that this knowledge is about.
     */
    UnreliablyCopiableKnowledge(Class<T> about) {
        super(about);
    }

    /**
     * Returns whether this class unmarshals reliably. By definition, it
     * doesn't.
     *
     * @return <code>false</code>.
     */
    @Override
    public boolean unmarshalsReliably(boolean asField) {
        return false;
    }
}
