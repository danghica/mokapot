package xyz.acygn.mokapot.benchmarksuite.programs.createcollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

/**
 * @author Marcello De Bernardi
 */
public class GeneratorImpl implements Generator {

    @Override
    public ICollection<ExampleObject> generate(int seed, int maxCollectionSize) {
        Random rng = new Random(seed);
        Collection<ExampleObject> newCollection = new ArrayList<>();

        for (int i = 0; i < rng.nextInt(maxCollectionSize); i++) {
            newCollection.add(new ExampleObject("object " + i, rng.nextInt(5)));
        }

        return new CollectionImpl<>(newCollection);
    }

}
