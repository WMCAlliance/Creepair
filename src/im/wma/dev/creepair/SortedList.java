package im.wma.dev.creepair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class SortedList<T extends Comparable<T>> extends ArrayList<T> {

    private static final long serialVersionUID = 80176252519063325L;

    @Override
    public boolean add(T object) {
	boolean returnValue = super.add(object);
	Collections.sort(this);
	return returnValue;
    }

    @Override
    public boolean addAll(Collection<? extends T> objects) {
	boolean returnValue = super.addAll(objects);
	Collections.sort(this);
	return returnValue;
    }

    public T first() {
	return get(0);
    }

    public T last() {
	return get(size() - 1);
    }
}
