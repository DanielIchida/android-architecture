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

package com.example.android.architecture.blueprints.todoapp.data.source;

import android.content.Context;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider;
import com.google.common.collect.Lists;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import rx.Completable;
import rx.Observable;
import rx.observers.TestSubscriber;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the implementation of the in-memory repository with cache.
 */
public class TasksRepositoryTest {

    private final static String TASK_TITLE = "title";

    private final static String TASK_TITLE2 = "title2";

    private final static String TASK_TITLE3 = "title3";

    private static List<Task> TASKS = Lists.newArrayList(new Task("Title1", "Description1"),
            new Task("Title2", "Description2"));

    private final static Task ACTIVE_TASK = new Task(TASK_TITLE, "Some Task Description");

    private final static Task COMPLETED_TASK = new Task(TASK_TITLE, "Some Task Description", true);

    private TasksRepository mTasksRepository;

    private TestSubscriber<List<Task>> mTasksTestSubscriber;

    @Mock
    private TasksDataSource mTasksRemoteDataSource;

    @Mock
    private TasksDataSource mTasksLocalDataSource;

    @Mock
    private Context mContext;

    private TestSubscriber mTestSubscriber = new TestSubscriber();

    @Before
    public void setupTasksRepository() {
        // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
        // inject the mocks in the test the initMocks method needs to be called.
        MockitoAnnotations.initMocks(this);

        // Get a reference to the class under test
        mTasksRepository = TasksRepository.getInstance(
                mTasksRemoteDataSource, mTasksLocalDataSource, new ImmediateSchedulerProvider());

        mTasksTestSubscriber = new TestSubscriber<>();
    }

    @After
    public void destroyRepositoryInstance() {
        TasksRepository.destroyInstance();
    }

    @Ignore
    @Test
    public void getTasks_repositoryCachesAfterFirstSubscription_whenTasksAvailableInLocalStorage() {
        // Given that the local data source has data available
        // And the remote data source does not have any data available
        new ArrangeBuilder()
                .withTasksAvailable(mTasksLocalDataSource, TASKS)
                .withTasksNotAvailable(mTasksRemoteDataSource);

        // When two subscriptions are set
        TestSubscriber<List<Task>> testSubscriber1 = new TestSubscriber<>();
        mTasksRepository.getTasks().subscribe(testSubscriber1);

        TestSubscriber<List<Task>> testSubscriber2 = new TestSubscriber<>();
        mTasksRepository.getTasks().subscribe(testSubscriber2);

        // Then tasks were only requested once from remote and local sources
        verify(mTasksRemoteDataSource).getTasks();
        verify(mTasksLocalDataSource).getTasks();

        testSubscriber1.assertValue(TASKS);
        testSubscriber2.assertValue(TASKS);
    }

    @Ignore
    @Test
    public void getTasks_repositoryCachesAfterFirstSubscription_whenTasksAvailableInRemoteStorage() {
        // Given that the local data source has data available
        // And the remote data source does not have any data available
        new ArrangeBuilder()
                .withTasksAvailable(mTasksRemoteDataSource, TASKS)
                .withTasksNotAvailable(mTasksLocalDataSource);

        // When two subscriptions are set
        TestSubscriber<List<Task>> testSubscriber1 = new TestSubscriber<>();
        mTasksRepository.getTasks().subscribe(testSubscriber1);

        TestSubscriber<List<Task>> testSubscriber2 = new TestSubscriber<>();
        mTasksRepository.getTasks().subscribe(testSubscriber2);

        // Then tasks were only requested once from remote and local sources
        verify(mTasksRemoteDataSource).getTasks();
        verify(mTasksLocalDataSource).getTasks();

        testSubscriber1.assertValue(TASKS);
        testSubscriber2.assertValue(TASKS);
    }

    @Ignore
    @Test
    public void getTasks_requestsAllTasksFromLocalDataSource() {
        // Given that the local data source has data available
        // And the remote data source does not have any data available
        new ArrangeBuilder()
                .withTasksAvailable(mTasksLocalDataSource, TASKS)
                .withTasksNotAvailable(mTasksRemoteDataSource);

        // When tasks are requested from the tasks repository
        mTasksRepository.getTasks().subscribe(mTasksTestSubscriber);

        // Then tasks are loaded from the local data source
        verify(mTasksLocalDataSource).getTasks();
        mTasksTestSubscriber.assertValue(TASKS);
    }

