package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Collectors;
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
        int taskDuaration = getTaskById(key).getDuration();
        HashMap<Integer, TreeSet<Integer>> executionData = tasksBody.get(key).integrateInputData();
        int[] minMax = new int[2];
        if (timingConstraints.get(3) != null) {
          findMinAndMax(executionData, minMax);
          Integer min = minMax[0];
          Integer max = minMax[1];
          if (max - min > timingConstraints.get(3)) {
            this.eventSetQueue.clear();
            return;
          }
        }  
        if (eventSensors.contains(key)) {
          addVersion(producedEventSensorsData, key, stateTime);
        }
        else if (actuators.contains(key)) {
          HashMap<Integer, TreeSet<Integer>> finalStageData = sourceTaskBody.integrateInputData();
          for (Entry<Integer, HashSet<Integer>> producedEventSensorDataEntry : producedEventSensorsData.entrySet()) {
            Integer eventSensorId = producedEventSensorDataEntry.getKey();
            HashSet<Integer> eventSensorVersions =  producedEventSensorDataEntry.getValue();
            for (int version : eventSensorVersions) {
              if (finalStageData.get(eventSensorId) != null) {
                if (finalStageData.get(eventSensorId).contains(version)) {
                  if (stateTime + taskDuaration - version <= timingConstraints.get(1)) {
                    eventSensorVersions.remove(version);
                    if (eventSensorVersions.size() == 0) {
                      producedEventSensorsData.remove(eventSensorId);
                    }
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
            if (statusSensors.contains(sensorId)) {
              for (Integer version : finalDataEntry.getValue()) {
                if (stateTime + taskDuaration - version > timingConstraints.get(2)) {
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

  private void executionAndSchedulityRegulator(State source) {
    for (EventSet eventSet : this.eventSetQueue) {
      for (Event updateEvent : eventSet.getUpdates()) {
        for (EventSet sourceEventSet : source.getEventSetQueue()) {
          if (sourceEventSet.getEventSetTime() < updateEvent.getTime()) {
            TreeSet<Event> sourceArrivals = sourceEventSet.getArrivals();
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

    for (Entry<Integer, HashSet<Integer>> unsatisfiedVersionsEntry : producedEventSensorsData.entrySet()) {
      Integer sensorId = unsatisfiedVersionsEntry.getKey();
      HashSet<Integer> unsatisfiedVersions = unsatisfiedVersionsEntry.getValue();
      for (Integer version : unsatisfiedVersions) {
        boolean isExpired = true;
        boolean foundNonNull = false;
        for (TaskBody taskBody : tasksBody.values()) {
        TreeSet<Integer> executionVersions = taskBody.integrateInputData().get(sensorId);
          if (executionVersions != null) {
            foundNonNull = true;
            if (executionVersions.contains(version)) {
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
  }

  private void prophecy() {
    TreeSet <Integer> grayTasks = findGrayTasks(actuators, 0);
    isFeasible(grayTasks);

  }

  private TreeSet<Integer> findGrayTasks(ArrayList<Integer> givenTasks, int forecomingExecutionTime) {
    TreeSet<Integer> resultedTasks = new TreeSet<>();
    resultedTasks.addAll(givenTasks);
    HashMap<Integer, Integer> waitingTimes = calculateWaitingTimes(givenTasks, stateTime);
    for (Integer givenTask : givenTasks) {
      ArrayList<Integer> badTasks = new ArrayList<>();
      int taskDuaration = getTaskById(givenTask).getDuration();
      int totalSpentTime = taskDuaration + forecomingExecutionTime + waitingTimes.get(givenTask);
      Set<Integer> badPorts = new HashSet<>();
      for (Entry<SimpleImmutableEntry<Integer, Integer>, TreeSet<Integer>> dataOnPort : 
      tasksBody.get(givenTask).getDataDelivered().entrySet()) {
        if (badPorts.contains(dataOnPort.getKey().getKey())) {
          continue;
        }
        boolean badData = dataOnPort.getValue().stream()
        .anyMatch(version -> stateTime + totalSpentTime - version > timingConstraints.get(2));
        if (badData) {
          int badPort = dataOnPort.getKey().getKey();
          badPorts.add(badPort);
          badTasks.addAll(linkList.stream()
          .filter(link -> link.getToTask() == givenTask && link.getToPort() == badPort)
          .map(Link::getFromTask)
          .collect(Collectors.toList()));
        }
      }
      resultedTasks.addAll(findGrayTasks(badTasks, totalSpentTime));
    }
    return resultedTasks;
  }

  private boolean isFeasible(TreeSet<Integer> grayTasks) {
    int abstractCpus = this.idleProcessors;
    HashSet<Integer> globalQueue = new HashSet<>(grayTasks);
    HashMap<Integer, List<Integer>> parentsMap = calculateParents(grayTasks);
    HashMap<Integer, Integer> totalResponseTime = new HashMap<>();
    while (!globalQueue.isEmpty()) {
      boolean progress = false;
      for (Integer grayTask : grayTasks) {
        if (globalQueue.contains(grayTask)) {
          boolean hasUnresolvedParents = false;
          for (Integer parent : parentsMap.getOrDefault(grayTask, Collections.emptyList())) {
            if (globalQueue.contains(parent)) {
              hasUnresolvedParents = true;
              break;
            }
          }
          if (!hasUnresolvedParents) {
            // taskDuaration + maxWaitingTime + 
            globalQueue.remove(grayTask);
            progress = true;
          }
        }
      }
      if (!progress) {
        // If no progress was made in this iteration, then there's a circular dependency
        return false;
      }
    }
    // If we exit the loop, all tasks were resolved
    return true;
  }

  private boolean isFeasible2(TreeSet<Integer> grayTasks) {
    int abstractCpus = this.idleProcessors;
    HashSet<Integer> globalQueue = new HashSet<>(grayTasks);
    HashMap<Integer, List<Integer>> parentsMap = calculateParents(grayTasks);
    HashMap<Integer, Integer> totalResponseTime = new HashMap<>();
    PriorityQueue<EventSet> abstractUpdates = getAllUpdates(eventSetQueue);
    for (int abstractTime = stateTime; abstractTime <= stateTime + timingConstraints.get(2); abstractTime++) {
      if (abstractCpus == 0) {
        TreeSet<Event> nextUpdates = abstractUpdates.poll().getUpdates();
        abstractCpus += nextUpdates.size();
        abstractTime = nextUpdates.first().getTime();
      }
      HashMap<Integer, Integer> waitingTimes = calculateWaitingTimes(new ArrayList<Integer>(globalQueue), abstractTime);
      for (Integer grayTask : grayTasks) {
        if (globalQueue.contains(grayTask)) {
          boolean hasUnresolvedParents = false;
          for (Integer parent : parentsMap.getOrDefault(grayTask, Collections.emptyList())) {
            if (globalQueue.contains(parent)) {
              hasUnresolvedParents = true;
              break;
            }
          }
          if (!hasUnresolvedParents && waitingTimes.get(grayTask) == 0) {
            // taskDuaration + maxWaitingTime + 
            globalQueue.remove(grayTask);
            // progress = true;
          }
        }
      }      
    }
    if (globalQueue.isEmpty()) {
      return true;
    }
    else {
      return false;
    }
  }

  private HashMap<Integer, Integer> calculateWaitingTimes(ArrayList<Integer> givenTasks, int refrenceTime) {
    HashMap<Integer, Integer> result = new HashMap<>();
    PriorityQueue<EventSet> copyQueue = eventSetQueue;
    while (!copyQueue.isEmpty()) {
      EventSet eventSet = copyQueue.poll();
      for (int givenTask : givenTasks){
        if (!result.containsKey(givenTask)) {
          for (Event event : eventSet.getArrivals()) {
            if (event.getTaskId() == givenTask) {
              int waitingTime = Math.max(0, event.getTime() - refrenceTime);
              result.put(givenTask, waitingTime);
              break;
            }
          }
        }
      }
    }
    return result;
  }

  private PriorityQueue<EventSet> getAllUpdates(PriorityQueue<EventSet> eventSetQueue2) {
    PriorityQueue<EventSet> result = new PriorityQueue<>();
    for (EventSet eventSet : eventSetQueue2) {
      if (eventSet.getEventSetTime() >= stateTime) {
        EventSet newEventSet = new EventSet(eventSet.getEventSetTime());
        newEventSet.setUpdates(eventSet.getUpdates());
        result.add(newEventSet);
      }
    }
    return result;
  }

  private HashMap<Integer, List<Integer>> calculateParents(TreeSet<Integer> grayTasks) {
    HashMap<Integer, List<Integer>> result = new HashMap<>();
    for (Integer grayTask : grayTasks) {
      result.put(grayTask, TaskSet.getParents(grayTask));
    }
    return result;
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

      TaskBody taskBodyValue = tasksBody.get(tId);
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