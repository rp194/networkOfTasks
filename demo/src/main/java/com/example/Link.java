package com.example;

public class Link implements InitializingConfigs {
  private int fromTask;
  private int toTask;
  private int fromPort;
  private int toPort;

  public Link(int fromTask, int fromPort, int toTask, int toPort) {
    this.fromTask = fromTask;
    this.fromPort = fromPort;
    this.toTask = toTask;
    this.toPort = toPort;
  }


  public Link(Link source) {
    this.fromTask = source.getFromTask();
    this.fromPort = source.getFromPort();
    this.toTask = source.getToTask();
    this.toPort = source.getToPort();
  }


  @Override
  public String toString() {
    return "{" +
      " fromTask='" + fromTask + "'" +
      ", fromPort='" + fromPort + "'" +
      ", toTask='" + toTask + "'" +
      ", toPort='" + toPort + "'" +
      "}";
  }


  public int getFromTask() {
    return this.fromTask;
  }


  public int getFromPort() {
    return this.fromPort;
  }


  public int getToPort() {
    return this.toPort;
  }

  public int getToTask() {
    return this.toTask;
  }

  
}
