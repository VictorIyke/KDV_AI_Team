package com.example.ortdemo;

import com.bumptech.glide.load.model.stream.MediaStoreImageThumbLoader;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.IOException;

public class BitmapModule extends ReactContextBaseJavaModule {

    public BitmapModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "Bitmap";
    }

    @ReactMethod
    public void getPixels(String filePath, final Promise promise) {
        try {
            WritableNativeMap result = new WritableNativeMap();
            WritableNativeArray pixels = new WritableNativeArray();
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getCurrentActivity().getContentResolver(), Uri.parse(filePath));
            if (bitmap == null) {
                promise.reject("Failed to decode. Path is incorrect or image is corrupted");
                return;
            }

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            boolean hasAlpha = bitmap.hasAlpha();

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int color = bitmap.getPixel(x, y);
                    pixels.pushInt(color);
                }
            }

            result.putInt("width", width);
            result.putInt("height", height);
            result.putBoolean("hasAlpha", hasAlpha);
            result.putArray("pixels", pixels);

            promise.resolve(result);

        } catch (Exception e) {
            promise.reject(e);
        }

    }

}
