package com.gestureai.gameautomation.managers;

import android.util.Log;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.fragments.*;
import java.util.HashMap;
import java.util.Map;

public class FragmentNavigationManager {
    private static final String TAG = "FragmentNavigationManager";
    
    private FragmentActivity activity;
    private FragmentManager fragmentManager;
    private Map<String, Fragment> fragmentCache;
    private Fragment currentFragment;
    
    public FragmentNavigationManager(FragmentActivity activity) {
        this.activity = activity;
        this.fragmentManager = activity.getSupportFragmentManager();
        this.fragmentCache = new HashMap<>();
        initializeFragments();
    }
    
    private void initializeFragments() {
        try {
            // Core fragments
            fragmentCache.put("gesture_controller", new GestureControllerFragment());
            fragmentCache.put("auto_play", new AutoPlayFragment());
            fragmentCache.put("screen_monitor", new ScreenMonitorFragment());
            fragmentCache.put("analytics", new AnalyticsFragment());
            fragmentCache.put("training", new TrainingFragment());
            fragmentCache.put("settings", new SettingsFragment());
            fragmentCache.put("debug", new DebugFragment());
            fragmentCache.put("more", new MoreFragment());
            
            Log.d(TAG, "Fragment cache initialized with " + fragmentCache.size() + " fragments");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing fragments", e);
        }
    }
    
    public boolean navigateToFragment(String fragmentKey) {
        try {
            Fragment fragment = fragmentCache.get(fragmentKey);
            if (fragment == null) {
                Log.e(TAG, "Fragment not found: " + fragmentKey);
                return false;
            }
            
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            
            // Hide current fragment if exists
            if (currentFragment != null) {
                transaction.hide(currentFragment);
            }
            
            // Show target fragment
            if (fragment.isAdded()) {
                transaction.show(fragment);
            } else {
                transaction.add(R.id.fragment_container, fragment, fragmentKey);
            }
            
            transaction.commitAllowingStateLoss();
            currentFragment = fragment;
            
            Log.d(TAG, "Navigated to fragment: " + fragmentKey);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to fragment: " + fragmentKey, e);
            return false;
        }
    }
    
    public Fragment getCurrentFragment() {
        return currentFragment;
    }
    
    public void setFragmentConnectionState(String fragmentKey, boolean isConnected) {
        Fragment fragment = fragmentCache.get(fragmentKey);
        if (fragment instanceof ServiceConnectionAware) {
            ((ServiceConnectionAware) fragment).onServiceConnectionChanged(isConnected);
        }
    }
    
    public interface ServiceConnectionAware {
        void onServiceConnectionChanged(boolean isConnected);
    }
    
    public void onServiceStateChanged(String serviceName, boolean isConnected) {
        // Notify all fragments about service state changes
        for (Fragment fragment : fragmentCache.values()) {
            if (fragment instanceof ServiceConnectionAware) {
                ((ServiceConnectionAware) fragment).onServiceConnectionChanged(isConnected);
            }
        }
    }
    
    public void refreshAllFragments() {
        try {
            for (Map.Entry<String, Fragment> entry : fragmentCache.entrySet()) {
                Fragment fragment = entry.getValue();
                if (fragment.isAdded() && fragment.isVisible()) {
                    // Refresh fragment data
                    if (fragment instanceof DataRefreshable) {
                        ((DataRefreshable) fragment).refreshData();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing fragments", e);
        }
    }
    
    public interface DataRefreshable {
        void refreshData();
    }
}