/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.addedittask;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.architecture.blueprints.todoapp.Injection;
import com.example.android.architecture.blueprints.todoapp.R;
import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.google.common.base.Preconditions;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Main UI for the add task screen. Users can enter a task title and description.
 */
public class AddEditTaskFragment extends Fragment {

    public static final String ARGUMENT_EDIT_TASK_ID = "EDIT_TASK_ID";

    @Nullable
    private TextView mTitle;

    @Nullable
    private TextView mDescription;

    @Nullable
    private String mEditedTaskId;

    @Nullable
    private CompositeSubscription mSubscription;

    @Nullable
    private AddEditTaskViewModel mViewModel;

    public static AddEditTaskFragment newInstance() {
        return new AddEditTaskFragment();
    }

    public AddEditTaskFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        bind();
    }

    @Override
    public void onPause() {
        super.onPause();
        unbind();
    }

    private void bind() {
        mSubscription = new CompositeSubscription();

        mSubscription.add(getViewModel().getTask()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Task>() {
                    @Override
                    public void call(Task task) {
                        setTask(task);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        showEmptyTaskError();
                    }
                }));
    }

    private void unbind() {
        getSubscription().unsubscribe();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setTaskIdIfAny();

        mViewModel = Injection.provideAddEditTaskViewModel(getContext(), mEditedTaskId);

        FloatingActionButton fab =
                (FloatingActionButton) getActivity().findViewById(R.id.fab_edit_task_done);
        fab.setImageResource(R.drawable.ic_done);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNewTask()) {
                    createTask();
                } else {
                    updateTask();
                }
            }
        });
    }

    private void updateTask() {
        Preconditions.checkNotNull(mSubscription);
        Preconditions.checkNotNull(mTitle);
        Preconditions.checkNotNull(mDescription);

        mSubscription.add(getViewModel()
                .updateTask(mTitle.getText().toString(), mDescription.getText().toString())
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        // After an edit, go back to the list.
                        showTasksList();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        showEmptyTaskError();
                    }
                }));
    }

    private void createTask() {
        Preconditions.checkNotNull(mTitle);
        Preconditions.checkNotNull(mDescription);

        getSubscription().add(
                getViewModel().createTask(
                        mTitle.getText().toString(),
                        mDescription.getText().toString())
                        .subscribe(new Subscriber<Void>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {
                                showEmptyTaskError();
                            }

                            @Override
                            public void onNext(Void aVoid) {
                                showTasksList();
                            }
                        }));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.addtask_frag, container, false);
        mTitle = (TextView) root.findViewById(R.id.add_task_title);
        mDescription = (TextView) root.findViewById(R.id.add_task_description);

        setHasOptionsMenu(true);
        setRetainInstance(true);
        return root;
    }

    private void showEmptyTaskError() {
        Snackbar.make(mTitle, getString(R.string.empty_task_message), Snackbar.LENGTH_LONG).show();
    }

    private void showTasksList() {
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    private void setTask(@NonNull Task task) {
        if (!isActive()) {
            return;
        }

        Preconditions.checkNotNull(mTitle);
        Preconditions.checkNotNull(mDescription);

        mTitle.setText(task.getTitle());
        mDescription.setText(task.getDescription());
    }

    private boolean isActive() {
        return isAdded();
    }

    private void setTaskIdIfAny() {
        if (getArguments() != null && getArguments().containsKey(ARGUMENT_EDIT_TASK_ID)) {
            mEditedTaskId = getArguments().getString(ARGUMENT_EDIT_TASK_ID);
        }
    }

    private boolean isNewTask() {
        return mEditedTaskId == null;
    }

    @NonNull
    private AddEditTaskViewModel getViewModel() {
        Preconditions.checkNotNull(mViewModel);
        return mViewModel;
    }

    @NonNull
    private CompositeSubscription getSubscription() {
        Preconditions.checkNotNull(mSubscription);
        return mSubscription;
    }
}
