package com.example.gallery_group07.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.gallery_group07.R;
import com.example.gallery_group07.SharedViewModel;
import com.example.gallery_group07.constants.OptionMenuConstants;
import com.example.gallery_group07.interfaces.OptionMenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OptionMenuDialogFragment extends DialogFragment implements OptionMenuItem {
    private String optionName = "";
    protected View.OnClickListener optionClickedListener;
    protected List<OptionMenuItem> optionItems;
    public static final String LOG_TAG = "OptionMenuDialogFragment";
    //private final OptionMenuDialogFragment parentMenu;
    private final AppCompatActivity rootActivity;

    public AppCompatActivity getRootActivity() {
        return rootActivity;
    }

    // Combine both
    public OptionMenuDialogFragment(AppCompatActivity rootActivity, String optionName, String[] childOptionMenu, OptionMenuItem[] options) {
        this.rootActivity = rootActivity;
        this.optionName = optionName;

        int totalSize = (childOptionMenu != null ? childOptionMenu.length : 0) + (options != null ? options.length : 0);;
        this.optionItems = new ArrayList<>(totalSize);
        if (childOptionMenu != null && childOptionMenu.length > 0){
            for (String menu : childOptionMenu){
                this.optionItems.add(createMenu(rootActivity, menu));
            }
        }
        if (options != null && options.length > 0){
            this.optionItems.addAll(Arrays.asList(options));
        }
    }

    public final static OptionMenuDialogFragment createMenu(AppCompatActivity rootActivity, String menuName) {
        switch (menuName) {
            case OptionMenuConstants.ADD_TO_ALBUM_OPTION_MENU: {
                return new AddToAlbumOptionMenuFragment(rootActivity, "Add to Album", null);
            }
            case OptionMenuConstants.REMOVE_FROM_ALBUM_OPTION_MENU: {
                return new RemoveFromAlbumOptionMenuFragment(rootActivity, "Remove from Album", null);
            }
            default:
                return null;
        }
    }

    public OptionMenuDialogFragment(AppCompatActivity rootActivity, String optionName, String[] childOptionMenus) {
        this.rootActivity = rootActivity;
        this.optionName = optionName;
        if (childOptionMenus == null) {
            this.optionItems = new ArrayList<>();
            return;
        }
        this.optionItems = new ArrayList<>(childOptionMenus.length);
        for (String menu : childOptionMenus) {
            this.optionItems.add(createMenu(rootActivity, menu));
        }
    }

    public OptionMenuDialogFragment(AppCompatActivity rootActivity, String optionName, OptionMenuItem[] options) {
        this.rootActivity = rootActivity;
        this.optionName = optionName;
        if (options == null) {
            this.optionItems = new ArrayList<>();
            return;
        }
        this.optionItems = Arrays.asList(options);
    }

    public final void onOptionSelected(int position) {
        optionItems.get(position).onThisOptionSelected();
        dismiss();
    }

    ;

    public void createOptions() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        createOptions();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the dialog's layout
        return inflater.inflate(R.layout.option_menu_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView listView = view.findViewById(R.id.option_menu_listview);

        SharedViewModel viewModel = getViewModel();

        ArrayList<String> optionTexts = new ArrayList<>(optionItems.size());
        for (OptionMenuItem item : optionItems) {
            optionTexts.add(item.getOptionName());
        }
        OptionMenuAdapter adapter = new OptionMenuAdapter(optionTexts);
        Log.i(LOG_TAG, "Size " + optionItems.size());
        listView.setAdapter(adapter);

        optionClickedListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = listView.getPositionForView(view);
                onOptionSelected(position);
            }
        };
    }

    public final SharedViewModel getViewModel() {
        return (new ViewModelProvider(rootActivity)).get(SharedViewModel.class);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Set custom dimensions for the dialog
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            getDialog().getWindow().setGravity(Gravity.BOTTOM);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    @Override
    public String getOptionName() {
        return optionName;
    }

    @Override
    public void onThisOptionSelected() {
        if (!isVisible() && rootActivity != null) {
            show(rootActivity.getSupportFragmentManager(), "");
        }
    }

//    @Override
//    public OptionMenuDialogFragment getParent() {
//        return parentMenu;
//    }

//    public final OptionMenuDialogFragment getRootDialog(){
//        OptionMenuDialogFragment root = null;
//        OptionMenuDialogFragment current = this;
//        while (current != null){
//            root = current;
//            current = current.parentMenu;
//        }
//        return root;
//    }

    public class OptionMenuAdapter extends BaseAdapter {
        private final List<String> options;

        public OptionMenuAdapter(List<String> options) {
            this.options = options; // Safety
        }

        @Override
        public int getCount() {
            return options.size();
        }

        @Override
        public Object getItem(int position) {
            return options.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.option_button, parent, false);
            }
            Button optionButton = convertView.findViewById(R.id.option_button_text);
            optionButton.setOnClickListener(optionClickedListener);

            optionButton.setText(options.get(position));
            return convertView;
        }
    }
}

