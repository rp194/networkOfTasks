package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;

public class SystemModel implements InitializingConfigs {
    private ArrayList<Task> tasks = new ArrayList<>();
    private ArrayList<Link> links = new ArrayList<>();
    private ArrayList<State> states = new ArrayList<>();
    private Map<Integer, Task> taskMap = new HashMap<>();
    private HashSet<Integer> sensorIds = new HashSet<>();
    private HashMap<Long, HashSet<Integer>> hashTable = new HashMap<>();
    private HashMap<Integer, ArrayList<String>> transitions = new HashMap<>();
    private int currentStateTime = -1;
    private int nextStateId;
    private final String LOG_FILE_PATH = 
    "E:\\Documents\\java_codes\\networkOfTasks\\demo\\src\\main\\java\\com\\example\\log.txt";
    private final String Dot_FILE_PATH =
    "E:\\Documents\\java_codes\\networkOfTasks\\demo\\src\\main\\java\\com\\example\\dot.dot";

    public SystemModel(int numberOfProcessors) throws Exception {
        InitializingConfigs.loadTasksAndLinks();        
        InitializingConfigs.loadConstraints();
        this.tasks = new ArrayList<Task>(taskList);
        this.links = new ArrayList<Link>(linkList);
        this.initializeEventQueue(numberOfProcessors);
    }

    private void initializeEventQueue(int numberOfProcessors) {
        int eventTime = 0;
        nextStateId = 0;
        int taskSetSize = tasks.size();
        HashMap<Integer, TaskBody> tasksChanges = new HashMap<>(taskSetSize);
        PriorityQueue<EventSet> initEventSetQueue = new PriorityQueue<>();
        initializeTasksAndDetermineSensors();        
        State initState = new State();
        String transitionLog = "A>T[";
        for (Task task : tasks) {
            int taskId = task.getId();
            addEvent(initEventSetQueue, new Event(taskId, eventTime, Event.EventType.ARRIVAL));
            HashMap<SimpleImmutableEntry<Integer, Integer>, TreeSet<Integer>> initialData = new HashMap<>();
            for (int port : task.getInPorts()) {
                for (int sensorId : sensorIds) {
                    initialData.put(new SimpleImmutableEntry<>(port, sensorId), new TreeSet<>());
                }
            }
            TaskBody taskBody = new TaskBody(taskId, new HashMap<>(), initialData);
            tasksChanges.put(taskId, taskBody);
            transitionLog += taskId + ",";
        }
        transitionLog += "]";
        stateHandler(initState, eventTime, numberOfProcessors, tasksChanges, initEventSetQueue, transitionLog);
    }

    public void stateSpaceGenerator(int endTime) {
        int stateIterator = 0;
        while(true) {
            int stateSize = states.size();
            if (stateSize <= stateIterator) {
                break;
            }
            State currentState = states.get(stateIterator);
            currentStateTime = currentState.getStateTime();
            if (currentStateTime <= endTime) {
                if(!currentState.getEventSetQueue().isEmpty()) {
                    eventSetHandler(currentState);
                }
            }
            stateIterator += 1;
        }
    }

