package io.realm;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

import io.realm.internal.OsSet;

/**
 * TODO
 *
 * @param <E>
 */
abstract class SetValueOperator<E> {

    protected final BaseRealm baseRealm;
    protected final OsSet osSet;
    protected final Class<E> valueClass;

    public SetValueOperator(BaseRealm baseRealm, OsSet osSet, Class<E> valueClass) {
        this.baseRealm = baseRealm;
        this.osSet = osSet;
        this.valueClass = valueClass;
    }

    abstract boolean add(@Nullable E value);

    abstract boolean contains(@Nullable Object o);

    abstract boolean remove(@Nullable Object o);

    abstract boolean containsAll(Collection<?> c);

    abstract boolean addAll(Collection<? extends E> c);

    abstract boolean retainAll(Collection<?> c);

    abstract boolean removeAll(Collection<?> c);

    boolean isValid() {
        if (baseRealm.isClosed()) {
            return false;
        }
        return osSet.isValid();
    }

    boolean isFrozen() {
        return baseRealm.isFrozen();
    }

    int size() {
        return Long.valueOf(osSet.size()).intValue();
    }

    boolean isEmpty() {
        return size() == 0;
    }

    Iterator<E> iterator() {
        return new SetIterator<>(osSet, baseRealm);
    }

    Object[] toArray() {
        // TODO
        return new Object[0];
    }

    <T> T[] toArray(T[] a) {
        // TODO
        return null;
    }

    public void clear() {
        osSet.clear();
    }

    public RealmSet<E> freeze() {
        BaseRealm frozenRealm = baseRealm.freeze();
        OsSet frozenOsSet = osSet.freeze(frozenRealm.sharedRealm);
        return new RealmSet<>(frozenRealm, frozenOsSet, valueClass);
    }

    OsSet getOsSet() {
        return osSet;
    }

    @SuppressWarnings("unchecked")
    protected boolean isRealmCollection(Collection<?> c) {
        // TODO: add support for RealmList and RealmResults when overloading is exposed by OS/Core
        return c instanceof RealmSet && ((RealmSet<? extends E>) c).isManaged();
    }

    protected boolean collectionFunnel(OsSet otherOsSet,
                                       OsSet.ExternalCollectionOperation operation) {
        // Special case if the passed collection is the same native set as this one
        if (osSet.getNativePtr() == otherOsSet.getNativePtr()) {
            switch (operation) {
                case CONTAINS_ALL:
                    // A set always contains itself
                    return true;
                case ADD_ALL:
                    // Nothing changes if we add this set to this very set
                    return false;
                case REMOVE_ALL:
                    // Clear and return true if the passed collection is this very set
                    osSet.clear();
                    return true;
                case RETAIN_ALL:
                    // Nothing changes if this set intersects this very set
                    return false;
                default:
                    throw new IllegalStateException("Unexpected value: " + operation);
            }
        }

        // Otherwise compute set-specific operation
        switch (operation) {
            case CONTAINS_ALL:
                return osSet.containsAll(otherOsSet);
            case ADD_ALL:
                return osSet.union(otherOsSet);
            case REMOVE_ALL:
                return osSet.asymmetricDifference(otherOsSet);
            case RETAIN_ALL:
                return osSet.intersect(otherOsSet);
            default:
                throw new IllegalStateException("Unexpected value: " + operation);
        }
    }
}

/**
 * TODO
 */
class StringOperator extends SetValueOperator<String> {

    public StringOperator(BaseRealm baseRealm, OsSet osSet, Class<String> valueClass) {
        super(baseRealm, osSet, valueClass);
    }

    @Override
    boolean add(@Nullable String value) {
        return osSet.add(value);
    }

    @Override
    boolean contains(@Nullable Object o) {
        return osSet.contains((String) o);
    }

    @Override
    boolean remove(@Nullable Object o) {
        return osSet.remove((String) o);
    }

    @Override
    boolean containsAll(Collection<?> c) {
        if (isRealmCollection(c)) {
            OsSet otherOsSet = ((RealmSet<?>) c).getOsSet();
            return collectionFunnel(otherOsSet, OsSet.ExternalCollectionOperation.CONTAINS_ALL);
        }
        return osSet.containsAllString(c);
    }

    @Override
    boolean addAll(Collection<? extends String> c) {
        if (isRealmCollection(c)) {
            OsSet otherOsSet = ((RealmSet<?>) c).getOsSet();
            return collectionFunnel(otherOsSet, OsSet.ExternalCollectionOperation.ADD_ALL);
        }
        return osSet.addAllString(c);
    }

    @Override
    boolean retainAll(Collection<?> c) {
        if (isRealmCollection(c)) {
            OsSet otherOsSet = ((RealmSet<?>) c).getOsSet();
            return collectionFunnel(otherOsSet, OsSet.ExternalCollectionOperation.RETAIN_ALL);
        }
        return osSet.retainAllString(c);
    }

    @Override
    boolean removeAll(Collection<?> c) {
        if (isRealmCollection(c)) {
            OsSet otherOsSet = ((RealmSet<?>) c).getOsSet();
            return collectionFunnel(otherOsSet, OsSet.ExternalCollectionOperation.REMOVE_ALL);
        }
        return osSet.removeAllString(c);
    }
}

/**
 * TODO
 *
 * @param <E>
 */
class SetIterator<E> implements Iterator<E> {

    private final OsSet osSet;
    private final BaseRealm baseRealm;      // TODO: needed for models, will be abstracted later

    private int pos = -1;

    public SetIterator(OsSet osSet, BaseRealm baseRealm) {
        this.osSet = osSet;
        this.baseRealm = baseRealm;
    }

    @Override
    public boolean hasNext() {
        return pos + 1 < osSet.size();
    }

    @Override
    public E next() {
        pos++;
        long size = osSet.size();
        if (pos >= size) {
            throw new NoSuchElementException("Cannot access index " + pos + " when size is " + size +
                    ". Remember to check hasNext() before using next().");
        }

        return getValueAtIndex(pos);
    }

    private E getValueAtIndex(int position) {
        //noinspection unchecked
        return (E) osSet.getValueAtIndex(position);
    }
}