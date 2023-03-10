package xyz.acygn.millr.mokapotsemantics;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Predicate;


/**
 * A snapshot is a representation of the state of a program at a given time.
 * The representation is limited by the fact that some of the remote states might not be accessible, and
 * hence can not be registered in the Snapshot. Specifically, the remote fields that do not have
 * appropriate getters cannot be accessed.
 * The snapshot is made in such a way that arrays and arrayWrapper are registered equally, and it does not take
 * into account additional fields added by millr.
 * This way, it can provides a useful comparison for milled programs and unmilled programs.
 */
class Snapshot {


    public Snapshot(boolean isRemote) {
        this.isRemote = isRemote;
        listRoots = new LinkedList<>();
    }

    private boolean isRemote;


    /**
     * A comparison between two Snapshots.
     * It will return false if two elements of the snapshots differed. In which case, it might be useful to call
     * the highlightDifference method to know where the differences come from.
     *
     * @param otherSnapShot
     * @return
     */
    boolean compareTo(Snapshot otherSnapShot) {
        if (listRoots.size() != otherSnapShot.listRoots.size()) {
            return false;
        }
        Iterator<Node> iteratorOne = listRoots.listIterator();
        Iterator<Node> iteratorTwo = otherSnapShot.listRoots.listIterator();
        Set<Node> ExploredNodes = new HashSet<>();
        BinaryRelationEqual<Node> equalityRelation = new BinaryRelationEqual<>();
        while (iteratorOne.hasNext()) {
            if (!areCompatible(iteratorOne.next(), iteratorTwo.next(), equalityRelation, ExploredNodes)) return false;
        }
        return true;
    }


    class BinaryRelationEqual<T> {

        class Pair<T> {
            T left;
            T right;

            Pair(T one, T two) {
                left = one;
                right = two;
            }

            @Override
            public boolean equals(Object o) {
                return (o != null) & (o instanceof Pair) & left.equals(((Pair) o).left) & right.equals(((Pair) o).right);
            }

            @Override
            public int hashCode() {
                return ((left == null) ? 0 : left.hashCode()) ^ ((right == null) ? 0 : right.hashCode());
            }

            boolean contains(T arg) {
                return (arg == null) ? (left == null || right == null) : arg.equals(left) || arg.equals(right);
            }
        }

        final Set<Pair<T>> equalityRelation = new HashSet<>();

        boolean areEqual(T one, T two) {
            return (one == null) ? one == two : equalityRelation.stream().anyMatch(s -> s.contains(one) & s.contains(two));
        }

        void putEqual(T one, T two) {
            equalityRelation.add(new Pair<T>(one, two));
        }


    }


    private Map<Object, Node> fromObjectToNode = new IdentityHashMap<>();
    private List<Node> listRoots;