    private void eventSetHandler(State currentState) {
        int idleProcessors = currentState.getIdleProcessors();
        PriorityQueue<EventSet> futureQueue = new PriorityQueue<>();
        HashMap<Integer, TaskBody> tasksChanges = new HashMap<>();
        TreeSet<Event> pastUpdates = new TreeSet<>();
        ArrayList<Event> pastArrivals = new ArrayList<>();
        PriorityQueue<EventSet> stateQueue = currentState.getEventSetQueue();
        separatePastEvents(stateQueue, pastUpdates, pastArrivals, futureQueue);

        if(!pastUpdates.isEmpty()) {
            TreeSet<Event> wpastUpdates = (TreeSet<Event>) pastUpdates.descendingSet();
            PriorityQueue<EventSet> futureAndArrivals = new PriorityQueue<>(futureQueue);
            for (Event arrivalEvent : pastArrivals) {
                addEvent(futureAndArrivals, arrivalEvent);
            }
            String transitionLog = "U>T[";
            for(Event pastUpdate: wpastUpdates) {
                processTheUpdate(currentState, pastUpdate, tasksChanges);
                idleProcessors += 1;
                transitionLog += pastUpdate.getTaskId() + ",";
            }
            transitionLog += "]";

            stateHandler(currentState, currentStateTime, idleProcessors, tasksChanges, futureAndArrivals, transitionLog);
            return;
        }

         if(!pastArrivals.isEmpty()) {
            String transitionLog = "+1";
            stateHandler(currentState, currentStateTime + 1, idleProcessors, tasksChanges, stateQueue, transitionLog);
            if(idleProcessors > 0) {
                transitionLog = "";
                for (Event pastArrival : pastArrivals) {
                    transitionLog = "A>T[" + pastArrival.getTaskId() + "]";
                    processTheArrival(pastArrival, stateQueue, currentState,tasksChanges, transitionLog);
                }
            }
        } else {   
            int nextStateTime = futureQueue.peek().getEventSetTime();
            String transitionLog = "+" + (nextStateTime - currentStateTime);
            stateHandler(currentState, nextStateTime, idleProcessors, tasksChanges, futureQueue, transitionLog);
        }
    }

    private void initializeTasksAndDetermineSensors() {
        for (Task task : tasks) {
            taskMap.put(task.getId(), task);
            if (task.getInPorts().length == 0) {
                sensorIds.add(task.getId());
            }
        }
    }    

    private void separatePastEvents(PriorityQueue<EventSet> stateQueue, TreeSet<Event> pastUpdates, ArrayList<Event> pastArrivals, PriorityQueue<EventSet> futureQueue) {
        for (EventSet eventSet : stateQueue) {
            if (eventSet.getEventSetTime() <= currentStateTime) {
                pastUpdates.addAll(eventSet.getUpdates());
                pastArrivals.addAll(eventSet.getArrivals());
            } else {
                futureQueue.add(eventSet);
            }
        }
    }

    private void processTheUpdate(State currentState, Event updateEvent, HashMap<Integer, TaskBody> tasksChanges) {
        int taskId = updateEvent.getTaskId();
        HashMap<Integer, TreeSet<Integer>> resultedOutput;
        TaskBody tt = currentState.getTaskBody(taskId);
        TaskBody taskBody = tasksChanges.computeIfAbsent(taskId, k -> new TaskBody(tt));
        HashMap<Integer, TreeSet<Integer>> executedData =  tt.integrateInputData();
        taskBody.setExecutedData(executedData);
        taskBody.setTaskID(0);
        if (sensorIds.contains(taskId)) {
            resultedOutput = createSensorData(taskId, currentStateTime);
            // taskBody.putData(0, resultedOutput);
            // HashMap<Integer, HashMap<Integer, TreeSet<Integer>>> taskData = taskBody.getDataDelivered();
            // HashMap<Integer, TreeSet<Integer>> dataOnPort = taskData.computeIfAbsent(int.O, k -> new HashMap<>());
            // TreeSet<Integer> setOfData = dataOnPort.computeIfAbsent(taskId, k -> new TreeSet<>());
            // setOfData.add(currentStateTime);
        } else {
            resultedOutput = currentState.getTaskBody(taskId).integrateInputData();
        }
                
        for (Link link : links) {
            if (link.getFromTask() == taskId) {
                Task nextTask = getTaskById(link.getToTask());
                int nextInPort = link.getToPort();
                int nextTaskId = nextTask.getId();
                tasksChanges.compute(nextTaskId, (k, v) -> {
                    if (v == null) {
                        v = new TaskBody(currentState.getTaskBody(nextTaskId));
                    }
                    for (int sensorId : resultedOutput.keySet()) {
                        SimpleImmutableEntry<Integer, Integer> entryKey = new SimpleImmutableEntry<>(nextInPort, sensorId);
                        TreeSet<Integer> dataForNextTask = new TreeSet<>();
                        v.getDataDelivered().put(entryKey, dataForNextTask);
                        dataForNextTask.addAll(resultedOutput.get(sensorId));
                    }
                    return v;
                });
            }
        }
        
    }




