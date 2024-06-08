package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest implements Ports
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
        
    }


      @Test
  public void testBooleans() {
    boolean cc = true;
    assertEquals(false, 0 != 0? 1 == 1? true: false : false);
   }

   @Test
       public void testCopyQueue() {
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
  
  // @Before
  // public void setup() throws Exception {
  //   try {
  //       JSONParser parser = new JSONParser();
  //       Object obj = parser.parse(new FileReader("E:\\Documents\\java_codes\\tasksets\\TasksAndLinks\\src\\sample.json"));
  //       JSONObject jsonObject = (JSONObject) obj;
  //       JSONArray tasks = (JSONArray) jsonObject.get("tasks");
  
  //       for (Object taskObj : tasks) {
  //         JSONObject task = (JSONObject) taskObj;
  //         long id = (long) task.get("id");
  //         String name = (String) task.get("name");
  //         long duration = (long) task.get("duration");
  //         long deadline = (long) task.get("deadline");
  //         long period = (long) task.get("period");
  //         JSONArray inportsArr = (JSONArray) task.get("inports");
  //         JSONArray outportsArr = (JSONArray) task.get("outports");
  //         Task.Port[] inports = new Task.Port[inportsArr.size()];
  //         Task.Port[] outports = new Task.Port[outportsArr.size()];

  //         for (int i = 0; i < inportsArr.size(); i++) {
  //           String portString = (String) inportsArr.get(i);
  //           inports[i] = Task.Port.valueOf(portString);
  //         }

  //         for (int i = 0; i < outportsArr.size(); i++) {
  //           String portString = (String) outportsArr.get(i);
  //           outports[i] = Task.Port.valueOf(portString);
  //         }             
  //         Task newTask = new Task((int) id, name, (int) duration, (int) deadline, (int) period,inports,outports);
  
  //         taskList.add(newTask);
  //       }
  
  //       JSONArray links = (JSONArray) jsonObject.get("links");
  
  //       for (Object linkObj : links) {
  //         JSONObject link = (JSONObject) linkObj;
  //         long fromTask = (long) link.get("fromTask");
  //         Link.Port fromPort = Link.Port.valueOf((String) link.get("fromPort"));
  //         long toTask = (long) link.get("toTask");
  //         Link.Port toPort = Link.Port.valueOf((String) link.get("toPort"));
  //         Link newLink = new Link((int) fromTask, fromPort, (int) toTask, toPort);
  //         linkList.add(newLink);
  //       }
  //     } catch(Exception e) {
  //       System.out.println(e.getMessage());
  //   }
  // }



  // @Test
  // public void testNextArrival(){
  //   assertEquals(12, systemModel.nextArrival(12, 1));
  // }

  

  // @Test
  // public void testPorts() {
  //   SystemModel systemModel = new SystemModel(taskList, linkList);
  //   Link link = new Link(linkList.get(1));
  //   Task task = systemModel.getTaskById(link.getFromTask());
  //   Port outPort = link.getFromPort();
  //   Port inPort = link.getToPort();
  //   Task nextTask = systemModel.getTaskById(link.getToTask());
  //   nextTask.receive(inPort, task.send(outPort, 3));
  //   Task checkTask = new Task(nextTask);
  //   checkTask.receive(inPort, task.send(outPort, 5));
  //   assertEquals(3, nextTask.getUpdateTime(Port.A));
  //   assertEquals(5, checkTask.getUpdateTime(Port.A));
  // }

  @Test
  public void testKeySet(){

    HashMap<Port, Integer> h1 = new HashMap<>();
    h1.put(Port.A, (Integer) 4);
    Iterator<Map.Entry<Port, Integer>> it = h1.entrySet().iterator();
    // it.next();
    assertEquals(Port.A, it.next().getKey());

  }

  @Test
  public void testIntegerCompare() {
    int a = 10;
    int b = 10;
    assertEquals(0,Integer.compare(a, b));
  }

  // @Test
  // public void testHashCode() {
  //   systemModel = new SystemModel(taskList, linkList);
  //   Link link = new Link(linkList.get(1));
  //   Task task = systemModel.getTaskById(link.getFromTask());
  //   Port outPort = link.getFromPort();
  //   Port inPort = link.getToPort();
  //   Task nextTask = systemModel.getTaskById(link.getToTask());
  //   nextTask.receive(inPort, task.send(outPort, 3));
  //   State state1 = new State(0, systemModel.getTasks());
  //   State state2 = new State(1, systemModel.getTasks());
  //   assertTrue(state1.equals(state2));
  //   assertEquals(state1.hashCode(), state2.hashCode());
  // }


  // @Test
  // public void tsetSerialized() {

  //   systemModel = new SystemModel(taskList, linkList);
  //   systemModel.stateHandler(taskList);
  //   State state1 = systemModel.getsStateById(0);
  //   taskList.get(0).end(1);
  //   State copy = null;
  //   try {
  //       copy = state1.copy();
  //   } catch (IOException e) {
  //       e.printStackTrace();
  //   } catch (ClassNotFoundException e) {
  //       e.printStackTrace();
  //   } 
  //   System.out.println(state1.toString());
  //   System.out.println(copy.toString());
  //   assertNotEquals(null, state1);
  //   assertNotEquals(null, copy);

  // }

  // @Test
  // public void testDeepSerialize() {
  //   systemModel = new SystemModel(taskList, linkList);
  //   // systemModel.stateHandler(taskList);
  //   systemModel.simulate(90);
  //   // systemModel.get
  // }
  
  @Test
  public void testNullMap() {
    HashMap <Integer, Integer> tt = new HashMap<>();
    tt.put(1, 2);
    assertEquals(null, tt.get(2));
  }

  @Test
  public void testSet() {
    Set<Integer> aa = new HashSet<>();
    aa.add(1);
    aa.add(2);
    aa.add(3);
    aa.add(4);
    aa.forEach(a -> System.out.println(a));
    // for(int a: aa) {
    //   aa.remove(a);
    //   System.out.println(a);
    //   aa.add(a);
    // }
  }

  @Test
  public void testMoode() {
    assertEquals(3, 13%10);
  }

  @Test
  public void testComputeFunction() throws Exception  {
    HashMap<Integer, ArrayList<Integer>> tasksChanges = new HashMap<>();
    tasksChanges.compute(3, (k, v) -> {
      if (v == null) {
          v = new ArrayList<>();
      }
      v.add(0);
      return v;
    });
    assertTrue(tasksChanges.containsKey(3));
    assertTrue(tasksChanges.get(3).contains(0));

  }

}
