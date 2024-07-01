package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.AbstractMap.SimpleImmutableEntry;

public class TaskBody implements InitializingConfigs {
  private int taskID;
  private HashMap<Integer, TreeSet<Integer>> executedData = new HashMap<>();
  private HashMap<SimpleImmutableEntry<Integer, Integer>, TreeSet<Integer>> dataDelivered = new HashMap<>();


  public TaskBody (int taskID, HashMap<Integer, TreeSet<Integer>> executedData, 
  HashMap<SimpleImmutableEntry<Integer, Integer>, TreeSet<Integer>> dataDelivered) {
    this.taskID = taskID;
    this.executedData = executedData;
    this.dataDelivered = dataDelivered;
  }

  public TaskBody(TaskBody source) {
    this.taskID = source.getTaskID();
    HashMap<Integer, TreeSet<Integer>> sourceExecutedData = source.getExecutedData();
    this.executedData.putAll(sourceExecutedData);
    // if (sourceExecutedData != null) {
    //   for (Entry<Integer, TreeSet<Integer>> entry : sourceExecutedData.entrySet()) {
    //     int key = entry.getKey();
    //     TreeSet<Integer> sourceVersions = entry.getValue();
    //     TreeSet<Integer> newVersions = new TreeSet<>();
    //     newVersions.addAll(sourceVersions);
    //     this.executedData.put(key, newVersions);
    //   }
    // }
    HashMap<SimpleImmutableEntry<Integer, Integer>, TreeSet<Integer>> sourceDataDelivered = source.getDataDelivered();
    if (sourceDataDelivered != null) {
        for (SimpleImmutableEntry<Integer, Integer> entry : sourceDataDelivered.keySet()) {
            TreeSet<Integer> newSetOfData = new TreeSet<>();
            TreeSet<Integer> sourceSetOfData = sourceDataDelivered.get(entry);
            if (sourceSetOfData != null) {
                newSetOfData.addAll(sourceSetOfData);
            }
            this.dataDelivered.put(entry, newSetOfData);
        }
    }  
  }

  public int getTaskID() {
    return taskID;
  }

  public HashMap<Integer, TreeSet<Integer>> getExecutedData() {
    return executedData;
  }

  public HashMap<SimpleImmutableEntry<Integer, Integer>, TreeSet<Integer>> getDataDelivered() {
    return dataDelivered;
  }

  public void setTaskID(int taskID) {
    this.taskID = taskID;
  }
  

  public void setExecutedData(HashMap<Integer, TreeSet<Integer>> executedData) {
    this.executedData = executedData;
  }

  public HashMap<Integer, TreeSet<Integer>> integrateInputData() {
    HashMap<Integer, TreeSet<Integer>> output = new HashMap<>();
    for (Map.Entry<SimpleImmutableEntry<Integer, Integer>, TreeSet<Integer>> entry : this.dataDelivered.entrySet()) {
      Integer sensorId = entry.getKey().getValue();
      TreeSet<Integer> versions = entry.getValue();
      output.compute(sensorId, (k, v) -> {
        TreeSet<Integer> newSet = new TreeSet<>();
        if (v != null) {
          newSet.addAll(v);
        }
        if (versions != null) {
          newSet.addAll(versions);
        }
        return newSet.isEmpty() ? null : newSet;
      });
    }
    return output;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\n\tT").append(taskID);
    sb.append("\n\t\tE=");
    String invoked = getExecutedData().toString();
    sb.append(invoked);

    if (dataDelivered != null) {
      for (Map.Entry<SimpleImmutableEntry<Integer, Integer>, TreeSet<Integer>> entry : dataDelivered.entrySet()) {
        SimpleImmutableEntry<Integer, Integer> key = entry.getKey();
        TreeSet<Integer> value = entry.getValue();
        sb.append("\n\t\tPort").append(key.getKey())
          .append(",Sensor").append(key.getValue())
          .append("=").append(value.toString());
      }
    }

    return sb.toString();
  }
  
}