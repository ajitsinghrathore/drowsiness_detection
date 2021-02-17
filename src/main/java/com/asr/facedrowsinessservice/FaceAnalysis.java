package com.asr.facedrowsinessservice;

import android.annotation.SuppressLint;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.Objects;

import io.reactivex.rxjava3.subjects.PublishSubject;

import static java.lang.Math.abs;


public class FaceAnalysis  implements ImageAnalysis.Analyzer {


    private final FaceDetectorOptions options = new FaceDetectorOptions.Builder()
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .build();

    private final FaceDetector faceDetector = FaceDetection.getClient(options);


    private final PublishSubject<Boolean> face_updates = PublishSubject.create();

    private final int deviation_angle;
    private final float confidence_level;

    public FaceAnalysis(int deviation_angle,float confidence_level){
        this.confidence_level = confidence_level;
        this.deviation_angle = deviation_angle;
    }




    @Override
    @SuppressLint("UnsafeExperimentalUsageError")
    public void analyze(@NonNull ImageProxy image) {

        Image cameraImage = image.getImage();
        if(cameraImage != null){
            InputImage inputImage =  InputImage.fromMediaImage(cameraImage,image.getImageInfo().getRotationDegrees());
            faceDetector.process(inputImage)
                    .addOnSuccessListener(faces -> process_faces(faces,image)).
                    addOnFailureListener(e -> handleError(e,image));
        }

    }


    private void handleError(Exception e,ImageProxy proxy) {
        Log.d("AITAG","handleError: "+e.getMessage());
        proxy.close();
    }

    private void process_faces(List<Face> faces,ImageProxy proxy) {
        if(faces != null && faces.size()>0){
            for(Face face : faces){
                handle_single_face(face);
            }
        }else{
            face_updates.onNext(false);
        }
        proxy.close();

    }

    private void handle_single_face(Face face) {
        float l = Objects.requireNonNull(face.getLeftEyeOpenProbability());
        float r = Objects.requireNonNull(face.getRightEyeOpenProbability());
        float deviation = abs(face.getHeadEulerAngleY());
        face_updates.onNext(l > confidence_level && r > confidence_level && deviation<=deviation_angle);


    }


    public PublishSubject<Boolean> getFace_updates() {
       return  face_updates;
    }

    public void close(){
        faceDetector.close();
    }








}