    private HashMap<Integer, TreeSet<Integer>> createSensorData(int taskId, int timestamp) {
        HashMap<Integer, TreeSet<Integer>> data = new HashMap<>();
        TreeSet<Integer> set = new TreeSet<>();
        set.add(timestamp);
        data.put(taskId, set);
        return data;  
    }

    private void processTheArrival(Event currentArrival, PriorityQueue<EventSet> stateQueue, State currentState,
    HashMap<Integer, TaskBody> tasksChanges, String transitionLog) {
        int idleProcessors = currentState.getIdleProcessors();
        int taskId = currentArrival.getTaskId();
        Task task = getTaskById(taskId);
        int taskPeriod = task.getPeriod();
        PriorityQueue<EventSet> finalQueue = new PriorityQueue<>(stateQueue);
        removeEvent(finalQueue, currentArrival);
        Event resultedArrivalEvent = new Event(task.getId(), nextArrival(taskPeriod, currentStateTime), Event.EventType.ARRIVAL);
        addEvent(finalQueue, resultedArrivalEvent);
        idleProcessors -= 1;
        Event resultedUpdateEvent = schedule(task, currentStateTime);
        addEvent(finalQueue, resultedUpdateEvent);
        tasksChanges.compute(taskId, (k, v) -> {
            if (v == null) {
                v = new TaskBody(currentState.getTaskBody(taskId));
            }
            return v;
        });
        stateHandler(currentState, currentStateTime, idleProcessors, tasksChanges, finalQueue, transitionLog);
    }

    private void updateEventSet(PriorityQueue<EventSet> stateQueue, Event event, boolean addEvent) {
        EventSet targetEventSet = null;
        for (EventSet eventSet : stateQueue) {
            if (eventSet.getEventSetTime() == event.getTime()) {
                targetEventSet = eventSet;
                break;
            }
        }    
        if (targetEventSet != null) {
            Event.EventType eventType = event.getType();
            HashSet<Event> updateEvents = new HashSet<>(targetEventSet.getUpdates());
            HashSet<Event> arrivalEvents = new HashSet<>(targetEventSet.getArrivals());
            EventSet newEventSet = new EventSet(event.getTime());
            if (addEvent) {
                switch (eventType) {
                    case ARRIVAL:
                        arrivalEvents.add(event);
                        break;
                    case UPDATE:
                        updateEvents.add(event);
                        break;
                }
            } else {
                switch (eventType) {
                    case ARRIVAL:
                        arrivalEvents.remove(event);
                        break;
                    case UPDATE:
                        updateEvents.remove(event);
                        break;
                }
            }    
            newEventSet.setArrivals(arrivalEvents);
            newEventSet.setUpdates(updateEvents);
            stateQueue.remove(targetEventSet);
            stateQueue.add(newEventSet);
        } else if (addEvent) {
            targetEventSet = new EventSet(event.getTime());
            targetEventSet.insertEvent(event);
            stateQueue.add(targetEventSet);
        }
    }
    
    private void addEvent(PriorityQueue<EventSet> stateQueue, Event newEvent) {
        updateEventSet(stateQueue, newEvent, true);
    }
    
    private void removeEvent(PriorityQueue<EventSet> stateQueue, Event currentEvent) {
        updateEventSet(stateQueue, currentEvent, false);
    }

    private Event schedule(Task task, int currentTime) {
        int startTime = currentTime;
        Event resEvent = new Event(task.getId(), startTime + task.getDuration(), Event.EventType.UPDATE);
        return resEvent;
    }

    private int nextArrival(int period, int currentTime) {
        return ((currentTime + period)/period) * period;
    }

    private Task getTaskById(int taskId) {
        return taskMap.get(taskId);
    }

    private void 
    stateHandler(State currentState, int newStateTime, int idleProcessors, HashMap<Integer, TaskBody> changes, PriorityQueue<EventSet> resultedQueue, String transitionLog) {
        int cuid = currentState.getStateID();
        State newState = new State(currentState, nextStateId, newStateTime, idleProcessors, changes, resultedQueue);
        State foundWrapper  = findWrapper(newState);
        int sourceId = currentState.getStateID();
        if (foundWrapper == null) {
            addNewState(newState, sourceId, transitionLog);
        } else {
            linkToSimilarState(foundWrapper, newState, sourceId, transitionLog);
        }
    }
    
