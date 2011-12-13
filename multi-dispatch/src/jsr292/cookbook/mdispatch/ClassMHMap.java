package jsr292.cookbook.mdispatch;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;

public class ClassMHMap {
  Node head;
  
  ClassMHMap(Node head) {
    this.head = head;
  }
  
  public MethodHandle lookup(Class<?> key) {
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
  
  void unsafeAdd(Class<?> key, MethodHandle value) {
    head = head.add(key.hashCode(), 0, key, value);
  }
  
  @Override
  public String toString() {
    return head.toString();
  }
  
  // head is not volatile and this code doesn't use a CAS
  // because this class act has a cache with no removal, so 
  // maybe some update may be lost but there will be re-computed if necessary
  MethodHandle update(int hashCode, Class<?> key) {
    Node head = this.head; 
    MethodHandle newValue = null;
    for(Class<?> clazz = key.getSuperclass(); clazz != null; clazz = clazz.getSuperclass()) {
      MethodHandle value = get(head, clazz.hashCode(), clazz);
      if (value != null) {
        newValue = value;
        break;
      }
    }

    for(;;) {
      head = this.head;  
      Node root = head.add(hashCode, 0, key, newValue);
      if (root != head) {  // if the tries has not been updated by another thread
        this.head = root;
      }
      return newValue;
      
    }
  }
  
  private static MethodHandle get(Node node, int hashCode, Class<?> key) {
    for(;;) {
      if (node instanceof EntryNode) {
        EntryNode entryNode = (EntryNode)node;
        if (key == entryNode.key)
          return entryNode.value;
        return null;
      }
      ArrayNode arrayNode = (ArrayNode)node;
      int bit = bit(hashCode, arrayNode.shift);
      int bits = arrayNode.bits;
      int index = index(bits, bit);
      if ((bits & bit) != 0) {
        node = arrayNode.nodes[index];
        continue;
      }
      return null;
    }
  }
  
  static int bit(int hashCode, int shift){
    return 1 << ((hashCode >>> shift) & 0x01f);
  }
  
  static int index(int bits, int bit){
    return Integer.bitCount(bits & (bit - 1));
  }
  
  static abstract class Node {
    abstract Node add(int hashCode, int shift, Class<?> key, MethodHandle newValue);
  }
  
  static final class EntryNode extends Node {
    final int hashCode;
    final Class<?> key;
    final MethodHandle value;
    
    EntryNode(int hashCode, Class<?> key, MethodHandle value) {
      this.hashCode = hashCode;
      this.key = key;
      this.value = value;
    }

    @Override
    Node add(int hashCode, int shift, Class<?> key, MethodHandle newValue) {
      if (key == this.key) // another thread has already updated the tries
        return this;
      return new ArrayNode(this, shift).add(hashCode, shift, key, newValue);
    }
    
    @Override
    public String toString() {
      return "("+Integer.toBinaryString(hashCode)+')'+key+": "+value;
    }
  }
  
  static class ArrayNode extends Node {
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
    Node add(int hashCode, int shift, Class<?> key, MethodHandle newValue) {
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
