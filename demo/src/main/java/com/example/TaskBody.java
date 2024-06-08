package com.example;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeSet;

public class TaskBody implements InitializingConfigs {
  private int taskID;
  private boolean isExecuted;
  private HashMap<Integer, HashMap<Integer, TreeSet<Integer>>> dataDelivered = new HashMap<>();

  public TaskBody (int taskID, boolean isExecuted, HashMap<Integer, HashMap<Integer, TreeSet<Integer>>> dataDelivered) {
    this.taskID = taskID;
    this.isExecuted = isExecuted;
    this.dataDelivered = dataDelivered;
  }

  public TaskBody(TaskBody source) {
    this.taskID = source.getTaskID();
    this.isExecuted = source.isExecuted();
    HashMap<Integer, HashMap<Integer, TreeSet<Integer>>> sourceData = source.getDataDelivered();
    if (sourceData != null){
      for (int port : sourceData.keySet()) {
        HashMap<Integer, TreeSet<Integer>> newDataOnPort = new HashMap<>();
        HashMap<Integer, TreeSet<Integer>> sourceDataOnPort = sourceData.get(port);
        for (Integer sensorId : sourceDataOnPort.keySet()) {
          TreeSet<Integer> newSetOfData = new TreeSet<>();
          TreeSet<Integer> sourceSetOfData = sourceDataOnPort.get(sensorId);
          if (sourceSetOfData != null) {
            newSetOfData.addAll(sourceSetOfData);
          }
          newDataOnPort.put(sensorId, newSetOfData);
        }
        this.dataDelivered.put(port, newDataOnPort);
      }
    }
  }

  public int getTaskID() {
    return taskID;
  }


  public boolean isExecuted() {
    return isExecuted;
  }

  public HashMap<Integer, HashMap<Integer, TreeSet<Integer>>> getDataDelivered() {
    return dataDelivered;
  }

  public void setTaskID(int taskID) {
    this.taskID = taskID;
  }
  
  public void setExecuted(boolean isExecuted) {
    this.isExecuted = isExecuted;
  }

  public void putData(int port, HashMap<Integer, TreeSet<Integer>> dataOnPort) {
    this.dataDelivered.put(port, dataOnPort);
  }

  public HashMap<Integer, TreeSet<Integer>> integrateInputData() {
    HashMap<Integer, TreeSet<Integer>> output = new HashMap<>();
    for (HashMap<Integer, TreeSet<Integer>> dataOnPort : this.dataDelivered.values()) {
        for (Entry<Integer, TreeSet<Integer>> sensorData : dataOnPort.entrySet()) {
            output.compute(sensorData.getKey(), (k, v) -> {
                if (v == null) {
                    v = new TreeSet<>();
                }
                if (sensorData.getValue() != null) {
                    v.addAll(sensorData.getValue());
                } 
                if (v.size() == 0) {
                    v = null;
                }
                return v;
            });
        }
    }
    return output;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
      // HashMap<Object, Object> body = entry.getValue();
      sb.append("\n\tT").append(taskID);
      sb.append("\n\t\tE=" );
      String invoked = isExecuted? "T" : "F";
      sb.append(invoked);
      if (dataDelivered != null) {  
        for (int port  : dataDelivered.keySet()) {
          sb.append("\n\t\t" + port  + "=");
          sb.append(dataDelivered.get(port).toString());
        }      
      }
      return sb.toString();
    }
}