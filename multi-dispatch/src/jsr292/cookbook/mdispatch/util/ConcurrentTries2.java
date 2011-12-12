package jsr292.cookbook.mdispatch.util;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class ConcurrentTries2 {
  volatile Node head;
  
  public ConcurrentTries2() {
    head = new Node(0, new Node[0], 0);
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
  
  static final AtomicReferenceFieldUpdater<ConcurrentTries2, Node> UPDATER =
      AtomicReferenceFieldUpdater.newUpdater(ConcurrentTries2.class, Node.class, "head");
  
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
  
  static int bit(int hashCode, int shift){
    return 1 << ((hashCode >>> shift) & 0x01f);
  }
  
  static int index(int bits, int bit){
    return Integer.bitCount(bits & (bit - 1));
  }
  
  private final class Node {
    final int hashCode;                // key/value node
    private final Class<?> key;
    private final int value;
    
    private final int bits;            // array of nodes
    private final Node[] nodes;
    private final int shift;
    
    private Node(int hashCode, Class<?> key, int value) {
      this.hashCode = hashCode;
      this.key = key;
      this.value = value;
      this.nodes = null;
      this.bits = this.shift = 0;
    }
    
    private Node(Node entryNode, int shift) {
      this(bit(entryNode.hashCode, shift), new Node[] {entryNode}, shift);
    }
    
    Node(int bits, Node[] nodes, int shift) {
      this.bits = bits;
      this.nodes = nodes;
      this.shift = shift;
      this.key = null;
      this.hashCode = this.value = 0;
    }

    int get(int hashCode, Class<?> key) {
      Node node = this;
      for(;;) {
        Node[] nodes = node.nodes;
        if (nodes == null) {
          if (key == node.key)
            return node.value;
          return 0;
        }
        int bit = bit(hashCode, node.shift);
        int bits = node.bits;
        int index = index(bits, bit);
        if ((bits & bit) != 0) {
          node = nodes[index];
          continue;
        }
        return 0;
      }
    }
    
    Node add(int hashCode, int shift, Class<?> key, int newValue) {
      if (nodes == null) {
        if (key == this.key) // another thread has already updated the tries
          return this;
        return new Node(this, shift).add(hashCode, shift, key, newValue);
      }
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
        return new Node(bits, array, shift);
      }
      
      int length = nodes.length;
      Node[] array = new Node[length + 1];
      System.arraycopy(nodes, 0, array, 0, index);
      array[index] = new Node(hashCode, key, newValue);
      System.arraycopy(nodes, index, array, index + 1, length - index);
      
      return new Node(bits | bit, array, shift);
    }
    
    int lookup(int hashCode, Class<?> key) {
      Node node = this;
      for(;;) {
        Node[] nodes = node.nodes;
        if (nodes == null) {
          if (key == node.key)
            return node.value;
          break;
        }
        int bit = bit(hashCode, node.shift);
        int bits = node.bits;
        if ((bits & bit) != 0) {
          node = nodes[index(bits, bit)];
          continue;
        }
        break;
      }
      return update(hashCode, key);
    }
    
    @Override
    public String toString() {
      if (nodes == null) {
        return "("+Integer.toBinaryString(hashCode)+')'+key+": "+value;
      }
      return shift + Arrays.toString(nodes);
    }
  }
}
