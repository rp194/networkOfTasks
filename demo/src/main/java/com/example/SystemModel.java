package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.List;
import java.util.Map.Entry;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class SystemModel implements TasksLinksPorts {
    private ArrayList<Task> tasks = new ArrayList<>();
    private ArrayList<Link> links = new ArrayList<>();
    private ArrayList<State> states = new ArrayList<>();
    private HashMap<Long, HashSet<Integer>> hashTable = new HashMap<>();
    private HashMap<Integer, ArrayList<String>> transitions = new HashMap<>();
    int currentStateTime = -1;
    private int nextStateId;
    private final String LOG_FILE_PATH = 
    "E:\\Documents\\java_codes\\networkOfTasks\\demo\\src\\main\\java\\com\\example\\log.txt";
    private final String Dot_FILE_PATH =
    "E:\\Documents\\java_codes\\networkOfTasks\\demo\\src\\main\\java\\com\\example\\dot.dot";

    public SystemModel(int numberOfProcessors) throws Exception {
        TasksLinksPorts.loadTasksAndLinks();        
        this.tasks = new ArrayList<Task>(taskList);
        this.links = new ArrayList<Link>(linkList);
        this.initializeEventQueue(numberOfProcessors);
    }

    private void initializeEventQueue(int numberOfProcessors) {
        int eventTime = 0;
        nextStateId = 0;
        HashMap<Integer ,ArrayList<Integer>> tasksChanges = new HashMap<>();
        PriorityQueue<EventSet> initEventSetQueue = new PriorityQueue<>();
        State initState = new State();
        String transitionLog = "A>T[";
        for (Task task : tasks) {
            int taskId = task.getId();
            addEvent(initEventSetQueue, new Event(taskId, eventTime, Event.EventType.ARRIVAL));
            updateTheTasksChanges(tasksChanges, taskId, -1);
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
            if (stateSize % 10000 ==0) {
                System.out.println(stateSize);
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
        HashMap<Integer, ArrayList<Integer>> tasksChanges = new HashMap<>();
        ArrayList<Event> pastUpdates = new ArrayList<>();
        ArrayList<Event> pastArrivals = new ArrayList<>();
        for(EventSet eventSet : currentState.getEventSetQueue()) {
            if (eventSet.getEventSetTime() <= currentState.getStateTime()) {
                pastUpdates.addAll(eventSet.getUpdates());
                pastArrivals.addAll(eventSet.getArrivals());
                // ArrayList<Event> setOfUpdates = eventSet.getUpdates();
                // ArrayList<Event> setOfArrivals = eventSet.getArrivals();
                // for(Event event : setOfUpdates) {    
                //     pastUpdates.add(event);
                // }
                // for(Event event : setOfArrivals) {
                //     pastArrivals.add(event);
                // }
            } else {
                ArrayList<Event> setOfEvents = eventSet.getAllEvents();
                for(Event event : setOfEvents) {
                    addEvent(futureQueue, event);
                }
            }
        }

        if(!pastUpdates.isEmpty()) {
            PriorityQueue<EventSet> futureAndArrivals = new PriorityQueue<>(futureQueue);
            for (Event arrivalEvent : pastArrivals) {
                addEvent(futureAndArrivals, arrivalEvent);
            }
            String transitionLog = "U>T[";
            for(Event pastUpdate: pastUpdates) {
                processtheUpdate(pastUpdate, tasksChanges);        
                idleProcessors += 1;
                transitionLog += pastUpdate.getTaskId() + ",";

            }
            transitionLog += "]";
            stateHandler(currentState, currentStateTime, idleProcessors, tasksChanges, futureAndArrivals, transitionLog);
            return;
        }

        // if (idleProcessors == 0) {
        //     ArrayList<EventSet> sortedQueue = new ArrayList<> (currentState.getEventSetQueue());
        //     for (EventSet eventSet : sortedQueue) {
        //         if (!eventSet.getUpdates().isEmpty()) {
        //             int nextStateTime = eventSet.getEventSetTime();
        //             String transitionLog = "+" + (nextStateTime - currentStateTime);
        //             stateHandler(currentState, nextStateTime, idleProcessors, tasksChanges, futureQueue, transitionLog);
        //             break;
        //         }
        //     }
        // }
        // else
         if(!pastArrivals.isEmpty()) {
            
            for(Event pastArrival : pastArrivals){
                addEvent(futureQueue, pastArrival);
            }
            String transitionLog = "+1";
            stateHandler(currentState, currentStateTime + 1, idleProcessors, tasksChanges, futureQueue, transitionLog);
            if(idleProcessors > 0) {
                transitionLog = "";
                for (Event pastArrival : pastArrivals) {
                    transitionLog = "A>T[" + pastArrival.getTaskId() + "]";
                    processTheArrival(pastArrival, futureQueue, currentState,tasksChanges, transitionLog);
                }
            }
        } else {   
            int nextStateTime = futureQueue.peek().getEventSetTime();
            String transitionLog = "+" + (nextStateTime - currentStateTime);
            stateHandler(currentState, nextStateTime, idleProcessors, tasksChanges, futureQueue, transitionLog);
        }
    }

    private void processtheUpdate(Event updateEvent, HashMap<Integer, ArrayList<Integer>> tasksChanges) {                    
        int taskId = updateEvent.getTaskId();
        Task task = getTaskById(taskId);
        updateTheTasksChanges(tasksChanges, taskId, 0);
        for (Link link : links) {
            if (link.getFromTask() == task.getId()) {
                Task nextTask = getTaskById(link.getToTask());
                Port inPort = link.getToPort();
                int inPortNumber = nextTask.getInPortIndex(inPort);
                updateTheTasksChanges(tasksChanges, nextTask.getId(), inPortNumber);
            }
        }
    }

   
    private void processTheArrival(Event currentArrival, PriorityQueue<EventSet> futureQueue, State currentState,
    HashMap<Integer, ArrayList<Integer>> tasksChanges, String transitionLog) {
        int idleProcessors = currentState.getIdleProcessors();
        int taskId = currentArrival.getTaskId();
        Task task = getTaskById(taskId);
        int taskPeriod = task.getPeriod();
        PriorityQueue<EventSet> finalQueue = new PriorityQueue<>();
        for(EventSet eventSet : futureQueue) {
            ArrayList<Event> copyEventSet = eventSet.getAllEvents();
            for(Event event : copyEventSet) {
                if(!event.equals(currentArrival)) {
                    addEvent(finalQueue, event);
                }
            }
        }
        Event resultedArrivalEvent = new Event();
        resultedArrivalEvent = new Event(task.getId(), nextArrival(taskPeriod, currentStateTime), Event.EventType.ARRIVAL);
        addEvent(finalQueue, resultedArrivalEvent);
        // updateTheTasksChanges(tasksChanges, taskId, -1);
        idleProcessors -= 1;
        Event resultedUpdateEvent = schedule(task, currentStateTime);
        addEvent(finalQueue, resultedUpdateEvent);
        stateHandler(currentState, currentStateTime, idleProcessors, tasksChanges, finalQueue, transitionLog);
    }

    private void updateTheTasksChanges(HashMap<Integer, ArrayList<Integer>> tasksChanges, int taskId, int value) {
        tasksChanges.compute(taskId, (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            v.add(value);
            return v;
        });
    }

    private void addEvent(PriorityQueue<EventSet> priorityQueue, Event newEvent) {
        EventSet targetEventSet = priorityQueue.stream()
                .filter(eventSet -> eventSet.getEventSetTime() == newEvent.getTime())
                .findFirst()
                .orElse(null);
        if (targetEventSet != null) {
            targetEventSet.insertEvent(newEvent);
        } else {
            targetEventSet = new EventSet(newEvent.getTime());
            targetEventSet.insertEvent(newEvent);
            priorityQueue.add(targetEventSet);
        }
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
        for (Task task : tasks) {
            if (task.getId() == taskId) {
                return task;
            }
        }
        return null;
    }

    private void 
    stateHandler(State currentState, int newStateTime, int idleProcessors, HashMap<Integer, ArrayList<Integer>> changes, PriorityQueue<EventSet> resultedQueue, String transitionLog) {
        State newState = new State(currentState, nextStateId, newStateTime, idleProcessors, changes, resultedQueue);
        State foundWrapper  = findWrapper(newState, newStateTime);
        int sourceId = currentState.getStateID();
        if (foundWrapper == null) {
            states.add(newState);
            Long newHashCode = newState.longHashCode();
            HashSet<Integer> sIDs = 
            hashTable.get(newHashCode) == null ? new HashSet<>() : hashTable.get(newHashCode);
            sIDs.add(nextStateId);
            if (!hashTable.containsKey(newHashCode)) {
                hashTable.put(newHashCode, sIDs);
            }
            String transitionMessage = nextStateId + "#" + transitionLog;
            if (!this.transitions.containsKey(sourceId)) {
                
                ArrayList<String> destination = new ArrayList<>();
                destination.add(transitionMessage);
                this.transitions.put(sourceId, destination);
            } else {
                this.transitions.get(sourceId).add(transitionMessage);
            }
            
            nextStateId += 1;
        } else {
            foundWrapper.insertNewSource(newState.getSourceIds());
            int foundWrapperId = foundWrapper.getStateID();
            String transitionMessage = foundWrapperId + "#" + transitionLog;
            if (!this.transitions.containsKey(sourceId)) {
                ArrayList<String> destination = new ArrayList<>();
                destination.add(transitionMessage);
                this.transitions.put(sourceId, destination);
            } else {
                this.transitions.get(sourceId).add(transitionMessage);
            }
        }
        // if (!iterationOfStates.containsKey(iteration)) {
        //     ArrayList<Integer> tempState = new ArrayList<>();
        //     tempState.add(nextStateId);
        //     iterationOfStates.put(iteration, tempState);
        //     } else {
        //     iterationOfStates.get(iteration).add(nextStateId);
        //     }
    
    }
    
    private State findWrapper(State newState, int newStateTime) {
        int newIdleCPUs = newState.getIdleProcessors();
        Long newHashCode = newState.longHashCode();
        HashMap<Integer, HashMap<Object, Object>> newTasksBody = newState.getTasksBody();
        PriorityQueue<EventSet> resultedQueue = newState.getEventSetQueue();
        int resultedQueueSize = resultedQueue.size();
        HashSet<Integer> previousStateIDs = hashTable.get(newHashCode);
        if (previousStateIDs != null) {
            for (int previousStateID : previousStateIDs) {
                State previousState = states.get(previousStateID);
                if (isWrapper(newIdleCPUs, resultedQueueSize, newStateTime, newTasksBody, resultedQueue, previousState)) {
                    return previousState;
                }
            }
        }
        return null;   
    }

    private boolean isWrapper(int newIdleCPUs, int newSize, int newStateTime, HashMap<Integer, HashMap<Object, Object>> newTasksBody, PriorityQueue<EventSet> newQueue, State previousState) {
        int previousSize = previousState.getEventSetQueue().size();
        if (previousSize == 0) {
            return true;
        }
        if (newIdleCPUs != previousState.getIdleProcessors()) {
            return false;
        }
        if (newSize != previousSize) {
            return false;
        }
        int previousStateTime = previousState.getStateTime();
        int diffTime = newStateTime - previousStateTime;
        if (!previousState.isTasksBodyEqual(newTasksBody, diffTime)) {
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

    private boolean compareEvents(List<Event> list1, List<Event> list2) {
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

    public EventSet getEventSetByTime(PriorityQueue<EventSet> queue, int time) {
        for (EventSet eventSet : queue) {
            if (eventSet.getEventSetTime() == time) {
                return eventSet;
            }
        }
        return null;
    }
        
    public void createLogFile() {
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
        loopCheck1(cID, sID, turningStates, new ArrayList<Integer>());
    }

    private void loopCheck1(int cID, int sID, HashSet<Integer> turningStates, ArrayList<Integer> visited) {
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
                loopCheck1(ancOfCID, sID, turningStates, visited);
            }
        }
    }
    
}