package com.example.android.architecture.blueprints.todoapp.addedittask;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource;

import java.util.concurrent.Callable;

import rx.Completable;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens to user actions from the UI ({@link AddEditTaskFragment}), retrieves the task from
 * repository, creates and updates a task.
 */
public class AddEditTaskViewModel {

    @NonNull
    private final TasksDataSource mTasksRepository;

    @Nullable
    private final String mTaskId;

    /**
     * Creates a ViewModel for the add/edit view.
     *
     * @param taskId          ID of the task to edit or null for a new task
     * @param tasksRepository a repository of data for tasks
     */
    public AddEditTaskViewModel(@Nullable String taskId,
                                @NonNull TasksDataSource tasksRepository) {
        mTaskId = taskId;
        mTasksRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null!");
    }

    @NonNull
    private void saveTask(@NonNull Task newTask) throws Exception {
        if (newTask.isEmpty()) {
            throw new Exception("Trying to save an empty task");
        }
        mTasksRepository.saveTask(newTask);
    }

    /**
     * Creates or updates a task.
     *
     * @param title       the title of the task
     * @param description the description of the task
     * @return a stream that emits only once, after the task was created. The stream will emit an
     * error if the task is empty.
     */
    @NonNull
    public Completable saveTask(@Nullable String title,
                                @Nullable String description) {
        final Task newTask;
        if (mTaskId == null) {
            newTask = new Task(title, description);
        } else {
            newTask = new Task(title, description, mTaskId);
        }

        return Completable.fromCallable(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                saveTask(newTask);
                return null;
            }
        });
    }

    /**
     * @return a stream containing the task with the current task id, if any.
     */
    @NonNull
    public Observable<Task> getTask() {
        return Observable.just(mTaskId)
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String taskId) {
                        return taskId != null;
                    }
                })
                .flatMap(new Func1<String, Observable<Task>>() {
                    @Override
                    public Observable<Task> call(String taskId) {
                        return mTasksRepository.getTask(taskId);
                    }
                });

    }
}
