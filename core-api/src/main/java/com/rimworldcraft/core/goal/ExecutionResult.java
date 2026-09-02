package com.rimworldcraft.core.goal;
public record ExecutionResult(Status status,String reason){public enum Status{STARTED,COMPLETED,FAILED,TIMED_OUT,CANCELLED}public static ExecutionResult failed(String reason){return new ExecutionResult(Status.FAILED,reason);}}
