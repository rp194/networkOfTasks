package com.example;

import java.util.*;

public class NewTest {
    public static void main(String[] args) {
        // Create the original PriorityQueue
        PriorityQueue<Integer> originalQueue = new PriorityQueue<>();
        originalQueue.add(3);
        originalQueue.add(1);
        originalQueue.add(2);

        // Create a copy of the original PriorityQueue
        PriorityQueue<Integer> copiedQueue = new PriorityQueue<>(originalQueue);

        // Poll an element from the copiedQueue
        int polledElement = copiedQueue.poll();
        System.out.println("Polled element from copiedQueue: " + polledElement);

        // Check the contents of both queues
        System.out.println("Original queue after polling from copied queue: " + originalQueue);
        System.out.println("Copied queue after polling: " + copiedQueue);
    }
}
