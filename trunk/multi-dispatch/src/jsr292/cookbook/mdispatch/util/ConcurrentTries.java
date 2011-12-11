package jsr292.cookbook.mdispatch.util;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class ConcurrentTries {
  volatile Node head;
  
  public ConcurrentTries() {
    head = new ArrayNode(0, new Node[0], 0);
  }
  
  public int lookup(Class<?> key) {
    return head.lookup(key.hashCode(), key);
  }
  
  public void unsafeAdd(Class<?> key, int value) {
    head = head.add(key.hashCode(), 0, key, value);
  }
  
  @Override
  public String toString() {
    return head.toString();
  }
  
  static final AtomicReferenceFieldUpdater<ConcurrentTries, Node> UPDATER =
      AtomicReferenceFieldUpdater.newUpdater(ConcurrentTries.class, Node.class, "head");
  
  int update(int hashCode, Class<?> key) {
    Node head = this.head;  // volatile read not necessary but
                            // there is no way to get a non-volatile read
    int newValue = 0;
    for(Class<?> clazz = key.getSuperclass(); clazz != null; clazz = clazz.getSuperclass()) {
      int value = head.get(clazz.hashCode(), clazz);
      if (value != 0) {
        newValue = value;
        break;
      }
    }

    for(;;) {
      head = this.head;  // volatile read
      Node root = head.add(hashCode, 0, key, newValue);
      if (root == head) {  // the tries has been already updated by another thread
        return newValue;
      }
      
      if (UPDATER.compareAndSet(this, head, root)) {
        return newValue;
      }
    }
  }
  
  private static abstract class Node {
    Node() {
      super();
    }
    
    abstract int lookup(int hashCode, Class<?> key);
    abstract int get(int hashCode, Class<?> key);
    abstract Node add(int hashCode, int shift, Class<?> key, int newValue);
  }
  
  private class EntryNode extends Node {
    final int hashCode;
    private final Class<?> key;
    private final int value;
    
    EntryNode(int hashCode, Class<?> key, int value) {
      this.hashCode = hashCode;
      this.key = key;
      this.value = value;
    }

    @Override
    int get(int hashCode, Class<?> key) {
      if (key == this.key)
        return value;
      return 0;
    }
    
    @Override
    Node add(int hashCode, int shift, Class<?> key, int newValue) {
      if (key == this.key) // another thread has already updated the tries
        return this;
      return new ArrayNode(this, shift).add(hashCode, shift, key, newValue);
    }
    
    @Override
    int lookup(int hashCode, Class<?> key) {
      if (key == this.key)
        return value;
      
      // we need to compute a new (key, value)
      return update(hashCode, key);
    }
    
    @Override
    public String toString() {
      return "("+Integer.toBinaryString(hashCode)+')'+key+": "+value;
    }
  }
  
  static int bit(int hashCode, int shift){
    return 1 << ((hashCode >>> shift) & 0x01f);
  }
  
  static int index(int bits, int bit){
    return Integer.bitCount(bits & (bit - 1));
  }
  
  private class ArrayNode extends Node {
    private final int bits;
    private final Node[] nodes;
    private final int shift;
    
    ArrayNode(EntryNode entryNode, int shift) {
      this(bit(entryNode.hashCode, shift), new Node[] {entryNode}, shift);
    }
    
    ArrayNode(int bits, Node[] nodes, int shift) {
      this.bits = bits;
      this.nodes = nodes;
      this.shift = shift;
    }

    @Override
    int get(int hashCode, Class<?> key) {
      int bit = bit(hashCode, shift);
      int bits = this.bits;
      int index = index(bits, bit);
      if ((bits & bit) != 0) {
        return nodes[index].get(hashCode, key);
      }
      return 0;
    }
    
    @Override
    Node add(int hashCode, int shift, Class<?> key, int newValue) {
      int bit = bit(hashCode, shift);
      int bits = this.bits;
      Node[] nodes = this.nodes;
      int index = index(bits, bit);
      if ((bits & bit) != 0) {  // collision
        Node node = nodes[index];
        Node newNode = node.add(hashCode, shift + 5, key, newValue);
        if (newNode == node)
          return this;
        
        Node[] array = Arrays.copyOf(nodes, nodes.length);
        array[index] = newNode;
        return new ArrayNode(bits, array, shift);
      }
      
      int length = nodes.length;
      Node[] array = new Node[length + 1];
      System.arraycopy(nodes, 0, array, 0, index);
      array[index] = new EntryNode(hashCode, key, newValue);
      System.arraycopy(nodes, index, array, index + 1, length - index);
      
      return new ArrayNode(bits | bit, array, shift);
    }
    
    @Override
    int lookup(int hashCode, Class<?> key) {
      int bit = bit(hashCode, shift);
      int bits = this.bits;
      if ((bits & bit) != 0) {
        return nodes[index(bits, bit)].lookup(hashCode, key);
      }
      
      return update(hashCode, key);
    }
    
    @Override
    public String toString() {
      return shift + Arrays.toString(nodes);
    }
  }
}
