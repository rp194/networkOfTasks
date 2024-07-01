package com.example;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.AbstractMap.SimpleImmutableEntry;

public class State implements InitializingConfigs {
  private int stateId;
  private HashSet<Integer> sourceIds = new HashSet<>();
  private HashMap<Integer, TaskBody> tasksBody = new HashMap<>();
  private int stateTime;
  private int idleProcessors;
  private PriorityQueue<EventSet> eventSetQueue = new PriorityQueue<>();
  private HashMap<Integer, HashSet<Integer>> producedEventSensorsData = new HashMap<>();
  private HashMap<Integer, HashSet<Integer>> producedStatusSensorsData = new HashMap<>();
  private HashMap<String, HashSet<Integer>> actuatorsWithStatusData = new HashMap<>();

  public State() {/*:)) */}

  public
  State(State source, int id, int stateTime, int idleProcessors,
  HashMap<Integer, TaskBody> tasksChanges, PriorityQueue<EventSet> eventSetQueue) {
    this.stateId = id;
    this.idleProcessors = idleProcessors;
    this.stateTime = stateTime;
    this.eventSetQueue.addAll(eventSetQueue);
    if (source.getEventSetQueue().isEmpty()) {
      this.sourceIds.add(-1);
    } else {
      this.sourceIds.add(source.getStateID());
      copyProducedSensorsData(this.producedEventSensorsData, source.getProducedEventSensorsData());
      // copyProducedSensorsData(this.producedStatusSensorsData, source.getProducedStatusSensorsData());
      copyProducedSensorsData(this.actuatorsWithStatusData, source.getActuatorsWithStatusData());
      this.producedStatusSensorsData = source.getUnsatisfiedStatusVersions();
    }

    copyTasksBody(source, tasksChanges);

    executionAndSchedulityRegulator(source);
    policiesChecker(source);
  } 


  private <K, V> void 
  copyProducedSensorsData(HashMap<K, HashSet<V>> producedSensorsData1, HashMap<K, HashSet<V>> producedSensorsData2) {
    for (Entry<K, HashSet<V>> entry : producedSensorsData2.entrySet()) {
      K key = entry.getKey();
      HashSet<V> originalList = entry.getValue();
      HashSet<V> copiedList = new HashSet<>(originalList.size());
      for (V value : originalList) {
        copiedList.add(value);
      }
      producedSensorsData1.put(key, copiedList);
    }
  }

  private void copyTasksBody(State source, Map<Integer, TaskBody> tasksChanges) {
    TreeMap<Integer, TaskBody> sortedMap = new TreeMap<>(Comparator.reverseOrder());
    sortedMap.putAll(source.getTasksBody());
    this.tasksBody.putAll(tasksChanges);  
    for (Entry<Integer, TaskBody> sourceTaskBodyEntry : sortedMap.entrySet()) {
      Integer key = sourceTaskBodyEntry.getKey();
      TaskBody sourceTaskBody = sourceTaskBodyEntry.getValue();
      if (!tasksChanges.containsKey(key)) {
        TaskBody newTaskBody = new TaskBody(sourceTaskBody);
        this.tasksBody.put(key, newTaskBody);
      }
      else if (tasksChanges.get(key).getTaskID() == 0) {
        tasksChanges.get(key).setTaskID(key);
        if (eventSensors.contains(key)) {
          addVersion(producedEventSensorsData, key, stateTime);
        }
        else if (statusSensors.contains(key)) {
          addVersion(producedStatusSensorsData, key, stateTime);
        }
        else if (actuators.contains(key)) {
          HashMap<Integer, TreeSet<Integer>> finalStageData = sourceTaskBody.integrateInputData();
          for (Entry<Integer, HashSet<Integer>> producedSensorDataEntry : producedEventSensorsData.entrySet()) {
            Integer eventSensorId = producedSensorDataEntry.getKey();
            HashSet<Integer> eventSensorVersions =  producedSensorDataEntry.getValue();
            for (int version : eventSensorVersions) {
              if (finalStageData.get(eventSensorId) != null) {
                if (finalStageData.get(eventSensorId).contains(version)) {
                  if (stateTime - version <= timingConstraints.get(1)) {
                    eventSensorVersions.remove(version);
                  }
                  else {
                    this.eventSetQueue.clear();
                    return;
                  }
                }
              }
            }
          }

          for (Entry<Integer, TreeSet<Integer>> finalDataEntry : finalStageData.entrySet()) {
            Integer sensorId = finalDataEntry.getKey();
            if (producedStatusSensorsData.containsKey(sensorId)) {
              for (Integer version : finalDataEntry.getValue()) {
                if (!isConstraintFulfilled(sensorId, version, actuators.size())) {
                  if (!updateStatus(sensorId, version, key)) {
                    this.eventSetQueue.clear();
                    return;
                  }
                }
              }
            }
          }
        }
      }
    }
  }


