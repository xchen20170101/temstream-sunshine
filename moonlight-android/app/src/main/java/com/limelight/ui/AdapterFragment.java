package com.limelight.ui;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.limelight.R;

public class AdapterFragment extends Fragment {
    private AdapterFragmentCallbacks callbacks;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        
        if (context instanceof AdapterFragmentCallbacks) {
            callbacks = (AdapterFragmentCallbacks) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement AdapterFragmentCallbacks");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(callbacks.getAdapterFragmentLayoutId(), container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        callbacks.receiveAbsListView(getView().findViewById(R.id.fragmentView));
    }
}
