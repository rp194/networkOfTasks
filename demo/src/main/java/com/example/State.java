package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class State implements TasksLinksPorts {
  private int stateId;
  private HashSet<Integer> sourceIds = new HashSet<>();
  private HashMap<Integer, HashMap<Object, Object>> tasksBody = new HashMap<>();
  private HashSet<Integer> ancestors = new HashSet<>();
  private int stateTime;
  private int idleProcessors;
  private PriorityQueue<EventSet> eventSetQueue = new PriorityQueue<>();

  public State() {/*:)) */}

  public
  State(State source, int id, int stateTime, int idleProcessors, HashMap<Integer, ArrayList<Integer>> changes, PriorityQueue<EventSet> eventSetQueue) {
    this.stateId = id;
    this.idleProcessors = idleProcessors;
    this.stateTime = stateTime;
    this.eventSetQueue.addAll(eventSetQueue);
    this.ancestors.addAll(source.getAncestors());
    this.ancestors.addAll(sourceIds);
    Set<Integer> sourceKeySet = source.getTasksBody().keySet();
    for (Integer key: sourceKeySet) {
        if (sourceKeySet.contains(key)) {
            HashMap<Object, Object> innerMap = new HashMap<>();
            HashMap<Object, Object> body = new HashMap<>(source.getTasksBody().get(key));
            for (Map.Entry<Object,Object> entry : body.entrySet()) {
                innerMap.put(entry.getKey(), entry.getValue());
            }
            this.tasksBody.put(key, innerMap);
        }
      }
    
    for (Map.Entry<Integer, ArrayList<Integer>> taskChange : changes.entrySet()) {
      HashMap<Object, Object> innerMap = new HashMap<>();
      Task task = getTaskById(taskChange.getKey());
      ArrayList<Integer> change = taskChange.getValue();
      Port[] inPorts = task.getInPorts();
      change.stream().filter(portNumber -> portNumber > 0)
      .forEach(portNumber -> innerMap.put(inPorts[portNumber-1], stateTime));    
      
      if (source.getEventSetQueue().isEmpty()) {
        this.tasksBody.put(taskChange.getKey(), innerMap);
      } else {
        this.tasksBody.get(taskChange.getKey()).putAll(innerMap);
      }
      boolean toExit = false;
      for (EventSet eventSet : eventSetQueue) {
        if (eventSet.getEventSetTime() == stateTime && !eventSet.getArrivals().isEmpty()) {
          innerMap.put("isExecuted", false);
          toExit = true;
        }
      }
      if (!toExit) {
        boolean isExecuted = change.contains(0);
        if(isExecuted) {
          innerMap.put("isExecuted", true);
        }
      }
    }

    if (source.getEventSetQueue().isEmpty()) {
      for (Map.Entry<Integer, HashMap<Object, Object>> taskBody : this.tasksBody.entrySet()) {
        Task task = getTaskById(taskBody.getKey());
        for (Port inPort: task.getInPorts()) {
          taskBody.getValue().put(inPort, 0);
      }
      this.sourceIds.add(-1);
      }
    } else {
      this.sourceIds.add(source.getStateID());

    }
    policyChecker(source);
  }

  public HashSet<Integer> getAncestors() {
    return ancestors;
  }

  public State(State source) {
    this.stateId = source.getStateID();
    this.stateTime = source.getStateTime();
    this.idleProcessors = source.getIdleProcessors();
    this.eventSetQueue.addAll(source.getEventSetQueue());
    for (Map.Entry<Integer, HashMap<Object, Object>> taskBody : source.getTasksBody().entrySet()) {
        HashMap<Object, Object> innerMap = new HashMap<>();
        HashMap<Object, Object> body = taskBody.getValue();
        for (Map.Entry<Object,Object> entry : body.entrySet()) {
            innerMap.put(entry.getKey(), entry.getValue());
        }
        this.tasksBody.put(taskBody.getKey(), innerMap);
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

  public HashMap<Integer, HashMap<Object, Object>> getTasksBody() {
    return tasksBody;
  }

  public PriorityQueue<EventSet> getEventSetQueue() {
    return this.eventSetQueue;
  }

  private void policyChecker(State source) {
    for (EventSet eventSet2 : this.eventSetQueue) {
      for (Event updateEvent : eventSet2.getUpdates()) {
        for (EventSet sourceEventSet : source.getEventSetQueue()) {
          if (sourceEventSet.getEventSetTime() < updateEvent.getTime()) {
            ArrayList<Event> sourceArrivals = sourceEventSet.getArrivals();
            for (Event sourceArrival : sourceArrivals) {
              int taskId = sourceArrival.getTaskId();
              Task task = getTaskById(taskId);
              if (sourceArrival.getTaskId() == updateEvent.getTaskId()) {
                if (updateEvent.getTime() > sourceArrival.getTime() + task.getDeadline()) {
                  this.eventSetQueue.clear();
                  return;
                }
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
      sb.append("\n  C>");
      for (Integer sourceId : sourceIds) {
          sb.append(sourceId).append(",");
      }
      sb.append("\n  @").append(stateTime);
      sb.append("\n  P:").append(idleProcessors);
      sb.append("\n  T[]:");
      for (Map.Entry<Integer, HashMap<Object, Object>> entry : tasksBody.entrySet()) {
        int taskId = entry.getKey();
        HashMap<Object, Object> body = entry.getValue();
        sb.append("\n  T").append(taskId);
        for (Map.Entry<Object, Object> info : body.entrySet()) {
          if(info.getKey().equals("isExecuted")) {
            sb.append("\n    E=" );
            String invoked = (Boolean) info.getValue() == true? "T" : "F";
            sb.append(invoked);
          } else {
            sb.append("\n    " + info.toString());
          }
        }
      }
      sb.append("\n  Q: ");
      for (EventSet eventSet: eventSetQueue) {
        for (Event up: eventSet.getUpdates()) {
          sb.append("\n  " + "U>T" + up.getTaskId() + "@" + up.getTime());
        }
        for (Event ar: eventSet.getArrivals()) {
          sb.append("\n  " + "A>T" + ar.getTaskId() + "@" + ar.getTime());
        }
      }
      
      return sb.append("\n").toString();
  }

  public boolean isTasksBodyEqual(HashMap<Integer, HashMap<Object, Object>> otherTasksBody, int diff) {
    for (Map.Entry<Integer, HashMap<Object, Object>> otherTaskBody : otherTasksBody.entrySet()) {
      int otherTaskId = otherTaskBody.getKey();
      HashMap<Object, Object> thisInnerBody = tasksBody.get(otherTaskId);
      HashMap<Object, Object> otherInnerValue = otherTaskBody.getValue();
      if(!thisInnerBody.get("isExecuted").equals(otherInnerValue.get("isExecuted"))) {
        return false;
      }
      for (Port otherPort : getTaskById(otherTaskId).getInPorts()) {
        if((int) otherInnerValue.get(otherPort) - (int) thisInnerBody.get(otherPort) != diff) {
          return false;
        } 
      }
    }
    return true;
  }
  

  public Long longHashCode() {
    if (eventSetQueue.isEmpty()) {
        return -101L;
    }

    long result = 1L;
    result = 31 * result + idleProcessors;
    result = 31 * result + eventSetQueue.size();
    for (Map.Entry<Integer, HashMap<Object, Object>> entry : tasksBody.entrySet()) {
      int j = ((boolean) entry.getValue().get("isExecuted")) ? 1 : 0;
        result = 31 * result + j;

        Task task = getTaskById(entry.getKey());
        for (Port port : task.getInPorts()) {
            result = 31 * result + stateTime - (int) entry.getValue().get(port);
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