  public int getStateID() {
    return this.stateId;
  }

  public int getStateTime() {
    return this.stateTime;
  }

  public int getIdleProcessors() {
    return idleProcessors;
  }

  public HashMap<Integer,TaskBody> getTasksBody() {
    return tasksBody;
  }

  public TaskBody getTaskBody(int taskId) {
    return tasksBody.get(taskId);
  }

  public PriorityQueue<EventSet> getEventSetQueue() {
    return this.eventSetQueue;
  }

  public HashMap<Integer, HashSet<Integer>> getProducedEventSensorsData() {
    return producedEventSensorsData;
  }

  public HashMap<Integer, HashSet<Integer>> getProducedStatusSensorsData() {
    return producedStatusSensorsData;
  }

  public HashMap<String, HashSet<Integer>> getActuatorsWithStatusData() {
    return actuatorsWithStatusData;
  }
  
  private void executionAndSchedulityRegulator(State source) {
    for (EventSet eventSet : this.eventSetQueue) {
      for (Event updateEvent : eventSet.getUpdates()) {
        for (EventSet sourceEventSet : source.getEventSetQueue()) {
          if (sourceEventSet.getEventSetTime() < updateEvent.getTime()) {
            HashSet<Event> sourceArrivals = sourceEventSet.getArrivals();
            for (Event sourceArrival : sourceArrivals) {
              int taskId = sourceArrival.getTaskId();
              Task task = getTaskById(taskId);
              if (sourceArrival.getTaskId() == updateEvent.getTaskId() && updateEvent.getTime() > sourceArrival.getTime() + task.getDeadline()) {
                this.eventSetQueue.clear();
                return;
              }
            }
          }
        }
      }
    }

    for (EventSet eventSet : this.eventSetQueue) {
      for (Event arrivalEvent : eventSet.getArrivals()) {
        int taskId = arrivalEvent.getTaskId();
        Task task = getTaskById(taskId);
        if (this.stateTime > arrivalEvent.getTime() + task.getDeadline() - task.getDuration()){
          this.eventSetQueue.clear();
          return;
        }
      }   
    }
  }
  
  private void policiesChecker(State source) {
    for (Entry<Integer, HashSet<Integer>> entry : producedEventSensorsData.entrySet()) {
      HashSet<Integer> value = entry.getValue();
      for (int version : value) {
        if (stateTime - version > timingConstraints.get(1)) {
          this.eventSetQueue.clear();
          return;
        }
      }
    }

    for (Map.Entry<Integer, HashSet<Integer>> entry : source.getProducedStatusSensorsData().entrySet()) {
      Integer sensorId = entry.getKey();
      HashSet<Integer> versions = entry.getValue();
      for (int version : versions) {
        if (!isConstraintFulfilled(sensorId, version, actuators.size())) {
          if (stateTime - version > timingConstraints.get(2)) {
            this.eventSetQueue.clear();
            return;
          }
        }
        boolean isExpired = true;
        boolean foundNonNull = false;
        for (TaskBody taskBody : tasksBody.values()) {
          TreeSet<Integer> executedVersions = taskBody.integrateInputData().get(sensorId);
          if (executedVersions != null) {
            foundNonNull = true;
            if (executedVersions.contains(version)) {
              isExpired = false;
              break;
            }
          }
        }
        if (isExpired && foundNonNull) {
          this.eventSetQueue.clear();
          return;
        }
      }
    }

    for (TaskBody taskBody : tasksBody.values()) {
      HashMap<Integer, TreeSet<Integer>> executedData = taskBody.getExecutedData();
      int[] minMax = new int[2];
      findMinAndMax(executedData, minMax);
      Integer min = minMax[0];
      Integer max = minMax[1];
      if (max - min > timingConstraints.get(3)) {
        this.eventSetQueue.clear();
        return;
      }      
    }
  }
  
