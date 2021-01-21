package zendo.games.zenlib;

public abstract class ListNode<T> {
    public T next;
    public T prev;

    public void reset() {}

    public T next() { return next; }
    public T prev() { return prev; }

    public void setNext(T next) { this.next = next; }
    public void setPrev(T prev) { this.prev = prev; }

}
