/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.architecture.blueprints.todoapp.taskdetail;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.example.android.architecture.blueprints.todoapp.Injection;
import com.example.android.architecture.blueprints.todoapp.R;
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskActivity;
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskFragment;
import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.google.common.base.Preconditions;

import rx.Completable;
import rx.SingleSubscriber;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Main UI for the task detail screen.
 */
public class TaskDetailFragment extends Fragment {

    @NonNull
    public static final String ARGUMENT_TASK_ID = "TASK_ID";

    public static final int REQUEST_EDIT_TASK = 1;

    @Nullable
    private TextView mLoadingProgress;

    @Nullable
    private TextView mDetailTitle;

    @Nullable
    private TextView mDetailDescription;

    @Nullable
    private CheckBox mDetailCompleteStatus;

    @Nullable
    private TaskDetailViewModel mViewModel;

    @Nullable
    private CompositeSubscription mSubscription;

    public static TaskDetailFragment newInstance(@Nullable String taskId) {
        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_TASK_ID, taskId);
        TaskDetailFragment fragment = new TaskDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        bindViewModel();
    }

    @Override
    public void onPause() {
        unbindViewModel();
        super.onPause();
    }

    private void bindViewModel() {
        mSubscription = new CompositeSubscription();

        mSubscription.add(getViewModel().getTask()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Task>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        showMissingTask();
                    }

                    @Override
                    public void onNext(Task task) {
                        showTask(task);
                    }
                }));

        mSubscription.add(getViewModel().getLoadingIndicator()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        showMissingTask();
                    }

                    @Override
                    public void onNext(Boolean active) {
                        setLoadingIndicator(active);
                    }
                }));
    }

    private void unbindViewModel() {
        getSubscription().unsubscribe();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.taskdetail_frag, container, false);
        setHasOptionsMenu(true);
        mLoadingProgress = (TextView) root.findViewById(R.id.loading_progress);
        mDetailTitle = (TextView) root.findViewById(R.id.task_detail_title);
        mDetailDescription = (TextView) root.findViewById(R.id.task_detail_description);
        mDetailCompleteStatus = (CheckBox) root.findViewById(R.id.task_detail_complete);

        // Set up floating action button
        FloatingActionButton fab =
                (FloatingActionButton) getActivity().findViewById(R.id.fab_edit_task);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTask();
            }
        });

        mViewModel = Injection.provideTaskDetailsViewModel(getTaskId(), getContext());

        return root;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_delete:
                deleteTask();
                return true;
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.taskdetail_fragment_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void showTask(@NonNull Task task) {
        String title = task.getTitle();
        String description = task.getDescription();

        if (TextUtils.isEmpty(title)) {
            hideTitle();
        } else {
            showTitle(title);
        }

        if (TextUtils.isEmpty(description)) {
            hideDescription();
        } else {
            showDescription(description);
        }
        showCompletionStatus(task.isCompleted());
    }

    private void setLoadingIndicator(boolean active) {
        Preconditions.checkNotNull(mLoadingProgress);

        int visibility = active ? View.VISIBLE : View.GONE;
        mLoadingProgress.setVisibility(visibility);
    }

    private void hideDescription() {
        Preconditions.checkNotNull(mDetailDescription);
        getDetailDescription().setVisibility(View.GONE);
    }

    private void hideTitle() {
        getDetailTitle().setVisibility(View.GONE);
    }

    private void showDescription(@NonNull String description) {
        getDetailDescription().setVisibility(View.VISIBLE);
        getDetailDescription().setText(description);
    }

    private void showCompletionStatus(final boolean complete) {
        Preconditions.checkNotNull(mDetailCompleteStatus);

        mDetailCompleteStatus.setChecked(complete);
        mDetailCompleteStatus.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            completeTask();
                        } else {
                            activateTask();
                        }
                    }
                });
    }

    private void deleteTask() {
        getViewModel().deleteTask()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Completable.CompletableSubscriber() {
                    @Override
                    public void onCompleted() {
                        showTaskDeleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        showMissingTask();
                    }

                    @Override
                    public void onSubscribe(Subscription d) {
                        // nothing to do here
                    }
                });
    }

    private void editTask() {
        getSubscription().add(getViewModel().editTask()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<String>() {
                    @Override
                    public void onSuccess(String taskId) {
                        showEditTask(taskId);
                    }

                    @Override
                    public void onError(Throwable error) {
                        showMissingTask();
                    }
                }));
    }

    private void showEditTask(@Nullable String taskId) {
        Intent intent = new Intent(getContext(), AddEditTaskActivity.class);
        intent.putExtra(AddEditTaskFragment.ARGUMENT_EDIT_TASK_ID, taskId);
        startActivityForResult(intent, REQUEST_EDIT_TASK);
    }

    private void showTaskDeleted() {
        getActivity().finish();
    }

    private void completeTask() {
        getViewModel().completeTask()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Completable.CompletableSubscriber() {
                    @Override
                    public void onCompleted() {
                        showTaskMarkedComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        showMissingTask();
                    }

                    @Override
                    public void onSubscribe(Subscription d) {
                        // nothing to do here
                    }
                });
    }

    private void showTaskMarkedComplete() {
        Snackbar.make(getView(), getString(R.string.task_marked_complete), Snackbar.LENGTH_LONG)
                .show();
    }

    private void activateTask() {
        getViewModel().activateTask()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Completable.CompletableSubscriber() {
                    @Override
                    public void onCompleted() {
                        showTaskMarkedActive();
                    }

                    @Override
                    public void onError(Throwable e) {
                        showMissingTask();
                    }

                    @Override
                    public void onSubscribe(Subscription d) {
                        // nothing to do here
                    }
                });
    }

    private void showTaskMarkedActive() {
        Snackbar.make(getView(), getString(R.string.task_marked_active), Snackbar.LENGTH_LONG)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT_TASK) {
            // If the task was edited successfully, go back to the list.
            if (resultCode == Activity.RESULT_OK) {
                getActivity().finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showTitle(@NonNull String title) {
        getDetailTitle().setVisibility(View.VISIBLE);
        getDetailTitle().setText(title);
    }

    private void showMissingTask() {
        getDetailTitle().setText("");
        getDetailDescription().setText(getString(R.string.no_data));
    }

    @NonNull
    private TaskDetailViewModel getViewModel() {
        return Preconditions.checkNotNull(mViewModel);
    }

    @NonNull
    private CompositeSubscription getSubscription() {
        return Preconditions.checkNotNull(mSubscription);
    }

    @NonNull
    private TextView getDetailTitle() {
        return Preconditions.checkNotNull(mDetailTitle);
    }

    @NonNull
    private TextView getDetailDescription() {
        return Preconditions.checkNotNull(mDetailDescription);
    }

    @Nullable
    private String getTaskId() {
        if (getArguments() != null) {
            return getArguments().getString(ARGUMENT_TASK_ID);
        }
        return null;
    }
}