    private void addNewState(State newState, int sourceId, String transitionLog) {
        states.add(newState);
        Long newHashCode = newState.longHashCode();
        HashSet<Integer> sIDs = 
        hashTable.get(newHashCode) == null ? new HashSet<>() : hashTable.get(newHashCode);
        sIDs.add(nextStateId);
        if (!hashTable.containsKey(newHashCode)) {
            hashTable.put(newHashCode, sIDs);
        }
        String transitionMessage = nextStateId + "#" + transitionLog;
        addNewTransition(sourceId, transitionMessage);
        nextStateId += 1;
    }

    private void linkToSimilarState(State foundWrapper, State newState, int sourceId, String transitionLog) {
        foundWrapper.insertNewSource(newState.getSourceIds());
        int foundWrapperId = foundWrapper.getStateID();
        String transitionMessage = foundWrapperId + "#" + transitionLog;
        addNewTransition(sourceId, transitionMessage);
    }

   private void addNewTransition(int sourceId, String transitionMessage) {
       transitions.computeIfAbsent(sourceId, k -> new ArrayList<>()).add(transitionMessage);
   }

    private State findWrapper(State newState) {
        Long newHashCode = newState.longHashCode();
        PriorityQueue<EventSet> resultedQueue = newState.getEventSetQueue();
        HashSet<Integer> previousStateIDs = hashTable.get(newHashCode);
        if (previousStateIDs != null) {
            for (int previousStateID : previousStateIDs) {
                State previousState = states.get(previousStateID);
                if (isWrapper(newState, previousState, resultedQueue)) {
                    return previousState;
                }
            }
        }
        return null;   
    }

    private boolean isWrapper(State newState, State previousState, PriorityQueue<EventSet> newQueue) {
        if (previousState.getEventSetQueue().isEmpty()) {
            return true;
        }
        int previousSize = previousState.getEventSetQueue().size();
        if (newState.getIdleProcessors() != previousState.getIdleProcessors() ||
                newState.getEventSetQueue().size() != previousSize) {
            return false;
        }
        int newStateTime = newState.getStateTime();
        HashMap<Integer, TaskBody> newTasksBody = newState.getTasksBody();
        int previousStateTime = previousState.getStateTime();
        int diffTime = newStateTime - previousStateTime;
        HashMap<Integer, HashSet<Integer>> previousUnsatisfiedStatus = previousState.getUnsatisfiedStatusVersions();
        HashMap<Integer, HashSet<Integer>> newUnsatisfiedStatus = newState.getUnsatisfiedStatusVersions();
        if (previousUnsatisfiedStatus.size() != newUnsatisfiedStatus.size()) {
            return false;
        }
        for (Entry<Integer, HashSet<Integer>> previousUnsatisfiedEntry : previousUnsatisfiedStatus.entrySet()) {
            int sensorId = previousUnsatisfiedEntry.getKey();
            if (!newUnsatisfiedStatus.containsKey(sensorId)) {
                return false;
            }
            boolean isEqual = newState
            .compareData(previousUnsatisfiedEntry.getValue(), newUnsatisfiedStatus.get(sensorId), diffTime);
            if (!isEqual) { 
                return false;
            }
        }
        if (!previousState.isTasksBodyEqual(newState, newTasksBody, diffTime)) {
            return false;
        }
        PriorityQueue<EventSet> previousCopy = new PriorityQueue<>(previousState.getEventSetQueue());
        PriorityQueue<EventSet> newCopy = new PriorityQueue<>(newQueue);
        for (int i = 0; i < previousSize; i++) {
            EventSet newSet = newCopy.poll();
            EventSet previousSet = previousCopy.poll();
            if (newSet.getEventSetTime() - previousSet.getEventSetTime() != diffTime) {
                return false;
            }
            if (!compareEvents(newSet.getUpdates(), previousSet.getUpdates())) {
                return false;
            }
            if (!compareEvents(newSet.getArrivals(), previousSet.getArrivals())) {
                return false;
            }
        }
        return true;
    }

