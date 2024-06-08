package com.example;

public class Event implements Ports, Comparable<Event> {
  private int taskId;
  private int eventTime;
  public static enum EventType {ARRIVAL, UPDATE};
  private EventType eventType;
  public Event(int taskId, int eventTime, EventType eventType) {
    this.taskId = taskId;
    this.eventTime = eventTime;
    this.eventType = eventType;
  }

  public Event(Event source) {
    this.taskId = source.getTaskId();
    this.eventTime = source.getTime();
    this.eventType = source.getType();

  }

  public Event() {

  }

  public int getTaskId() {
    return this.taskId; 
  }

  public int getTime() {
    return this.eventTime;
  }

  public EventType getType() {
    return this.eventType;
  }

  public void setTime(int i) {
    this.eventTime = i;
  }
  
  @Override
  public int compareTo(Event specifiedObject) {
    int timeComparison = Integer.compare(this.eventTime, specifiedObject.getTime());
    if (timeComparison != 0) {
      return timeComparison;
    } else {
      if (this.eventType == EventType.UPDATE || this.eventType == EventType.ARRIVAL) {
        return -1;
      } else if (specifiedObject.getType() == EventType.UPDATE  || specifiedObject.getType() == EventType.ARRIVAL) {
        return 1;
      } else {
        return 0;
      }
    }
  }
  
}