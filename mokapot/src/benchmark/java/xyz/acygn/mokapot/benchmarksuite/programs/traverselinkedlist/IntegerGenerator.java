package xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist;

import java.util.Random;

public class IntegerGenerator implements Generator<Integer> {

    /**
     * Generates a linked list of integers for testing.
     *
     * @return a linked list of integers
     */
    public Cell<Integer> generateTestList(int listSize) {
        final Random rng = new Random(0); // seeded for reproducibility
        final CellImpl<Integer> head = new CellImpl<>(0, null);
        CellImpl<Integer> pointer = head;

        for (int i = 0; i < listSize; i++) {
            pointer.setNext(new CellImpl<>(rng.nextInt(500), null));
            pointer = (CellImpl<Integer>) pointer.next();
        }

        return head;
    }

}