    @Ignore
    @Test
    public void saveTask_savesTaskToServiceAPI() {
        // Given a stub task with title and description
        when(mTasksLocalDataSource.saveTask(ACTIVE_TASK)).thenReturn(Completable.complete());
        when(mTasksRemoteDataSource.saveTask(ACTIVE_TASK)).thenReturn(Completable.complete());

        // When a task is saved to the tasks repository
        mTasksRepository.saveTask(ACTIVE_TASK).subscribe();

        // Then the service API and persistent repository are called and the cache is updated
        verify(mTasksRemoteDataSource).saveTask(ACTIVE_TASK);
        verify(mTasksLocalDataSource).saveTask(ACTIVE_TASK);
    }

    @Test
    public void completeTask_completesTask() {
        // Given that a task is completed successfully in local and remote data source
        new ArrangeBuilder()
                .withCompletedTask(mTasksLocalDataSource, ACTIVE_TASK)
                .withCompletedTask(mTasksRemoteDataSource, ACTIVE_TASK);

        // When a task is completed to the tasks repository
        mTasksRepository.completeTask(ACTIVE_TASK)
                .subscribe(mTestSubscriber);

        // The completable completes without error
        mTestSubscriber.assertCompleted();
        mTestSubscriber.assertNoErrors();
    }

    @Test
    public void completeTask_whenLocalDataSourceCompletesWithError_doesNotComplete() {
        // Given that a task is not completed successfully in local data source
        Exception exception = new RuntimeException("test");
        new ArrangeBuilder()
                .withTaskCompletesWithError(mTasksLocalDataSource, ACTIVE_TASK, exception)
                .withCompletedTask(mTasksRemoteDataSource, ACTIVE_TASK);

        // When a task is completed to the tasks repository
        mTasksRepository.completeTask(ACTIVE_TASK)
                .subscribe(mTestSubscriber);

        // The completable completes with error
        mTestSubscriber.assertError(exception);
        // The task is not completed in the remote data source
        verify(mTasksRemoteDataSource, never()).completeTask(ACTIVE_TASK);
    }

    @Test
    public void completeTaskId_completesTask() {
        // Given that a task is completed successfully in local and remote data source
        new ArrangeBuilder()
                .withCompletedTaskId(mTasksLocalDataSource, ACTIVE_TASK.getId())
                .withCompletedTaskId(mTasksRemoteDataSource, ACTIVE_TASK.getId());

        // When a task is completed to the tasks repository
        mTasksRepository.completeTask(ACTIVE_TASK.getId())
                .subscribe(mTestSubscriber);

        // The completable completes without error
        mTestSubscriber.assertCompleted();
        mTestSubscriber.assertNoErrors();
    }


    @Test
    public void activateTask_activatesTask() {
        // Given that a task is activated successfully in local and remote data source
        new ArrangeBuilder()
                .withActivatedTask(mTasksLocalDataSource, COMPLETED_TASK)
                .withActivatedTask(mTasksRemoteDataSource, COMPLETED_TASK);

        // When a completed task is activated to the tasks repository
        mTasksRepository.activateTask(COMPLETED_TASK)
                .subscribe(mTestSubscriber);

        // The completable completes without error
        mTestSubscriber.assertCompleted();
        mTestSubscriber.assertNoErrors();
    }

    @Test
    public void activateTaskId_activatesTask() {
        // Given that a task is activated successfully in local and remote data source
        new ArrangeBuilder()
                .withActivatedTaskId(mTasksLocalDataSource, COMPLETED_TASK.getId())
                .withActivatedTaskId(mTasksRemoteDataSource, COMPLETED_TASK.getId());

        // When a completed task is activated with its id to the tasks repository
        mTasksRepository.activateTask(COMPLETED_TASK.getId())
                .subscribe(mTestSubscriber);

        // The completable completes without error
        mTestSubscriber.assertCompleted();
        mTestSubscriber.assertNoErrors();
    }

