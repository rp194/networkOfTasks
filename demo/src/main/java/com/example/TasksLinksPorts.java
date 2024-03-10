package com.example;

import java.io.FileReader;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
public interface TasksLinksPorts {
  static enum Port {A, B, C, D, W, X, Y, Z};
  ArrayList<Task> taskList = new ArrayList<>();
  ArrayList<Link> linkList = new ArrayList<>();
  public static void loadTasksAndLinks() {
    try {
      JSONTokener tokener = new JSONTokener(new FileReader("E:\\Documents\\java_codes\\n" + //
                "etworkOfTasks\\demo\\src\\main\\java\\com\\example\\sample.json"));
      JSONObject jsonObject = new JSONObject(tokener);
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
        Task.Port[] inports = new Task.Port[inportsArrSize];
        Task.Port[] outports = new Task.Port[outportsArrSize];
        for (int i = 0; i < inportsArrSize; i++) {
          String portString = (String) inportsArr.get(i);
          inports[i] = Task.Port.valueOf(portString);
        }
        for (int i = 0; i < outportsArrSize; i++) {
          String portString = (String) outportsArr.get(i);
          outports[i] = Task.Port.valueOf(portString);
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
        Link.Port fromPort = Link.Port.valueOf((String) link.get("fromPort"));
        int toTask = (int) link.get("toTask");
        Link.Port toPort = Link.Port.valueOf((String) link.get("toPort"));
        Link newLink = new Link((int) fromTask, fromPort, (int) toTask, toPort);
        linkList.add(newLink);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
