package com.example.android.architecture.blueprints.todoapp.tasks;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.example.android.architecture.blueprints.todoapp.R;
import com.example.android.architecture.blueprints.todoapp.data.Task;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapter for the list of tasks displayed in {@link TasksFragment}
 */
class TasksAdapter extends BaseAdapter {

    @NonNull
    private List<Task> mTasks;

    @NonNull
    private TasksFragment.TaskItemListener mItemListener;

    public TasksAdapter(@NonNull List<Task> tasks,
                        @NonNull TasksFragment.TaskItemListener itemListener) {
        setList(tasks);
        mItemListener = itemListener;
    }

    public void replaceData(List<Task> tasks) {
        setList(tasks);
        notifyDataSetChanged();
    }

    private void setList(List<Task> tasks) {
        mTasks = checkNotNull(tasks);
    }

    @Override
    public int getCount() {
        return mTasks.size();
    }

    @Override
    public Task getItem(int i) {
        return mTasks.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View rowView = view;
        if (rowView == null) {
            LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
            rowView = inflater.inflate(R.layout.task_item, viewGroup, false);
        }

        final Task task = getItem(i);

        TextView titleTV = (TextView) rowView.findViewById(R.id.title);
        titleTV.setText(task.getTitleForList());

        CheckBox completeCB = (CheckBox) rowView.findViewById(R.id.complete);

        // Active/completed task UI
        completeCB.setChecked(task.isCompleted());
        if (task.isCompleted()) {
            rowView.setBackgroundDrawable(viewGroup.getContext()
                    .getResources().getDrawable(R.drawable.list_completed_touch_feedback));
        } else {
            rowView.setBackgroundDrawable(viewGroup.getContext()
                    .getResources().getDrawable(R.drawable.touch_feedback));
        }

        completeCB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!task.isCompleted()) {
                    mItemListener.onCompleteTaskClick(task);
                } else {
                    mItemListener.onActivateTaskClick(task);
                }
            }
        });

        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mItemListener.onTaskClick(task);
            }
        });

        return rowView;
    }
}
