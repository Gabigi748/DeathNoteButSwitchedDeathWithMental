package com.mindbody.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.mindbody.app.R;
import com.mindbody.app.model.Symptom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SymptomExpandableAdapter extends BaseExpandableListAdapter {

    private final Context context;
    private final List<String> groups;
    private final LinkedHashMap<String, List<String>> children;

    // Track selected symptoms: key = "group|symptom", value = severity
    private final Map<String, String> selectedSymptoms = new HashMap<>();
    private final Map<String, Boolean> checkedState = new HashMap<>();

    public SymptomExpandableAdapter(Context context, List<String> groups, LinkedHashMap<String, List<String>> children) {
        this.context = context;
        this.groups = groups;
        this.children = children;
    }

    @Override
    public int getGroupCount() {
        return groups.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        String group = groups.get(groupPosition);
        List<String> childList = children.get(group);
        return childList != null ? childList.size() : 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return groups.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        String group = groups.get(groupPosition);
        List<String> childList = children.get(group);
        return childList != null ? childList.get(childPosition) : "";
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
        }
        TextView tv = convertView.findViewById(android.R.id.text1);
        tv.setText(groups.get(groupPosition));
        tv.setPadding(48, 24, 24, 24);
        tv.setTextSize(16);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    android.R.layout.simple_list_item_1, parent, false);
            // Create custom layout programmatically
            android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
            layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            layout.setPadding(16, 8, 16, 8);
            layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

            CheckBox cb = new CheckBox(context);
            cb.setId(android.R.id.checkbox);
            cb.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Spinner spinner = new Spinner(context);
            spinner.setId(android.R.id.custom);
            android.widget.LinearLayout.LayoutParams spinnerParams = new android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            spinner.setLayoutParams(spinnerParams);

            String[] severities = {context.getString(R.string.severity_mild),
                    context.getString(R.string.severity_moderate),
                    context.getString(R.string.severity_severe)};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, severities);
            spinner.setAdapter(adapter);
            spinner.setVisibility(View.GONE);

            layout.addView(cb);
            layout.addView(spinner);

            convertView = layout;
        }

        String group = groups.get(groupPosition);
        String symptomName = (String) getChild(groupPosition, childPosition);
        String key = group + "|" + symptomName;

        android.widget.LinearLayout layout = (android.widget.LinearLayout) convertView;
        CheckBox cb = (CheckBox) layout.getChildAt(0);
        Spinner spinner = (Spinner) layout.getChildAt(1);

        cb.setOnCheckedChangeListener(null); // Remove old listener
        cb.setText(symptomName);
        cb.setChecked(Boolean.TRUE.equals(checkedState.get(key)));

        spinner.setVisibility(Boolean.TRUE.equals(checkedState.get(key)) ? View.VISIBLE : View.GONE);

        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checkedState.put(key, isChecked);
            spinner.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                selectedSymptoms.put(key, context.getString(R.string.severity_mild));
            } else {
                selectedSymptoms.remove(key);
            }
        });

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> adapterView, View view, int position, long id) {
                if (Boolean.TRUE.equals(checkedState.get(key))) {
                    String[] severities = {context.getString(R.string.severity_mild),
                            context.getString(R.string.severity_moderate),
                            context.getString(R.string.severity_severe)};
                    selectedSymptoms.put(key, severities[position]);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> adapterView) {}
        });

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public List<Symptom> getSelectedSymptoms() {
        List<Symptom> symptoms = new ArrayList<>();
        for (Map.Entry<String, String> entry : selectedSymptoms.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            if (parts.length == 2) {
                symptoms.add(new Symptom(parts[0], parts[1], entry.getValue()));
            }
        }
        return symptoms;
    }

    public void clearSelections() {
        selectedSymptoms.clear();
        checkedState.clear();
        notifyDataSetChanged();
    }
}