    private boolean compareEvents(HashSet<Event> list1, HashSet<Event> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }

        Set<Integer> taskIds = new HashSet<>();
        for (Event event : list1) {
            taskIds.add(event.getTaskId());
        }

        for (Event event : list2) {
            if (!taskIds.contains(event.getTaskId())) {
                return false;
            }
        }

        return true;
    }

    public State getsStateById(int id) {
        for (State state: states) {
            if (state.getStateID()==id) {
                return state;
            }
        }
        return null;
    }
        
    public void createLogFile() {
        System.out.println(hashTable.size());
        for (HashSet<Integer> value : hashTable.values()) {
            if (value.size() != 1) {
                System.out.println(value);
            }
        }
        clearLogFile(LOG_FILE_PATH);
        try {
            FileWriter fileWriter = new FileWriter(LOG_FILE_PATH, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            for(State state : states) {
                bufferedWriter.newLine();
                bufferedWriter.write(state.toString());
            }
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void clearLogFile(String filePath) {
        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write("");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();      
        }
    }

    public void createDotFile() {
        System.out.println(hashTable.size());
        clearLogFile(Dot_FILE_PATH);
        try {
            FileWriter fileWriter = new FileWriter(Dot_FILE_PATH, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            HashSet<Integer> turningStates = new HashSet<>();
            bufferedWriter.write("digraph state_space {");
            bufferedWriter.write("\n"
                                    + "  size = \"1,1.9\";\n"
                                    + "  ratio=\"fill\";\n"
                                    + "  node [shape=box, fontsize=90, style=filled, fillcolor=lightblue, width=2, height=1];\n"
                                    + "  edge [fontsize=80, style=bold];\n"
                                    + "  splines = true;\n"
                                    + "  overlap = false;");
            for(Entry<Integer, ArrayList<String>> transition : transitions.entrySet()) {
                int cID = transition.getKey();
                for (String toSIDString : transition.getValue()) {
                    String [] transitionMessage = toSIDString.split("#");
                    bufferedWriter.newLine();
                    int sID = Integer.parseInt(transitionMessage[0]);
                    loopCheck(cID, sID, turningStates);
                    bufferedWriter.write("  " + cID + " -> ");
                    if(getsStateById(Integer.parseInt(transitionMessage[0])).getEventSetQueue().size() == 0) {
                        String invalidState = "\"" + cID + " invalidate\"";
                        bufferedWriter.write(invalidState);
                        bufferedWriter.write(" [label = \"" + transitionMessage[1] + "\"];\n  ");
                        bufferedWriter.write(invalidState + " [shape=circle, fontsize=7, style=filled, fillcolor=orange, width=0.3, height=0.3]");
                        continue;
                    }
                    bufferedWriter.write(transitionMessage[0]);
                    if (cID >= sID) {
                        bufferedWriter.write(" [label = \"" + transitionMessage[1] + "\"];");
                    } else {
                        bufferedWriter.write(" [label = \"" + transitionMessage[1] + "\", weight=1];");
                    }
                }
            }
            for (int turningState : turningStates) {
                    bufferedWriter.write("\n  " + turningState + " [shape=diamond]");
            }
            bufferedWriter.write("\n}");
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }
   
    private void loopCheck(int cID, int sID, HashSet<Integer> turningStates) {
        loopCheckRecursive(cID, sID, turningStates, new HashSet<>());
    }

    private void loopCheckRecursive(int cID, int sID, HashSet<Integer> turningStates, HashSet<Integer> visited) {
        if (cID == 0 || cID < sID || visited.contains(cID) || turningStates.contains(sID)) {
            return;
        }        
        visited.add(cID);   
        HashSet<Integer> cIDsOfCID = getsStateById(cID).getSourceIds();
        if (cIDsOfCID.contains(sID)) {
            turningStates.add(sID);
            return;
        } else {
            for (int ancOfCID : cIDsOfCID) {
                loopCheckRecursive(ancOfCID, sID, turningStates, visited);
            }
        }
    }
    
}