    @Ignore
    @Test
    public void getTask_requestsSingleTaskFromLocalDataSource() {
        // Given a stub completed task with title and description in the local repository
        // And the task not available in the remote repository
        new ArrangeBuilder()
                .withTaskAvailable(mTasksLocalDataSource, COMPLETED_TASK)
                .withTaskNotAvailable(mTasksRemoteDataSource, COMPLETED_TASK.getId());

        // When a task is requested from the tasks repository
        TestSubscriber<Task> testSubscriber = new TestSubscriber<>();
        mTasksRepository.getTask(COMPLETED_TASK.getId()).subscribe(testSubscriber);

        // Then the task is loaded from the database
        verify(mTasksLocalDataSource).getTask(eq(COMPLETED_TASK.getId()));
        testSubscriber.assertValue(COMPLETED_TASK);
    }

    @Ignore
    @Test
    public void getTask_whenDataNotLocal_fails() {
        // Given a stub completed task with title and description in the remote repository
        // And the task not available in the local repository
        new ArrangeBuilder()
                .withTaskAvailable(mTasksRemoteDataSource, COMPLETED_TASK)
                .withTaskNotAvailable(mTasksLocalDataSource, COMPLETED_TASK.getId());

        // When a task is requested from the tasks repository
        TestSubscriber<Task> testSubscriber = new TestSubscriber<>();
        mTasksRepository.getTask(COMPLETED_TASK.getId()).subscribe(testSubscriber);

        // Verify no data is returned
        testSubscriber.assertNoValues();
        // Verify that error is returned
        testSubscriber.assertError(NoSuchElementException.class);
    }

    @Test
    public void clearCompletedTasks_deletesTasksFromRemoteDataSource() {
        // When all completed tasks are cleared from the tasks repository
        mTasksRepository.clearCompletedTasks();

        // Verify that tasks are cleared from remote
        verify(mTasksRemoteDataSource).clearCompletedTasks();
    }

    @Test
    public void clearCompletedTasks_deletesTasksFromLocalDataSource() {
        // When all completed tasks are cleared from the tasks repository
        mTasksRepository.clearCompletedTasks();

        // Verify that tasks are cleared from local
        verify(mTasksLocalDataSource).clearCompletedTasks();
    }

    @Test
    public void deleteAllTasks_deletesTasksFromRemoteDataSource() {
        // When all tasks are deleted to the tasks repository
        mTasksRepository.deleteAllTasks();

        // Verify that tasks deleted from remote
        verify(mTasksRemoteDataSource).deleteAllTasks();
    }

    @Test
    public void deleteAllTasks_deletesTasksFromLocalDataSource() {
        // When all tasks are deleted to the tasks repository
        mTasksRepository.deleteAllTasks();

        // Verify that tasks deleted from local
        verify(mTasksLocalDataSource).deleteAllTasks();
    }

    @Test
    public void deleteTask_deletesTaskFromRemoteDataSource() {
        // When task deleted
        mTasksRepository.deleteTask(COMPLETED_TASK.getId());

        // Verify that the task was deleted from remote
        verify(mTasksRemoteDataSource).deleteTask(COMPLETED_TASK.getId());
    }

    @Test

    public void deleteTask_deletesTaskFromLocalDataSource() {
        // When task deleted
        mTasksRepository.deleteTask(COMPLETED_TASK.getId());

        // Verify that the task was deleted from local
        verify(mTasksLocalDataSource).deleteTask(COMPLETED_TASK.getId());
    }

    @Ignore
    @Test
    public void getTasksWithDirtyCache_tasksAreRetrievedFromRemote() {
        // Given that the remote data source has data available
        new ArrangeBuilder()
                .withTasksAvailable(mTasksRemoteDataSource, TASKS);

        // When calling getTasks in the repository with dirty cache
        mTasksRepository.refreshTasks();
        mTasksRepository.getTasks().subscribe(mTasksTestSubscriber);

        // Verify the tasks from the remote data source are returned, not the local
        verify(mTasksLocalDataSource, never()).getTasks();
        verify(mTasksRemoteDataSource).getTasks();
        mTasksTestSubscriber.assertValue(TASKS);
    }

    @Ignore
    @Test
    public void getTasksWithLocalDataSourceUnavailable_tasksAreRetrievedFromRemote() {
        // Given that the local data source has no data available
        // And the remote data source has data available
        new ArrangeBuilder()
                .withTasksNotAvailable(mTasksLocalDataSource)
                .withTasksAvailable(mTasksRemoteDataSource, TASKS);

        // When calling getTasks in the repository
        mTasksRepository.getTasks().subscribe(mTasksTestSubscriber);

        // Verify the tasks from the remote data source are returned
        verify(mTasksRemoteDataSource).getTasks();
        mTasksTestSubscriber.assertValue(TASKS);
    }

