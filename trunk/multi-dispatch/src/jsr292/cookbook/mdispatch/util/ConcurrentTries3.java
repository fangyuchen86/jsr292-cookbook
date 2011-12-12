package jsr292.cookbook.mdispatch.util;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class ConcurrentTries3 {
  volatile Node head;
  
  public ConcurrentTries3() {
    head = new ArrayNode(0, new Node[0], 0);
  }
  
  public int lookup(Class<?> key) {
    int hashCode = key.hashCode();
    Node node = head;
    for(;;) {
      if (node instanceof EntryNode) {
        EntryNode entryNode = (EntryNode)node;
        if (key == entryNode.key)
          return entryNode.value;

        break;
      }

      ArrayNode arrayNode = (ArrayNode)node;
      int bit = bit(hashCode, arrayNode.shift);
      int bits = arrayNode.bits;
      if ((bits & bit) != 0) {
        node = arrayNode.nodes[index(bits, bit)];
        continue;
      }
      break;
    }
    return update(hashCode, key);
  }
  
  public void unsafeAdd(Class<?> key, int value) {
    head = head.add(key.hashCode(), 0, key, value);
  }
  
  @Override
  public String toString() {
    return head.toString();
  }
  
  static final AtomicReferenceFieldUpdater<ConcurrentTries3, Node> UPDATER =
      AtomicReferenceFieldUpdater.newUpdater(ConcurrentTries3.class, Node.class, "head");
  
  int update(int hashCode, Class<?> key) {
    Node head = this.head;  // volatile read not necessary but
                            // there is no way to get a non-volatile read
    int newValue = 0;
    for(Class<?> clazz = key.getSuperclass(); clazz != null; clazz = clazz.getSuperclass()) {
      int value = get(head, clazz.hashCode(), clazz);
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
  
  private static int get(Node node, int hashCode, Class<?> key) {
    for(;;) {
      if (node instanceof EntryNode) {
        EntryNode entryNode = (EntryNode)node;
        if (key == entryNode.key)
          return entryNode.value;
        return 0;
      }
      ArrayNode arrayNode = (ArrayNode)node;
      int bit = bit(hashCode, arrayNode.shift);
      int bits = arrayNode.bits;
      int index = index(bits, bit);
      if ((bits & bit) != 0) {
        node = arrayNode.nodes[index];
        continue;
      }
      return 0;
    }
  }
  
  static int bit(int hashCode, int shift){
    return 1 << ((hashCode >>> shift) & 0x01f);
  }
  
  static int index(int bits, int bit){
    return Integer.bitCount(bits & (bit - 1));
  }
  
  private static abstract class Node {
    Node() {
      super();
    }
    
    abstract Node add(int hashCode, int shift, Class<?> key, int newValue);
  }
  
  private static final class EntryNode extends Node {
    final int hashCode;
    final Class<?> key;
    final int value;
    
    EntryNode(int hashCode, Class<?> key, int value) {
      this.hashCode = hashCode;
      this.key = key;
      this.value = value;
    }

    @Override
    Node add(int hashCode, int shift, Class<?> key, int newValue) {
      if (key == this.key) // another thread has already updated the tries
        return this;
      return new ArrayNode(this, shift).add(hashCode, shift, key, newValue);
    }
    
    @Override
    public String toString() {
      return "("+Integer.toBinaryString(hashCode)+')'+key+": "+value;
    }
  }
  
  private static class ArrayNode extends Node {
    final int bits;
    final Node[] nodes;
    final int shift;
    
    ArrayNode(EntryNode entryNode, int shift) {
      this(bit(entryNode.hashCode, shift), new Node[] {entryNode}, shift);
    }
    
    ArrayNode(int bits, Node[] nodes, int shift) {
      this.bits = bits;
      this.nodes = nodes;
      this.shift = shift;
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
    public String toString() {
      return shift + Arrays.toString(nodes);
    }
  }
}
