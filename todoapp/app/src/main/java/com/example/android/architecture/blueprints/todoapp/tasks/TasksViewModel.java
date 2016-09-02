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

package com.example.android.architecture.blueprints.todoapp.tasks;

import android.support.annotation.NonNull;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository;
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource;

import java.util.List;

import rx.Completable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens to user actions from the UI ({@link TasksFragment}), retrieves the data and updates the
 * UI as required.
 */
public class TasksViewModel {

    @NonNull
    private final TasksRepository mTasksRepository;

    @NonNull
    private TasksFilterType mCurrentFiltering = TasksFilterType.ALL_TASKS;

    @NonNull
    private final BehaviorSubject<TasksFilterType> mFilteringSubject;

    @NonNull
    private final BehaviorSubject<Boolean> mLoadTasksSubject;

    @NonNull
    private final BehaviorSubject<Boolean> mShowLoadingSubject;

    private boolean mFirstLoad = true;

    public TasksViewModel(@NonNull TasksRepository tasksRepository) {
        mTasksRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null");

        mFilteringSubject = BehaviorSubject.create(TasksFilterType.ALL_TASKS);
        mLoadTasksSubject = BehaviorSubject.create(true);
        mShowLoadingSubject = BehaviorSubject.create(false);
    }

    @Override
    public void subscribe() {
        loadTasks(false);
    }

    public void loadTasks(boolean forceUpdate) {
        mLoadTasksSubject.onNext(forceUpdate);
    }

    @NonNull
    public Observable<List<Task>> getTasksFromRepository() {
        return mTasksRepository.getTasks()
                .flatMap(new Func1<List<Task>, Observable<List<Task>>>() {
                    @Override
                    public Observable<List<Task>> call(List<Task> tasks) {
                        return Observable.from(tasks).filter(new Func1<Task, Boolean>() {
                            @Override
                            public Boolean call(Task task) {
                                switch (mCurrentFiltering) {
                                    case ACTIVE_TASKS:
                                        return task.isActive();
                                    case COMPLETED_TASKS:
                                        return task.isCompleted();
                                    case ALL_TASKS:
                                    default:
                                        return true;
                                }
                            }
                        }).toList();
                    }
                });
    }

    @NonNull
    public Observable<Boolean> getLoadingIndicator() {
        return mShowLoadingSubject.distinctUntilChanged();
    }

    @NonNull
    public Observable<List<Task>> getTasks() {
        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        return mLoadTasksSubject
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean forceUpdate) {
                        if (forceUpdate) {
                            mTasksRepository.refreshTasks();
                            mLoadTasksSubject.onNext(true);
                        }
                    }
                })
                .doOnTerminate(new Action0() {
                    @Override
                    public void call() {
                        mLoadTasksSubject.onNext(false);
                    }
                })
                .flatMap(new Func1<Boolean, Observable<List<Task>>>() {
                    @Override
                    public Observable<List<Task>> call(Boolean aBoolean) {
                        return getTasksFromRepository();
                    }
                });


        Subscription subscription = mTasksRepository
                .getTasks()
                .flatMap(new Func1<List<Task>, Observable<Task>>() {
                    @Override
                    public Observable<Task> call(List<Task> tasks) {
                        return Observable.from(tasks);
                    }
                })
                .filter(new Func1<Task, Boolean>() {
                    @Override
                    public Boolean call(Task task) {
                        switch (mCurrentFiltering) {
                            case ACTIVE_TASKS:
                                return task.isActive();
                            case COMPLETED_TASKS:
                                return task.isCompleted();
                            case ALL_TASKS:
                            default:
                                return true;
                        }
                    }
                })
                .toList()
                .subscribe(new Observer<List<Task>>() {
                    @Override
                    public void onCompleted() {
                        mTasksView.setLoadingIndicator(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        mTasksView.showLoadingTasksError();
                    }

                    @Override
                    public void onNext(List<Task> tasks) {
                        processTasks(tasks);
                    }
                });
    }

    private void processTasks(@NonNull List<Task> tasks) {
        if (tasks.isEmpty()) {
            // Show a message indicating there are no tasks for that filter type.
            processEmptyTasks();
        } else {
            // Show the list of tasks
            mTasksView.showTasks(tasks);
            // Set the filter label's text.
            showFilterLabel();
        }
    }

    private void processEmptyTasks() {
        switch (mCurrentFiltering) {
            case ACTIVE_TASKS:
                mTasksView.showNoActiveTasks();
                break;
            case COMPLETED_TASKS:
                mTasksView.showNoCompletedTasks();
                break;
            default:
                mTasksView.showNoTasks();
                break;
        }
    }

    @NonNull
    public Completable completeTask(@NonNull final Task completedTask) {
        checkNotNull(completedTask, "completedTask cannot be null!");
        return Completable.fromAction(new Action0() {
            @Override
            public void call() {
                mTasksRepository.completeTask(completedTask);
                mLoadTasksSubject.onNext(false);
            }
        });
    }

    @NonNull
    public Completable activateTask(@NonNull final Task activeTask) {
        checkNotNull(activeTask, "activeTask cannot be null!");
        return Completable.fromAction(new Action0() {
            @Override
            public void call() {
                mTasksRepository.activateTask(activeTask);
                mLoadTasksSubject.onNext(false);
            }
        });
    }

    @NonNull
    public Completable clearCompletedTasks() {
        return Completable.fromAction(new Action0() {
            @Override
            public void call() {
                mTasksRepository.clearCompletedTasks();
                mLoadTasksSubject.onNext(false);
            }
        });
    }

    /**
     * Sets the current task filtering type.
     *
     * @param requestType Can be {@link TasksFilterType#ALL_TASKS},
     *                    {@link TasksFilterType#COMPLETED_TASKS}, or
     *                    {@link TasksFilterType#ACTIVE_TASKS}
     */
    public void setFiltering(@NonNull TasksFilterType requestType) {
        mFilteringSubject.onNext(requestType);
    }

    @NonNull
    public Observable<String> getFilteringLabel(){
    }

    @NonNull
    public Observable<TasksFilterType> getFiltering() {
        return mFilteringSubject.asObservable();
    }

}