    @Ignore
    @Test
    public void getTasksWithBothDataSourcesUnavailable_firesOnDataUnavailable() {
        // Given that the local data source has no data available
        // And the remote data source has no data available
        new ArrangeBuilder()
                .withTasksNotAvailable(mTasksLocalDataSource)
                .withTasksNotAvailable(mTasksRemoteDataSource);

        // When calling getTasks in the repository
        mTasksRepository.getTasks().subscribe(mTasksTestSubscriber);

        // Verify no data is returned
        mTasksTestSubscriber.assertNoValues();
        // Verify that error is returned
        mTasksTestSubscriber.assertError(NoSuchElementException.class);
    }

    @Ignore
    @Test
    public void getTaskWithBothDataSourcesUnavailable_firesOnError() {
        // Given a task id
        final String taskId = "123";
        // And the local data source has no data available
        // And the remote data source has no data available
        new ArrangeBuilder()
                .withTaskNotAvailable(mTasksLocalDataSource, taskId)
                .withTaskNotAvailable(mTasksRemoteDataSource, taskId);

        // When calling getTask in the repository
        TestSubscriber<Task> testSubscriber = new TestSubscriber<>();
        mTasksRepository.getTask(taskId).subscribe(testSubscriber);

        // Verify that error is returned
        testSubscriber.assertError(NoSuchElementException.class);
    }

    @Ignore
    @Test
    public void getTasks_refreshesLocalDataSource() {
        // Given that the remote data source has data available
        new ArrangeBuilder()
                .withTasksAvailable(mTasksRemoteDataSource, TASKS);

        // Mark cache as dirty to force a reload of data from remote data source.
        mTasksRepository.refreshTasks();

        // When calling getTasks in the repository
        mTasksRepository.getTasks().subscribe(mTasksTestSubscriber);

        // Verify that the data fetched from the remote data source was saved in local.
        verify(mTasksLocalDataSource, times(TASKS.size())).saveTask(any(Task.class));
        mTasksTestSubscriber.assertValue(TASKS);
    }


    class ArrangeBuilder {

        ArrangeBuilder withTasksNotAvailable(TasksDataSource dataSource) {
            when(dataSource.getTasks()).thenReturn(Observable.just(Collections.<Task>emptyList()));
            return this;
        }

        ArrangeBuilder withTasksAvailable(TasksDataSource dataSource, List<Task> tasks) {
            // don't allow the data sources to complete.
            when(dataSource.getTasks()).thenReturn(Observable.just(tasks).concatWith(Observable.<List<Task>>never()));
            return this;
        }

        ArrangeBuilder withTaskNotAvailable(TasksDataSource dataSource, String taskId) {
            when(dataSource.getTask(eq(taskId))).thenReturn(Observable.<Task>just(null).concatWith(Observable.<Task>never()));
            return this;
        }

        ArrangeBuilder withTaskAvailable(TasksDataSource dataSource, Task task) {
            when(dataSource.getTask(eq(task.getId()))).thenReturn(Observable.just(task).concatWith(Observable.<Task>never()));
            return this;
        }

        ArrangeBuilder withActivatedTask(TasksDataSource dataSource, Task task) {
            when(dataSource.activateTask(task)).thenReturn(Completable.complete());
            return this;
        }

        ArrangeBuilder withActivatedTaskId(TasksDataSource dataSource, String taskId) {
            when(dataSource.activateTask(taskId)).thenReturn(Completable.complete());
            return this;
        }

        ArrangeBuilder withCompletedTask(TasksDataSource dataSource, Task task) {
            when(dataSource.completeTask(task)).thenReturn(Completable.complete());
            return this;
        }

        ArrangeBuilder withCompletedTaskId(TasksDataSource dataSource, String taskId) {
            when(dataSource.completeTask(taskId)).thenReturn(Completable.complete());
            return this;
        }

        ArrangeBuilder withTaskCompletesWithError(TasksDataSource dataSource,
                                                  Task task,
                                                  Exception exception) {
            when(dataSource.completeTask(task)).thenReturn(Completable.error(exception));
            return this;
        }
    }
}
