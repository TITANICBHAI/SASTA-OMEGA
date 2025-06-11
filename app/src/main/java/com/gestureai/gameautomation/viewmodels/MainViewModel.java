package com.gestureai.gameautomation.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    
    private final MutableLiveData<Boolean> permissionsReady = new MutableLiveData<>(false);
    private final MutableLiveData<GestureEngineState> gestureEngineState = new MutableLiveData<>(new GestureEngineState());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    
    public static class GestureEngineState {
        public boolean isDetecting = false;
        public boolean isConnected = false;
        public String status = "Ready";
        
        public GestureEngineState() {}
        
        public GestureEngineState(boolean isDetecting, boolean isConnected, String status) {
            this.isDetecting = isDetecting;
            this.isConnected = isConnected;
            this.status = status;
        }
    }
    
    public LiveData<Boolean> getPermissionsReady() {
        return permissionsReady;
    }
    
    public void setPermissionsReady(boolean ready) {
        permissionsReady.setValue(ready);
    }
    
    public LiveData<GestureEngineState> getGestureEngineState() {
        return gestureEngineState;
    }
    
    public void updateGestureEngineState(boolean isDetecting, boolean isConnected, String status) {
        gestureEngineState.setValue(new GestureEngineState(isDetecting, isConnected, status));
    }
    
    public LiveData<String> getToastMessage() {
        return toastMessage;
    }
    
    public void showToast(String message) {
        toastMessage.setValue(message);
    }
    
    public void clearToast() {
        toastMessage.setValue(null);
    }
}