  private void addVersion(HashMap<Integer, HashSet<Integer>> producedSensorsData, int sensorId, int version) {
    HashSet<Integer> sensorVersions =  producedSensorsData.computeIfAbsent(sensorId, k -> new HashSet<Integer>());
    sensorVersions.add(version);    
  }
  
  public static List<Integer> findUniquePositives(Set<Integer> set) {
    List<Integer> result = new ArrayList<>();
    for (Integer num : set) {
      if (num > 0 && !set.contains(-num)) {
        result.add(num);
      }
    }
    return result;
  }
  
  private void findMinAndMax(HashMap<Integer, TreeSet<Integer>> executedData, int[] minMax) {
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    for (TreeSet<Integer> treeSet : executedData.values()) {
      if (!treeSet.isEmpty()) {
        int localMin = treeSet.first();
        int localMax = treeSet.last();          
        if (localMin < min) {
          min = localMin;
        }
        if (localMax > max) {
          max = localMax;
        }
      }
    }
    minMax[0] = min;
    minMax[1] = max;
  }
  
  private boolean updateStatus(int sensorId, int version, int finalTaskId) {
    if (stateTime - version > timingConstraints.get(2)) {
      return false;
    }
    String key = sensorId + "_" + version;
    actuatorsWithStatusData.putIfAbsent(key, new HashSet<>());
    actuatorsWithStatusData.get(key).add(finalTaskId);
    if (isConstraintFulfilled(sensorId, version, actuators.size())) {
      producedStatusSensorsData.get(sensorId).remove(version);
      if (producedStatusSensorsData.get(sensorId).size() == 0) {
        producedStatusSensorsData.remove(sensorId);
      }
    }
    return true;
  }

  public boolean isConstraintFulfilled(int sensorId, int version, int numOfFinalTasks) {
    String key = sensorId + "_" + version;
    if (!actuatorsWithStatusData.containsKey(key)) {
        return false;
    }
    HashSet<Integer> executedTasks = actuatorsWithStatusData.get(key);
    return executedTasks.size() == numOfFinalTasks;
  }

  public Task getTaskById(int taskId) {
    for (Task task : taskList) {
        if (task.getId() == taskId) {
            return task;
        }
    }
    return null;
  }

  public HashSet<Integer> getSourceIds() {
    return this.sourceIds;
  }

  public void insertNewSource(HashSet<Integer> newSourceIds) {
    sourceIds.addAll(newSourceIds);
  }

  public HashMap<Integer, HashSet<Integer>> getUnsatisfiedStatusVersions() {
    HashMap<Integer, HashSet<Integer>> result = new HashMap<>();
    int actuatorSize = actuators.size();
    for (Entry<Integer, HashSet<Integer>> statusEntry : producedStatusSensorsData.entrySet()) {
      Integer sensorId = statusEntry.getKey();
      HashSet<Integer> innerSet = new HashSet<>();
      result.put(sensorId, innerSet);
      for (int version : statusEntry.getValue()) {
        if (!isConstraintFulfilled(sensorId, version, actuatorSize)) {
          innerSet.add(version);
        }
      }
    }
    return result;
  }