    private boolean areCompatible(Node nodeOne, Node nodeTwo, BinaryRelationEqual<Node> binaryRelationEqual, Set<Node> exploredNodes) {
        if (binaryRelationEqual.areEqual(nodeOne, nodeTwo)) {
            return true;
        }
        if (exploredNodes.contains(nodeOne) ^ exploredNodes.contains(nodeTwo)) {
            return false;
        }
        if (exploredNodes.contains(nodeOne) & exploredNodes.contains(nodeTwo)) {
            return true;
        }
        exploredNodes.add(nodeOne);
        exploredNodes.add(nodeTwo);
        if (nodeOne == null || nodeTwo == null) {
            throw new RuntimeException();
        }
        if (nodeOne == Node.UNINITIALIZEDNODE || nodeTwo == Node.UNINITIALIZEDNODE) {
            if (nodeOne == nodeTwo) {
                binaryRelationEqual.putEqual(nodeOne, nodeTwo);
                return true;
            }
            return false;
        }
        if (nodeOne == Node.STOPEXPLORENODE || nodeTwo == Node.STOPEXPLORENODE) {
            binaryRelationEqual.putEqual(nodeOne, nodeTwo);
            return true;
        } else {
            if (nodeOne.nodeValue != null) {
                if (nodeOne.nodeValue.equals(nodeTwo.nodeValue)) {
                    binaryRelationEqual.putEqual(nodeOne, nodeTwo);
                    return true;
                }
                return false;
            }
            if (nodeOne.getChild().size() != nodeTwo.getChild().size()) {
                System.err.println("Node Two: " + nodeOne.nodeValue + "\n");
                nodeOne.getChild().stream().forEach(e -> System.out.println(e.getKey()));
                System.err.println("Node Two: " + nodeTwo.nodeValue + "\n");
                nodeTwo.getChild().stream().forEach(e -> System.out.println(e.getKey()));
                System.out.println("insn");
                throw new RuntimeException();
            }
            Iterator<Map.Entry<String, Node>> iterator = nodeOne.getChild().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Node> entry = iterator.next();
                Node node = nodeTwo.childrenFields.get(entry.getKey());
                if (node == null) {
                    //  System.out.println("insn");
                    throw new RuntimeException();
                }
                if (!areCompatible(nodeOne, nodeTwo, binaryRelationEqual, exploredNodes)) return false;
            }
            binaryRelationEqual.putEqual(nodeOne, nodeTwo);
            return true;
        }
    }


    static class Node {
        /**
         * Special node that have no childs, nor value, that informs us that we the Exploration stopped.
         * The use case is an object that is not milled when the program makes use of mokapot (that is, the
         * boolean isRemote is true).
         */
        static final Node STOPEXPLORENODE = new Node();

        static final Node UNINITIALIZEDNODE = new Node();

        static final Node NULLNODE = new Node();

        /**
         * Node constructor.
         */
        public Node() {
            childrenFields = new HashMap<>();
        }

        /**
         * The string are the name of the fields, or the reference inside the array, and the nodes are the childs.
         */
        HashMap<String, Node> childrenFields;

        Collection<Map.Entry<String, Node>> getChild() {
            return childrenFields.entrySet();
        }

        /**
         * The Value of the node;
         */
        String nodeValue;

        void registerField(FieldComponent component, Node node) {
            childrenFields.put(component.getName(), node);
        }


        Node(String nodeValue) {
            this();
            this.nodeValue = nodeValue;
        }


        /**
         * Node are compared using reference equality.
         *
         * @param o
         * @return
         */
        @Override
        public boolean equals(Object o) {
            return o == this;
        }


    }

    private Node getNode(FieldComponent component, boolean isRemote) {
        if (!component.isInitialized()) return Node.UNINITIALIZEDNODE;
        if (!component.hasTargetGetter() & isRemote) {
            return Node.STOPEXPLORENODE;
        }
        return getNode(component.getValue(), isRemote);
    }


    private Node getNode(WeakObject obj, boolean isRemote) {
        if (obj.isNull()) return Node.NULLNODE;
        if (fromObjectToNode.containsKey(obj.getValue())) {
            return fromObjectToNode.get(obj.getValue());
        }
        Node node;
        if (obj.isPrintable() || obj.isArrayPrintable()) {
            node = new Node(obj.toString());
        } else {
            node = new Node();
        }
        fromObjectToNode.put(obj.getValue(), node);
        return node;
    }


    public void registerField(FieldComponent component, boolean isRemote) {
        if (component.isComingFromMillr() || component.isComingFromMokapot()) return;
        FieldComponent componentClone = component;
        Node parentNode = fromObjectToNode.get(component.getTarget());
        if (parentNode == null) {
            System.out.println("error");
            throw new RuntimeException("Parent Class Name : " + component.getTarget().getClass().getName()
                    + " \n"
                    + "field Name " + component.getName());
        }
        parentNode.registerField(component, getNode(component, isRemote));

    }

    public void registerArrayElement(Object Array, int indexElement, boolean isRemote) {
        registerField(new FieldComponent(Array, indexElement), isRemote);
    }


    /**
     * Exception when trying to read from a field that we were not suppose to explore.
     * At the moment, 3 cases are dealt with: We trying to access a null field, the field has been
     * added by millr, the field is  coming from mokapot (fields of the DistributedCommunicator...)
     * Should be called through the factory method, that will examine what is wrong with the field and trying
     * to report an adequate message to the user.
     */
    static class UnexpectedFieldException extends Exception {

        public static UnexpectedFieldException getUnexpectedFieldException(FieldComponent field) {
            if (field == null)
                return new UnexpectedFieldException("Unexpected Field Exception : The field is null");
            if (field.isComingFromMillr())
                return new UnexpectedFieldException("Unexpected Field Exception : The field is coming from millr"
                        + field.toString());
            if (field.isComingFromMokapot()) {
                return new UnexpectedFieldException("Unexpected Field Exception : The field is coming from mokapot "
                        + field.toString());
            }
            return new UnexpectedFieldException("Unexpected Field Exception: the field " + field.toString());
        }

        private UnexpectedFieldException(String message) {
            super(message);
        }

    }


    /**
     * Getting all the fields from an object o, that satisfy the default Filter predicate.
     *
     * @param o The object.
     * @return All the fields recursively reachable from o.
     * @throws UnexpectedFieldException
     * @throws IllegalAccessException
     */
    private void registerAllFields(Object o) throws UnexpectedFieldException, IllegalAccessException,
            InvocationTargetException {
        Set<Object> exploredObjects = new HashSet<>();
        registerFields(o, exploredObjects, true);
    }

    private void registerFields(Object o) throws UnexpectedFieldException, IllegalAccessException, InvocationTargetException {
        Set<Object> exploredObjects = new HashSet<>();
        registerFields(o, exploredObjects, false);
    }

    void registerAllObject(Object o) throws UnexpectedFieldException, IllegalAccessException, InvocationTargetException {
        WeakObject weakO = new WeakObject(o);
        if (weakO.isNull()) {
            return;
        }
        Node node = getNode(weakO, isRemote);
        fromObjectToNode.put(weakO.getValue(), node);
        listRoots.add(node);
        registerAllFields(o);
    }

    void registerObject(Object o) throws UnexpectedFieldException, IllegalAccessException, InvocationTargetException {
        WeakObject weakO = new WeakObject(o);
        if (weakO.isNull()) {
            return;
        }
        Node node = getNode(weakO, isRemote);
        fromObjectToNode.put(weakO.getValue(), node);
        listRoots.add(node);
        registerFields(o);
    }


    Predicate<FieldComponent> defaultFilter = fieldComponent -> {
        return fieldComponent.isInitializedWithNonNull() &&
                !fieldComponent.getValue().isArrayPrintable() && !fieldComponent.getValue().isPrintable()
                && ((isRemote) ? fieldComponent.hasTargetGetter() : true);
    };


    /**
     *
     */
    private void registerFields(Object o, Set<Object> exploredObjects, boolean recursive) throws UnexpectedFieldException, IllegalAccessException, InvocationTargetException {
        registerFields(o, exploredObjects, recursive, defaultFilter);
    }

    /**
     * Retrieve all the FieldsComponent that are printable recursively from o, skipping the exploredObjects.
     * Throws exception in the following cases:
     * - o is an array of primitive types.
     *
     * @param o               The object to be explored.
     * @param exploredObjects The objects already explored.
     * @param recursive       If the fields should be explored recursively.
     * @param filter          A filter that sorts the fields we would like to consider.
     * @return The fields that are printable.
     * @throws UnexpectedFieldException
     * @throws IllegalAccessException
     */
    private void registerFields(Object o, Set<Object> exploredObjects, boolean recursive, Predicate<FieldComponent> filter) throws UnexpectedFieldException, IllegalAccessException, InvocationTargetException {
        if (o == null) {
            return;
        }
        if (exploredObjects.contains(o)) {
            return;
        }
        exploredObjects.add(o);
        if (o.getClass().isArray()) {
            if (o.getClass().getComponentType().isPrimitive()) {
                System.out.println("insn");
                throw UnexpectedFieldException.getUnexpectedFieldException(null);
            }
            Object[] values = (Object[]) o;
            for (int i = 0; i < values.length; i++) {
                FieldComponent component = new FieldComponent(o, i);
                if (!component.shouldNotBeExplored()) {
                    registerArrayElement(o, i, isRemote);
                    if (filter.test(component)) {
                        registerFields(component.getValue().getValue(), exploredObjects, recursive);
                    }
                }
            }
        } else
            if (!isRemote || new WeakObject(o).hasGetter()) {
                for (Field field : ReflectionUtilsMillr.getAllFields(o.getClass())) {
                    FieldComponent component = new FieldComponent(field, o);
                    if (!component.shouldNotBeExplored()) {
                        registerField(component, isRemote);
                        if (filter.test(component) && recursive) {
                            registerFields(component.getValue().getValue(), exploredObjects, recursive);
                        }
                    }
                }
            }
            return;
        }

        @Override
        public String toString () {
            return toString(new HashSet<>());

        }

        public String toString (Set < Node > exploredNode) {
            StringBuilder str = new StringBuilder();
            int i = 0;
            for (Node root : listRoots) {
                str.append(" root ").append(String.valueOf(i)).append(" : ").append(toString(root, exploredNode));
                i++;
            }
            return str.toString();
        }

        private String toString (Node node, Set < Node > exploredNode){
            if (exploredNode.contains(node)) {
                return "loop";
            }
            if (node == Node.NULLNODE) return "null \n ";
            if (node == Node.UNINITIALIZEDNODE) return "unintialized node \n ";
            if (node == Node.STOPEXPLORENODE) return "ExplorationStoped \n ";
            exploredNode.add(node);
            if (node.nodeValue != null) return "Value " + node.nodeValue + "\n";
            StringBuilder str = new StringBuilder();
            for (Map.Entry<String, Node> child : node.getChild()) {
                str.append(child.getKey()).append(" : ").append(toString(child.getValue(), exploredNode));
            }
            return str.toString();
        }
    }


