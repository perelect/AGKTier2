package com.thegamecreators.agk_player;

import android.Manifest;
import android.app.Activity;
import android.app.NativeActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;

public class AGKActivity extends NativeActivity
{
    @Override
    public void onCreate( Bundle state )
    {
        super.onCreate( state );

        Intent intent = getIntent();
        if ( intent != null )
        {
            Uri data = intent.getData();
            if ( data != null ) AGKHelper.g_sLastURI = data.toString();
        }
    }

    @Override
    public void onNewIntent( Intent intent )
    {
        if ( intent == null ) return;
        if ( intent.getData() == null ) return;
        AGKHelper.g_sLastURI = intent.getData().toString();
    }

    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        switch(requestCode)
        {
            case 10002: {
                if (resultCode != Activity.RESULT_OK) {
                    Log.i("MediaProjection", "User cancelled");
                    return;
                }
                if (Build.VERSION.SDK_INT >= 21) {
                    Log.i("MediaProjection", "Starting screen capture");

                    int width = AGKHelper.g_pAct.getWindow().getDecorView().getWidth();
                    int height = AGKHelper.g_pAct.getWindow().getDecorView().getHeight();
                    if (width > height) {
                        if (width > 1280) width = 1280;
                        if (height > 720) height = 720;
                    } else {
                        if (width > 720) width = 720;
                        if (height > 1280) height = 1280;
                    }
                    int audioSource = 0;
                    if (AGKHelper.g_iScreenRecordMic == 1) {
                        audioSource = MediaRecorder.AudioSource.MIC;
                    }

                    AGKHelper.mMediaRecorder = new MediaRecorder();
                    if (audioSource > 0) AGKHelper.mMediaRecorder.setAudioSource(audioSource);
                    AGKHelper.mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                    AGKHelper.mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    AGKHelper.mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                    if (audioSource > 0) {
                        AGKHelper.mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                        AGKHelper.mMediaRecorder.setAudioEncodingBitRate(96000);
                        AGKHelper.mMediaRecorder.setAudioSamplingRate(44100);
                    }
                    AGKHelper.mMediaRecorder.setVideoEncodingBitRate(2048 * 1000);
                    AGKHelper.mMediaRecorder.setVideoFrameRate(30);
                    AGKHelper.mMediaRecorder.setVideoSize(width, height);
                    AGKHelper.mMediaRecorder.setOutputFile(AGKHelper.g_sScreenRecordFile);
                    try {
                        AGKHelper.mMediaRecorder.prepare();
                    } catch (Exception e) {
                        e.printStackTrace();
                        AGKHelper.mMediaRecorder.release();
                        AGKHelper.mMediaRecorder = null;
                        return;
                    }

                    DisplayMetrics metrics = AGKHelper.g_pAct.getResources().getDisplayMetrics();
                    AGKHelper.mMediaProjection = AGKHelper.mMediaProjectionManager.getMediaProjection(resultCode, data);
                    Log.i("MediaProjection", "Setting up a VirtualDisplay: " + width + "x" + height + " (" + metrics.densityDpi + ")");
                    AGKHelper.mVirtualDisplay = AGKHelper.mMediaProjection.createVirtualDisplay("ScreenCapture",
                            width, height, metrics.densityDpi,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                            AGKHelper.mMediaRecorder.getSurface(), null, null);
                    AGKHelper.mMediaRecorder.start();
                }
            }
            break;
            case 9004: // capture camera image
            {
                if ( resultCode == RESULT_OK )
                {
                    if (data != null && data.getExtras() != null) {
                        Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                        try {
                            FileOutputStream out = new FileOutputStream(AGKHelper.sCameraSavePath);
                            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
                            Log.w("Camera Image", "Saved image to: " + AGKHelper.sCameraSavePath);
                            AGKHelper.iCapturingImage = 2;
                            return;
                        }
                        catch( IOException e )
                        {
                            Log.e("Camera Image", "Failed to save image: "+e.toString() );
                        }
                    }
                    AGKHelper.iCapturingImage = 0;
                }
                else
                {
                    Log.e("Camera Image", "User cancelled capture image" );
                    AGKHelper.iCapturingImage = 0;
                }
                break;
            }
            case 9005: // choose image
            {
                if ( resultCode == RESULT_OK )
                {
                    if (data != null) {
                        Uri uri = data.getData();
                        try {
                            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            FileOutputStream out = new FileOutputStream(AGKHelper.sChosenImagePath);
                            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
                            Log.w("Choose Image", "Saved image to: " + AGKHelper.sChosenImagePath);
                            AGKHelper.iChoosingImage = 2;
                            return;
                        }
                        catch( IOException e )
                        {
                            Log.e("Choose Image", "Failed to save image: "+e.toString() );
                        }
                    }
                    AGKHelper.iChoosingImage = 0;
                }
                else
                {
                    Log.e("Choose Image", "User cancelled choose image" );
                    AGKHelper.iChoosingImage = 0;
                }
                break;
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        int index = requestCode-5;
        if ( index < 0 ) return;
        if ( index >= AGKHelper.g_sPermissions.length ) return;

        AGKHelper.g_iPermissionStatus[index] = 2;
    }
}