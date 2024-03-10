package com.example;

import java.util.ArrayList;
public class EventSet implements Comparable<EventSet> {
  private int eventSetTime;
  private ArrayList<Event> updates = new ArrayList<>();
  private ArrayList<Event> arrivals = new ArrayList<>();
  private ArrayList<Event> starts = new ArrayList<>();
  private ArrayList<Event> allEvents = new ArrayList<>();  

  public EventSet(int time) {
    this.eventSetTime = time;
  }

  public void insertEvent(Event event) {
    allEvents.add(event);
    switch (event.getType()) {
      case UPDATE:
        updates.add(event);
        break;
      case ARRIVAL:
        arrivals.add(event);
        break;
      case START:
        starts.add(event);
    }
  }
  
  public void remove(Event currentEvent) {
    allEvents.remove(currentEvent);
    switch (currentEvent.getType()) {
      case UPDATE:
        updates.remove(currentEvent);
        break;
      case ARRIVAL:
        arrivals.remove(currentEvent);
        break;
      case START:
        starts.remove(currentEvent);
    }

  }
  
  public int getEventSetTime() {
    return eventSetTime;
  }

  public ArrayList<Event> getAllEvents(){
    return allEvents;
  }


  public ArrayList<Event> getUpdates() {
    return updates;
  }

  public ArrayList<Event> getArrivals() {
    return arrivals;
  }

  public ArrayList<Event> getStarts() {
    return starts;
  }

  public int getEventSetSize() {
    return allEvents.size();
  }


  @Override
  public int compareTo(EventSet specifiedObject) {
      int timeComparison = Integer.compare(this.eventSetTime, specifiedObject.getEventSetTime());
      return timeComparison;
  }




}