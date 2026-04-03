package com.labeliq.app.presentation.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * MainViewModel (Java) — exposes navigation LiveData for MainActivity.
 * Demonstrates Kotlin/Java interop within the same source set.
 */
public class MainViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _navigateToScan = new MutableLiveData<>(false);

    /** Observed by MainActivity to trigger navigation. */
    public MutableLiveData<Boolean> getNavigateToScan() {
        return _navigateToScan;
    }

    /** Call when the user taps "Scan Ingredients". */
    public void onScanClicked() {
        _navigateToScan.setValue(true);
    }

    /** Reset after navigation has been handled to avoid re-triggering on config change. */
    public void onNavigated() {
        _navigateToScan.setValue(false);
    }
}
