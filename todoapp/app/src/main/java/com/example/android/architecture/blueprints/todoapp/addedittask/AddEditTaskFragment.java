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

import rx.Completable;
import rx.Subscription;
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
    private CompositeSubscription mSubscription;

    @Nullable
    private AddEditTaskViewModel mViewModel;

    public static AddEditTaskFragment newInstance() {
        return new AddEditTaskFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        bind();
    }

    @Override
    public void onPause() {
        unbind();
        super.onPause();
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

        mViewModel = Injection.provideAddEditTaskViewModel(getContext(), getTaskId());

        FloatingActionButton fab =
                (FloatingActionButton) getActivity().findViewById(R.id.fab_edit_task_done);
        fab.setImageResource(R.drawable.ic_done);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTask();
            }
        });
    }

    private void saveTask() {
        Preconditions.checkNotNull(mTitle);
        Preconditions.checkNotNull(mDescription);

        getViewModel()
                .saveTask(mTitle.getText().toString(), mDescription.getText().toString())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Completable.CompletableSubscriber() {
                    @Override
                    public void onCompleted() {
                        showTasksList();
                    }

                    @Override
                    public void onError(Throwable e) {
                        showEmptyTaskError();
                    }

                    @Override
                    public void onSubscribe(Subscription d) {
                        //nothing to do here
                    }
                });
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
        Preconditions.checkNotNull(mTitle);
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

    @Nullable
    private String getTaskId() {
        if (getArguments() != null) {
            return getArguments().getString(ARGUMENT_EDIT_TASK_ID);
        }
        return null;
    }

    @NonNull
    private AddEditTaskViewModel getViewModel() {
        return Preconditions.checkNotNull(mViewModel);
    }

    @NonNull
    private CompositeSubscription getSubscription() {
        return Preconditions.checkNotNull(mSubscription);
    }
}
