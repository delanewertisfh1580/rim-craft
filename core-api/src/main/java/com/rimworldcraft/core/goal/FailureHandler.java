package com.rimworldcraft.core.goal;
public interface FailureHandler { Decision onFailure(String reason,int attempts); enum Decision{RETRY,REPLAN,FAIL_TASK,IDLE} }
