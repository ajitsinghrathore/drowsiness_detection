package com.asr.facedrowsinessservice;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;


public class Face_detector {

    private static  Face_detector instance;
    private final ImageAnalysis imageAnalysis;
    private final face_detection_lifecycleOwner lifecycleOwner ;
    private FaceAnalysis faceAnalysis;
    private boolean is_running ;
    private boolean was_wink ;



    private Face_detector(){
        imageAnalysis =  new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        lifecycleOwner = new face_detection_lifecycleOwner();
        is_running= false;

    }
    public static  Face_detector getInstance(Context context){
        if(instance == null){
            synchronized (Face_detector.class) {
                if (instance == null) {
                    if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                        return null;
                    }
                    instance = new Face_detector();
                }
            }
        }
        return  instance;
    }


    public Flowable<Boolean> start_face_detection(@NonNull Context context,int deviation_angle , float confidence_level){

        if(is_running){
            return null;
        }
        if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            return null;
        }
        is_running = true;

        ListenableFuture<ProcessCameraProvider> CameraProviderFuture =  ProcessCameraProvider.getInstance(context);
        CameraProviderFuture.addListener(() -> {
            try {
                setup_camera(CameraProviderFuture);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        },ContextCompat.getMainExecutor(context));
        faceAnalysis = new FaceAnalysis(deviation_angle,confidence_level);
        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(),faceAnalysis);


        return faceAnalysis.getFace_updates()
                .distinctUntilChanged()
                .debounce(aBoolean -> {
                    was_wink = !aBoolean;
                    return aBoolean? Observable.empty():Observable.timer(500,TimeUnit.MILLISECONDS);
                })
                .distinctUntilChanged()
                .map(aBoolean -> {

                    if(!aBoolean){
                        if(was_wink){
                            Log.d("AITAG","onCreate: sleeping detected");
                            return false;
                        }
                    }

                    Log.d("AITAG","onCreate: waked up");
                    return true;



                }).toFlowable(BackpressureStrategy.DROP);

    }

    private void setup_camera(ListenableFuture<ProcessCameraProvider> CameraProviderFuture) throws ExecutionException, InterruptedException {
        ProcessCameraProvider  cameraProvider =  CameraProviderFuture.get();
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(lifecycleOwner,CameraSelector.DEFAULT_FRONT_CAMERA,imageAnalysis);
        lifecycleOwner.trigger_start();
    }

    public void stop_face_detection(){
        lifecycleOwner.trigger_destroy();
        faceAnalysis.close();
        faceAnalysis = null;
        is_running = false;
    }













}