  @Override
  public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("S>").append(stateId);
      sb.append("\n\tC>");
      int count = 1;
      for (int sourceId : sourceIds) {
        if (count % 12 == 0) {
          sb.append("\n\t");
        }
        sb.append(sourceId).append(",");
        count += 1;
      }
      sb.append("\n\t@").append(stateTime);
      sb.append("\n\tP:").append(idleProcessors);
      sb.append("\n\tT[]:");
      for (Entry<Integer, TaskBody> entry : tasksBody.entrySet()) {
        sb.append(entry.getValue().toString());
      }
      sb.append("\n\tQ: ");
      for (EventSet eventSet: eventSetQueue) {
        for (Event up: eventSet.getUpdates()) {
          sb.append("\n\t" + "U>T" + up.getTaskId() + "@" + up.getTime());
        }
        for (Event ar: eventSet.getArrivals()) {
          sb.append("\n\t" + "A>T" + ar.getTaskId() + "@" + ar.getTime());
        }
      }
      
      return sb.append("\n").toString();
  }

  public boolean isTasksBodyEqual(State newState, HashMap<Integer, TaskBody> newTasksBody, int diff) {
    for (Map.Entry<Integer, TaskBody> newTaskBodyEntry : newTasksBody.entrySet()) {
      Integer newTaskId = newTaskBodyEntry.getKey();
      TaskBody thisTaskBody = tasksBody.get(newTaskId);
      TaskBody newTaskBody = newTaskBodyEntry.getValue();
      HashMap<Integer, TreeSet<Integer>> thisExecutedData = thisTaskBody.getExecutedData();
      HashMap<Integer, TreeSet<Integer>> newExecutedData = newTaskBody.getExecutedData();
      if (newExecutedData == null && thisExecutedData == null) {
        return true;
      }
      else if (newExecutedData == null || thisExecutedData == null) {
        return false;
      }
      TreeSet<Integer> sensorIds = new TreeSet<>();
      sensorIds.addAll(thisExecutedData.keySet());
      sensorIds.addAll(newExecutedData.keySet());
      for (Integer sensorId : sensorIds) {
        if (!thisExecutedData.containsKey(sensorId) || !newExecutedData.containsKey(sensorId)) {
          return false;
        }
        if (!compareData(thisExecutedData.get(sensorId), newExecutedData.get(sensorId), diff)) {
          return false;
        }
      }
      HashMap<SimpleImmutableEntry<Integer, Integer>, TreeSet<Integer>> thisData = thisTaskBody.getDataDelivered();
      HashMap<SimpleImmutableEntry<Integer, Integer>, TreeSet<Integer>> newData = newTaskBody.getDataDelivered();
      if (newData != null) {
        for (Map.Entry<SimpleImmutableEntry<Integer, Integer>, TreeSet<Integer>> entry : newData.entrySet()) {
          SimpleImmutableEntry<Integer, Integer> key = entry.getKey();
          TreeSet<Integer> newPortData = entry.getValue();
          TreeSet<Integer> thisPortData = thisData.get(key);
          if (!compareData(thisPortData, newPortData, diff)) {
            return false;
          }
        }
      }    
    }
    return true;
  }

  public boolean compareData(Set<Integer> previousData, Set<Integer> newData, int diff) {
    if (previousData == null && newData == null) {
      return true;
    }
    if (previousData == null || newData == null) {
      return false;
    }
    if (newData.size() != previousData.size()) {
        return false;
    }
    Iterator<Integer> previousDataIterator = previousData.iterator();
    Iterator<Integer> newDataIterator = newData.iterator();

    while (previousDataIterator.hasNext()) {
      int preVersion = previousDataIterator.next();
      int newVersion = newDataIterator.next();
      if (preVersion + diff != newVersion) {
        return false;
      }
    }
    return true;
  }


  public Long longHashCode() {
    if (eventSetQueue.isEmpty()) {
        return -101L;
    }
    Long result = 1L;
    result = 31 * result + idleProcessors;
    int tId;
    for (tId = 1; tasksBody.containsKey(tId); tId++){
      result = 31 * result + tId;

      ArrayList<Map.Entry<Integer, HashSet<Integer>>> sortedEventEntries = 
      new ArrayList<>(producedEventSensorsData.entrySet());
      sortedEventEntries.sort(Comparator.comparing(Entry::getKey));
      for (Entry<Integer, HashSet<Integer>> eventEntry : sortedEventEntries) {
        int sensorId = eventEntry.getKey();
        result = 31 * result + sensorId;
        TreeSet<Integer> sortedVersions = new TreeSet<>(eventEntry.getValue());
        for (int version : sortedVersions) {
          result = 31 * result + stateTime - version;
        }
      }

      ArrayList<Map.Entry<Integer, HashSet<Integer>>> sortedStatusEntries = 
      new ArrayList<>(producedStatusSensorsData.entrySet());
      sortedStatusEntries.sort(Comparator.comparing(Entry::getKey));
      for (Entry<Integer, HashSet<Integer>> statusEntry : sortedStatusEntries) {
        int sensorId = statusEntry.getKey();
        result = 31 * result + sensorId;
        TreeSet<Integer> sortedVersions = new TreeSet<>(statusEntry.getValue());
        for (int version : sortedVersions) {
          result = 31 * result + stateTime - version;
        }
      }

      TaskBody taskBodyValue = tasksBody.get(tId);
      if (taskBodyValue.getExecutedData() != null) {
        for (Entry<Integer, TreeSet<Integer>> executedDataEntry : taskBodyValue.getExecutedData().entrySet()) {
          result = 31 * result + executedDataEntry.getKey();
          for (Integer version : executedDataEntry.getValue()) {
            result = 31 * result + stateTime - version;
          }
        }
      }

      HashMap<SimpleImmutableEntry<Integer, Integer>, TreeSet<Integer>> taskData = taskBodyValue.getDataDelivered();
      if (taskData != null) {
        ArrayList<Map.Entry<SimpleImmutableEntry<Integer, Integer>, TreeSet<Integer>>> sortedEntries = 
        new ArrayList<>(taskData.entrySet());
        sortedEntries.sort(Comparator
        .comparing((Map.Entry<SimpleImmutableEntry<Integer, Integer>, TreeSet<Integer>> entry) -> entry.getKey().getKey())
        .thenComparing(entry -> entry.getKey().getValue()));
        for (Map.Entry<SimpleImmutableEntry<Integer, Integer>, TreeSet<Integer>> entry : sortedEntries) {
          SimpleImmutableEntry<Integer, Integer> key = entry.getKey();
          TreeSet<Integer> versions = entry.getValue();
          int port = key.getKey();
          int sensorId = key.getValue();
          result = 31 * result + port;
          result = 31 * result + sensorId;    
          if (versions != null) {
            for (int version : versions) {
              result = 31 * result + stateTime - version;
            }
          }
        }
      } 
    }

    PriorityQueue<EventSet> copyQueue = new PriorityQueue<>(eventSetQueue);
    while (!copyQueue.isEmpty()) {
        EventSet eventSet = copyQueue.poll();
        result = 31 * result + eventSet.getEventSetSize();

        PriorityQueue<Integer> sortedIds = new PriorityQueue<>();
        for (Event upEvent : eventSet.getUpdates()) {
          sortedIds.add(upEvent.getTaskId());
        }
        while (!sortedIds.isEmpty()) {
          int id = sortedIds.poll();
          result = 31 * result + 31 * id;
          result = 31 * result + stateTime - eventSet.getEventSetTime();
        }
        sortedIds.clear();
        for (Event arEvent : eventSet.getArrivals()) {
          sortedIds.add(arEvent.getTaskId());
        }
        while (!sortedIds.isEmpty()) {
          int id = sortedIds.poll();
          result = 31 * result + 31 * id;
          result = 31 * result + stateTime - eventSet.getEventSetTime();
        }
    }
    return result;  
  }

}