package it.unitn.DS;

import java.util.ArrayList;
import java.util.List;


public class Tree<T> {

    private T data = null;

    private List<Tree<T>> children = new ArrayList<>();

    private Tree<T> parent = null;

    public Tree(T data) {
        this.data = data;
    }

    public Tree<T> addChild(Tree<T> child) {
        child.setParent(this);
        this.children.add(child);
        return child;
    }

    public List<Tree<T>> getChildren() {
        return children;
    }

    public T getData() {
        return data;
    }

    private void setParent(Tree<T> parent) {
        this.parent = parent;
    }

    public Tree<T> getParent() {
        return parent;
    }

    public List<T> getNeighbors(){
        List<T> neighbours = new ArrayList<T>();
        this.children.forEach(c -> {
            neighbours.add(c.data);
        });
        neighbours.add(this.parent.data);
        return neighbours;
    }
}