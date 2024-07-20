package com.example;

import java.util.TreeSet;
public class EventSet implements Comparable<EventSet> {
  private int eventSetTime;
  private TreeSet<Event> updates = new TreeSet<>();
  private TreeSet<Event> arrivals = new TreeSet<>();
  private TreeSet<Event> starts = new TreeSet<>();
  private TreeSet<Event> allEvents = new TreeSet<>();  

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
    }
  }

  public void setArrivals(TreeSet<Event> arrivalEvents) {
    this.arrivals = arrivalEvents;
  }

  public void setUpdates(TreeSet<Event> updateEvents) {
    this.updates = updateEvents;
  }
  
  public void remove(Event currentEvent) {
    allEvents.remove(currentEvent);
    switch (currentEvent.getType()) {
      case UPDATE:
        updates.remove(currentEvent);
        break;
      case ARRIVAL:
        arrivals.remove(currentEvent);
    }

  }
  
  public int getEventSetTime() {
    return eventSetTime;
  }

  public TreeSet<Event> getAllEvents(){
    return allEvents;
  }

  public TreeSet<Event> getUpdates() {
    return updates;
  }

  public TreeSet<Event> getArrivals() {
    return arrivals;
  }

  public TreeSet<Event> getStarts() {
    return starts;
  }

  public int getEventSetSize() {
    return allEvents.size();
  }

  @Override
  public int hashCode() {
    return eventSetTime;
  }

  @Override
  public boolean equals(Object obj) {
      if (this == obj) {
          return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
          return false;
      }
      EventSet other = (EventSet) obj;
      return eventSetTime == other.eventSetTime;
  }

  @Override
  public int compareTo(EventSet specifiedObject) {
      int timeComparison = Integer.compare(this.eventSetTime, specifiedObject.getEventSetTime());
      return timeComparison;
  }

}