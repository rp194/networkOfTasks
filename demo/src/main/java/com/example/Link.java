package com.example;

public class Link implements TasksLinksPorts {
  private int fromTask;
  private int toTask;
  // public enum Port {A, B, C, D, W, X, Y,Z};
  private Port fromPort;
  private Port toPort;

  public Link(int fromTask, Port fromPort, int toTask, Port toPort) {
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


  public Port getFromPort() {
    return this.fromPort;
  }


  public Port getToPort() {
    return this.toPort;
  }

  public int getToTask() {
    return this.toTask;
  }

  
}
