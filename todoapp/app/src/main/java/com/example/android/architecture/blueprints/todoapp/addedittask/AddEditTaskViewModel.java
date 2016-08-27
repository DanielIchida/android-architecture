package com.example.android.architecture.blueprints.todoapp.addedittask;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource;

import rx.Observable;
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
    private String mTaskId;

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

    /**
     * Creates and saves a new task with a title and description.
     *
     * @param title       the title of the new task
     * @param description the description of the new task.
     * @return a stream that emits only once, after the task was created. The stream will emit an
     * error if the task is empty.
     */
    @NonNull
    public Observable<Void> createTask(@Nullable String title,
                                       @Nullable String description) {
        return saveTask(new Task(title, description));
    }

    /**
     * Updates the title and the description of the the task with the current task id in the
     * repository.
     *
     * @param title       the new title of the task
     * @param description the new description of the task
     * @return a stream that emits only once, after the task was updated. The stream will emit an
     * error if there is no task id or if the task is empty.
     */
    @NonNull
    public Observable<Void> updateTask(@Nullable String title,
                                       @Nullable String description) {
        if (mTaskId == null) {
            return Observable.error(new Exception("updateTask() was called but task is new."));
        }
        return saveTask(new Task(title, description, mTaskId));
    }

    @NonNull
    private Observable<Void> saveTask(@NonNull Task newTask) {
        if (newTask.isEmpty()) {
            return Observable.error(new Exception("Trying to save an empty task"));
        }

        return Observable.just(newTask)
                .map(new Func1<Task, Void>() {
                    @Override
                    public Void call(Task task) {
                        mTasksRepository.saveTask(task);
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
