package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

public class State implements InitializingConfigs {
  private int stateId;
  private HashSet<Integer> sourceIds = new HashSet<>();
  private HashMap<Integer, TaskBody> tasksBody = new HashMap<>();
  private int stateTime;
  private int idleProcessors;
  private PriorityQueue<EventSet> eventSetQueue = new PriorityQueue<>();
  private HashMap<Integer, HashSet<Integer>> producedSensorsData = new HashMap<>();

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
      copyProducedSensorsData(this.producedSensorsData, source.getProducedSensorsData());
    }

    copyTasksBody(source, tasksChanges);

    executionAndSchedulityRegulator(source);
    policiesChecker();
  }


  private void copyProducedSensorsData(HashMap<Integer, HashSet<Integer>> producedSensorsData1,
    HashMap<Integer, HashSet<Integer>> producedSensorsData2) {
      for (Entry<Integer, HashSet<Integer>> entry : producedSensorsData2.entrySet()) {
        Integer key = entry.getKey();
        HashSet<Integer> originalList = entry.getValue();
        HashSet<Integer> copiedList = new HashSet<>(originalList.size());
        for (Integer value : originalList) {
          copiedList.add(value);
        }          
        producedSensorsData1.put(key, copiedList);
      }    
    }

  private void copyTasksBody(State source, Map<Integer, TaskBody> tasksChanges) {
    for (Entry<Integer, TaskBody> sourceTaskBodyEntry : source.getTasksBody().entrySet()) {
      Integer key = sourceTaskBodyEntry.getKey();
      TaskBody sourceTaskBody = sourceTaskBodyEntry.getValue();
      if (!tasksChanges.containsKey(key)) {
        TaskBody newTaskBody = new TaskBody(sourceTaskBody);
        this.tasksBody.put(key, newTaskBody);
      }
      else if (tasksChanges.get(key).getTaskID() == 0) {
        if (eventSensors.contains(key)) {
          HashSet<Integer> sensorVersions =  producedSensorsData.computeIfAbsent(key, k -> new HashSet<Integer>());
          sensorVersions.add(stateTime);
        }
        else if (actuators.contains(key)) {
          for (Entry<Integer, HashSet<Integer>> producedSensorDataEntry : producedSensorsData.entrySet()) {
            Integer eventSensorId = producedSensorDataEntry.getKey();
            HashSet<Integer> sensorVersions =  producedSensorDataEntry.getValue();
            HashMap<Integer, TreeSet<Integer>> finalStageData = tasksChanges.get(key).integrateInputData();
            for (int version : findUniquePositives(sensorVersions)) {
              if (finalStageData.get(eventSensorId) != null) {
                if (finalStageData.get(eventSensorId).contains(version)) {
                  if (stateTime - version <= timingConstraints.get(1)) {
                    sensorVersions.add(-version);
                  }
                  else {
                    this.eventSetQueue.clear();
                    return;
                  }
                }
              }
            }
          }
        }
        tasksChanges.get(key).setTaskID(key);
      }
    }
    this.tasksBody.putAll(tasksChanges);
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

  public HashMap<Integer, HashSet<Integer>> getProducedSensorsData() {
    return producedSensorsData;
  }

  private void executionAndSchedulityRegulator(State source) {
    for (EventSet eventSet : this.eventSetQueue) {
        if (eventSet.getEventSetTime() == stateTime) {
            for (Event arrival : eventSet.getArrivals()) {
                int taskId = arrival.getTaskId();
                this.tasksBody.get(taskId).setExecuted(false);
            }
        }

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
  
  private void policiesChecker() {
    for (Entry<Integer, HashSet<Integer>> entry : producedSensorsData.entrySet()) {
      HashSet<Integer> value = entry.getValue();
      for (int version : findUniquePositives(value)) {
        if (stateTime - version > timingConstraints.get(1)) {
          this.eventSetQueue.clear();
          return;
        }
      }
    }
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

  public boolean isTasksBodyEqual(HashMap<Integer, TaskBody> newTasksBody, int diff) {
    for (Entry<Integer, TaskBody> newTaskBody : newTasksBody.entrySet()) {
      Integer newTaskId = newTaskBody.getKey();
      TaskBody thisInnerBody = tasksBody.get(newTaskId);
      TaskBody newInnerValue = newTaskBody.getValue();
      if(!thisInnerBody.isExecuted() == newInnerValue.isExecuted()) {
        return false;
      }
      HashMap<Integer, HashMap<Integer, TreeSet<Integer>>> newData = newInnerValue.getDataDelivered();
      HashMap<Integer, HashMap<Integer, TreeSet<Integer>>> thisData = thisInnerBody.getDataDelivered();
      if (newData != null){


        for (int newPort : newData.keySet()) {
          HashMap<Integer, TreeSet<Integer>> newPortData = newData.get(newPort);
          HashMap<Integer, TreeSet<Integer>> thisPortData = thisData.get(newPort);
          for (int sensorId : newPortData.keySet()) {
            if (!compareData(thisPortData.get(sensorId), newPortData.get(sensorId), diff)) {
              return false;
            }
          }
        }
      }
    }
    return true;
  }

  private boolean compareData(TreeSet<Integer> previousData, TreeSet<Integer> newData, int diff) {
    if (previousData == null && newData == null) {
      return  true;
    }
    if (previousData == null || newData == null) {
      return false;
    }
    if (newData.size() != previousData.size()) {
        return false;
    }
    Iterator<Integer> previousDataIterator = previousData.iterator();
    Iterator<Integer> newDataIterator = newData.iterator();

    while(previousDataIterator.hasNext()) {
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

    long result = 1L;
    int tId;
    for (tId = 1; tasksBody.containsKey(tId); tId++){
      result = 31 * result + tId;
      TaskBody taskBodyValue = tasksBody.get(tId);
      HashMap<Integer, HashMap<Integer, TreeSet<Integer>>> taskData = taskBodyValue.getDataDelivered();
      result = 31 * result + (taskBodyValue.isExecuted() ? 1 : 0); // Handle boolean field
      if (taskData != null) {
        for (int port : taskData.keySet()) {
          HashMap<Integer, TreeSet<Integer>> sensorsContent = taskData.get(port);
          for (int sensorId : sensorsContent.keySet()) {
            result = 31 * result + sensorId; // Handle non-null sensorId
            TreeSet<Integer> versions = sensorsContent.get(sensorId);
            if (versions != null) {
              for (int version : versions) {
                result = 31 * result + stateTime - version; // Handle null version
              }
            }
          }
        }
      }
      
    }

    PriorityQueue<EventSet> copyQueue = new PriorityQueue<>(eventSetQueue);
    while (!copyQueue.isEmpty()) {
        EventSet eventSet = copyQueue.poll();
        result = 31 * result + eventSet.getEventSetSize();
        result = 31 * result + Math.abs(stateTime - eventSet.getEventSetTime());

        PriorityQueue<Integer> sortedIds = new PriorityQueue<>();
        for (Event upEvent : eventSet.getUpdates()) {
          sortedIds.add(upEvent.getTaskId());
        }
        while (!sortedIds.isEmpty()) {
          int id = sortedIds.poll();
          result = 31 * result + 31 * id;
        }
        sortedIds.clear();
        for (Event arEvent : eventSet.getArrivals()) {
          sortedIds.add(arEvent.getTaskId());
        }
        while (!sortedIds.isEmpty()) {
          int id = sortedIds.poll();
          result = 31 * result + 31 * id;
        }
    }

    return result;  
  }

}