package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TaskSet {

  private static Map<Integer, List<Integer>> adjList = new HashMap<Integer, List<Integer>>();

  public void addVertex(int vertex) {
    adjList.putIfAbsent(vertex, new ArrayList<>());
  }

  public static void addEdge(int source, int destination) {
    adjList.putIfAbsent(source, new ArrayList<>());
    adjList.get(source).add(destination);
  }

  public List<Integer> getAdjVertices(int vertex) {
    return adjList.get(vertex);
  }

  public List<Integer> getParents(int vertex) {
    List<Integer> parents = new ArrayList<>();
    for (Map.Entry<Integer, List<Integer>> entry : adjList.entrySet()) {
      if (entry.getValue().contains(vertex)) {
        parents.add(entry.getKey());
      }
    }
    return parents;
  }

}
