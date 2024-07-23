package com.example;

import java.util.HashSet;
import java.util.HashSet;
public class EventSet implements Comparable<EventSet> {
  private int eventSetTime;
  private HashSet<Event> updates = new HashSet<>();
  private HashSet<Event> arrivals = new HashSet<>();
  private HashSet<Event> allEvents = new HashSet<>();  

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

  public void setArrivals(HashSet<Event> arrivalEvents) {
    this.arrivals = arrivalEvents;
  }

  public void setUpdates(HashSet<Event> updateEvents) {
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

  public HashSet<Event> getAllEvents(){
    return allEvents;
  }

  public HashSet<Event> getUpdates() {
    return updates;
  }

  public HashSet<Event> getArrivals() {
    return arrivals;
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