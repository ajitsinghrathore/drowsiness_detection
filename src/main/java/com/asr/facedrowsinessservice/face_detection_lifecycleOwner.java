package com.asr.facedrowsinessservice;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

public class face_detection_lifecycleOwner implements LifecycleOwner {

    private final LifecycleRegistry lifecycleRegistry;

    protected face_detection_lifecycleOwner(){
        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
    }

    public void trigger_start(){
        lifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
    }

    public void trigger_destroy(){
        lifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
    }


    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }


}
