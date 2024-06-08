package com.example;

public class Main {
    public static void main(String[] args) throws Exception {

        SystemModel systemModel = new SystemModel(2);
        systemModel.stateSpaceGenerator(100);
        systemModel.createLogFile();
        systemModel.createDotFile();
        // ObjectPool<TaskBody> tBodyPool = new ObjectPool<>(10);
        // tBodyPool.
        }
}
