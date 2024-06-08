package com.example;

import java.util.Arrays;

public class Task implements InitializingConfigs {
    private int id;
    private String name;
    private int duration;
    private int deadline;
    private int period;
    private int[] inPorts;
    private int[] outPorts;

    public Task(int id, String name, int duration, int deadline, int period, int[] inPorts, int[] outPorts) {
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

    public int[] getInPorts() {
        return Arrays.copyOf(inPorts, inPorts.length);
    }

    public int[] getOutPorts() {
        return Arrays.copyOf(outPorts, outPorts.length);
    }

    public int getInPortIndex(int inPort) {
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