package com.example;

public class LinkedList {
  private class Node {
    public int data;
    public Node next;
  
    public Node(int data) {
        this.data = data;
        this.next = null;
    }
  }
  
  private Node head;
  private Node tail;

  public void add(int data) {
      Node newNode = new Node(data);
      if (head == null) {
          head = tail = newNode;
      } else {
          tail.next = newNode;
          tail = newNode;
      }
  }

  public void printList() {
      Node current = head;
      while (current != null) {
          System.out.print(current.data + " -> ");
          current = current.next;
      }
      System.out.println("null");
  }

  // Additional methods for searching, removing, etc.
}


