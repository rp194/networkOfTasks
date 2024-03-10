package com.example;

public class Main {
    public static void main(String[] args) throws Exception {
            SystemModel systemModel = new SystemModel(1);
            systemModel.stateSpaceGenerator(80);
            systemModel.createLogFile();
            systemModel.createDotFile();
        }
}
