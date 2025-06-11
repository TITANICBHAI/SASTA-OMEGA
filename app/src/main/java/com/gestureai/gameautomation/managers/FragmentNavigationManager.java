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
            // Core fragments - using index-based keys for MainActivity compatibility
            fragmentCache.put("0", new DashboardFragment());
            fragmentCache.put("1", new AutoPlayFragment());
            fragmentCache.put("2", new AnalyticsFragment());
            fragmentCache.put("3", new TrainingFragment());
            fragmentCache.put("4", new SettingsFragment());
            fragmentCache.put("5", new DebugFragment());
            fragmentCache.put("6", new MoreFragment());
            
            // Named access for convenience
            fragmentCache.put("dashboard", fragmentCache.get("0"));
            fragmentCache.put("auto_play", fragmentCache.get("1"));
            fragmentCache.put("analytics", fragmentCache.get("2"));
            fragmentCache.put("training", fragmentCache.get("3"));
            fragmentCache.put("settings", fragmentCache.get("4"));
            fragmentCache.put("debug", fragmentCache.get("5"));
            fragmentCache.put("more", fragmentCache.get("6"));
            
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