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

package com.example.android.architecture.blueprints.todoapp.statistics;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.example.android.architecture.blueprints.todoapp.AbstractViewModel;
import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository;
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider;

import java.util.List;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens to user actions from the UI ({@link StatisticsFragment}), retrieves the data and updates
 * the UI as required.
 */
public class StatisticsViewModel extends AbstractViewModel {

    @NonNull
    private final TasksRepository mTasksRepository;

    @NonNull
    private final BaseSchedulerProvider mSchedulerProvider;

    @NonNull
    private BehaviorSubject<Boolean> mProgressIndicatorSubject;

    public StatisticsViewModel(@NonNull TasksRepository tasksRepository,
                               @NonNull BaseSchedulerProvider schedulerProvider) {
        mTasksRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null");
        mSchedulerProvider = checkNotNull(schedulerProvider, "schedulerProvider cannot be null");

        mProgressIndicatorSubject = BehaviorSubject.create(true);
    }

    public Observable<Pair<Integer, Integer>> getStatistics() {
        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        Observable<Task> tasks = mTasksRepository
                .getTasks()
                .flatMap(new Func1<List<Task>, Observable<Task>>() {
                    @Override
                    public Observable<Task> call(List<Task> tasks) {
                        return Observable.from(tasks);
                    }
                });
        Observable<Integer> completedTasks = tasks.filter(new Func1<Task, Boolean>() {
            @Override
            public Boolean call(Task task) {
                return task.isCompleted();
            }
        }).count();
        Observable<Integer> activeTasks = tasks.filter(new Func1<Task, Boolean>() {
            @Override
            public Boolean call(Task task) {
                return task.isActive();
            }
        }).count();

        return Observable
                .zip(completedTasks, activeTasks, new Func2<Integer, Integer, Pair<Integer, Integer>>() {
                    @Override
                    public Pair<Integer, Integer> call(Integer completed, Integer active) {
                        return Pair.create(active, completed);
                    }
                })
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        mProgressIndicatorSubject.onNext(false);
                    }
                });
    }

    public Observable<Boolean> getProgressIndicator() {
        return mProgressIndicatorSubject.asObservable();
    }

    @Override
    protected void subscribeToData(@NonNull CompositeSubscription subscription) {

    }
//
//    private void loadStatistics() {
//        mStatisticsView.setProgressIndicator(true);
//
//        // The network request might be handled in a different thread so make sure Espresso knows
//        // that the app is busy until the response is handled.
//        EspressoIdlingResource.increment(); // App is busy until further notice
//
//        Observable<Task> tasks = mTasksRepository
//                .getTasks()
//                .flatMap(new Func1<List<Task>, Observable<Task>>() {
//                    @Override
//                    public Observable<Task> call(List<Task> tasks) {
//                        return Observable.from(tasks);
//                    }
//                });
//        Observable<Integer> completedTasks = tasks.filter(new Func1<Task, Boolean>() {
//            @Override
//            public Boolean call(Task task) {
//                return task.isCompleted();
//            }
//        }).count();
//        Observable<Integer> activeTasks = tasks.filter(new Func1<Task, Boolean>() {
//            @Override
//            public Boolean call(Task task) {
//                return task.isActive();
//            }
//        }).count();
//        Subscription subscription = Observable
//                .zip(completedTasks, activeTasks, new Func2<Integer, Integer, Pair<Integer, Integer>>() {
//                    @Override
//                    public Pair<Integer, Integer> call(Integer completed, Integer active) {
//                        return Pair.create(active, completed);
//                    }
//                })
//                .subscribeOn(mSchedulerProvider.computation())
//                .observeOn(mSchedulerProvider.ui())
//                .subscribe(new Action1<Pair<Integer, Integer>>() {
//                    @Override
//                    public void call(Pair<Integer, Integer> stats) {
//                        mStatisticsView.showStatistics(stats.first, stats.second);
//                    }
//                }, new Action1<Throwable>() {
//                    @Override
//                    public void call(Throwable throwable) {
//                        mStatisticsView.showLoadingStatisticsError();
//                    }
//                }, new Action0() {
//                    @Override
//                    public void call() {
//                        mStatisticsView.setProgressIndicator(false);
//                    }
//                });
//        mSubscriptions.add(subscription);
//    }
}
