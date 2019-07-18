package com.gigaspaces;

import io.vavr.collection.List;
import io.vavr.collection.Set;

@SuppressWarnings("unused")
public class Diff {
    private List<Instance> added;
    private List<Instance> removed;
    private List<Instance> unchanged;

    @SuppressWarnings("WeakerAccess")
    public static Diff create(Set<Instance> oldInstances, Set<Instance> newInstances){
        List<Instance> added = List.ofAll(newInstances.diff(oldInstances)).sorted();
        List<Instance> removed = List.ofAll(oldInstances.diff(newInstances)).sorted();
        List<Instance> unchanged =  List.ofAll(oldInstances.intersect(newInstances)).sorted();
        return new Diff(added, removed, unchanged);
    }

    private Diff(List<Instance> added, List<Instance> removed, List<Instance> unchanged) {
        this.added = added;
        this.removed = removed;
        this.unchanged = unchanged;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean wasModified(){
        return !added.isEmpty() || !removed.isEmpty();
    }

    @SuppressWarnings("WeakerAccess")
    public List<Instance> getAdded() {
        return added;
    }

    @SuppressWarnings("WeakerAccess")
    public List<Instance> getRemoved() {
        return removed;
    }

    @SuppressWarnings("WeakerAccess")
    public List<Instance> getUnchanged() {
        return unchanged;
    }

    @SuppressWarnings("WeakerAccess")
    public int getRunningSize(){
        return added.length() + unchanged.length();
    }
    @SuppressWarnings("WeakerAccess")
    public int getAddedSize(){
        return added.length();
    }
    @SuppressWarnings("WeakerAccess")
    public int getRemovedSize(){
        return removed.length();
    }

    public boolean isHasRemoved(){
        return !removed.isEmpty();
    }
    public boolean isHasUnchanged(){
        return !unchanged.isEmpty();
    }
    public boolean isHasAdded(){
        return !added.isEmpty();
    }
}
