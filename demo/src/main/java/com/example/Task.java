package com.example;

import java.util.Arrays;

public class Task implements TasksLinksPorts {
    private int id;
    private String name;
    private int duration;
    private int deadline;
    private int period;
    private Port[] inPorts;
    private Port[] outPorts;

    public Task(int id, String name, int duration, int deadline, int period, Port[] inPorts, Port[] outPorts) {
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.deadline = deadline;
        this.period = period;
        this.inPorts = Arrays.copyOf(inPorts, inPorts.length);
        this.outPorts = Arrays.copyOf(outPorts, outPorts.length);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDuration() {
        return duration;
    }

    public int getDeadline() {
        return deadline;
    }

    public int getPeriod() {
        return period;
    }

    public Port[] getInPorts() {
        return Arrays.copyOf(inPorts, inPorts.length);
    }

    public Port[] getOutPorts() {
        return Arrays.copyOf(outPorts, outPorts.length);
    }

    public int getInPortIndex(Port inPort) {
        for (int i=0; i<inPorts.length; i++) {
            if (this.inPorts[i] == inPort) {
                return i+1;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
      return "{" +
        " id='" + getId() + "'" +
        ", name='" + getName() + "'" +
        ", duration='" + getDuration() + "'" +
        ", deadline='" + getDeadline() + "'" +
        ", period='" + getPeriod() + "'" +
        ", inPorts='" + getInPorts() + "'" +
        ", outPorts='" + getOutPorts() + "'" +
        "}";
    }

}