package com.mindbody.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mindbody.app.R;

import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {

    private final List<DayItem> days;

    public CalendarAdapter(List<DayItem> days) {
        this.days = days;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DayItem item = days.get(position);
        holder.tvDay.setText(String.valueOf(item.day));

        if (item.checked) {
            holder.viewDot.setBackgroundResource(R.drawable.bg_calendar_checked);
        } else {
            holder.viewDot.setBackgroundResource(R.drawable.bg_calendar_unchecked);
        }
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View viewDot;
        TextView tvDay;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewDot = itemView.findViewById(R.id.view_dot);
            tvDay = itemView.findViewById(R.id.tv_day);
        }
    }

    public static class DayItem {
        public int day;
        public boolean checked;

        public DayItem(int day, boolean checked) {
            this.day = day;
            this.checked = checked;
        }
    }
}
