package com.example;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
public interface InitializingConfigs {
  ArrayList<Task> taskList = new ArrayList<>();
  ArrayList<Link> linkList = new ArrayList<>();
  ArrayList<Integer> eventSensors = new ArrayList<>();
  ArrayList<Integer> statusSensors = new ArrayList<>();
  ArrayList<Integer> actuators = new ArrayList<>();
  HashMap<Integer, Integer> timingConstraints = new HashMap<>(3);
  public static void loadTasksAndLinks() {
    try {
      JSONTokener taskSetTokener = 
      new JSONTokener(new FileReader
      ("E:\\Documents\\java_codes\\networkOfTasks\\demo\\src\\main\\java\\com\\example\\sample.json"));
      JSONObject jsonObject = new JSONObject(taskSetTokener);
      JSONArray tasks = (JSONArray) jsonObject.get("tasks");
      for (Object taskObj : tasks) {
        JSONObject task = (JSONObject) taskObj;
        int id = (int) task.get("id");
        String name = (String) task.get("name");
        int duration = (int) task.get("duration");
        int deadline = (int) task.get("deadline");
        int period = (int) task.get("period");
        JSONArray inportsArr = (JSONArray) task.get("inports");
        JSONArray outportsArr = (JSONArray) task.get("outports");
        int inportsArrSize = inportsArr.length();
        int outportsArrSize = outportsArr.length();
        int[] inports = new int[inportsArrSize];
        int[] outports = new int[outportsArrSize];
        for (int i = 0; i < inportsArrSize; i++) {
          inports[i] = (int) inportsArr.get(i);
        }
        for (int i = 0; i < outportsArrSize; i++) {
          outports[i] = (int) outportsArr.get(i);
        }             
        Task newTask = new Task((int) id, name, (int) duration, (int) deadline, (int) period,inports,outports);
        taskList.add(newTask);
      }
      if (!jsonObject.has("links")) {
        return;
      }
      Object linksObject = jsonObject.get("links");
      JSONArray links = (JSONArray) linksObject;
      for (Object linkObj : links) {
        JSONObject link = (JSONObject) linkObj;
        int fromTask = (int) link.get("fromTask");
        int fromPort = (int) link.get("fromPort");
        int toTask = (int) link.get("toTask");
        int toPort = (int) link.get("toPort");
        Link newLink = new Link(fromTask, fromPort, toTask, toPort);
        linkList.add(newLink);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public static void loadConstraints() {
    try {
      JSONTokener constraintsTokener = new JSONTokener(new FileReader("E:\\Documents\\java_codes\\n" +
                "etworkOfTasks\\demo\\src\\main\\java\\com\\example\\constraints.json"));
      JSONObject jsonObject = new JSONObject(constraintsTokener);
      JSONArray timings = (JSONArray) jsonObject.get("timing");
      for (Object constraintObj : timings) {
        JSONObject constraint = (JSONObject) constraintObj;
        int key = (int) constraint.get("number");
        int value = (int) constraint.get("value");
        timingConstraints.put(key, value);
        JSONArray eventSensorArray = (JSONArray) jsonObject.get("eventSensors");
        for (int i = 0; i < eventSensorArray.length(); i++) {
          eventSensors.add((int) eventSensorArray.get(i));
        }
      }
      JSONArray statusSensorArray = (JSONArray) jsonObject.get("statusSensors");
      for (int i = 0; i < statusSensorArray.length(); i++) {
        statusSensors.add((int) statusSensorArray.get(i));
      }
      JSONArray actuatorArray = (JSONArray) jsonObject.get("actuators");
      for (int i = 0; i < actuatorArray.length(); i++) {
        actuators.add((int) actuatorArray.get(i));
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
