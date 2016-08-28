package com.example.android.architecture.blueprints.todoapp.addedittask;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.observers.TestSubscriber;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the implementation of {@link AddEditTaskViewModel}.
 */
public class AddEditTaskViewModelTest {

    private final String TITLE = "title";
    private final String DESCRIPTION = "descripton";
    private final Task TASK = new Task(TITLE, DESCRIPTION);

    private TestSubscriber<Void> mTestSubscriber;
    private TestSubscriber<Task> mTaskTestSubscriber;

    @Mock
    private TasksRepository mTasksRepository;

    private AddEditTaskViewModel mViewModel;

    @Before
    public void setupStatisticsPresenter() {
        // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
        // inject the mocks in the test the initMocks method needs to be called.
        MockitoAnnotations.initMocks(this);

        mTestSubscriber = new TestSubscriber<>();
        mTaskTestSubscriber = new TestSubscriber<>();
    }

    @Test
    public void getTask_emits_whenTaskId() {
        // Given a task in the repository
        when(mTasksRepository.getTask(TASK.getId())).thenReturn(Observable.just(TASK));
        // Get a reference to the class under test for the same task id
        mViewModel = new AddEditTaskViewModel(TASK.getId(), mTasksRepository);

        //When subscribing to getTask
        mViewModel.getTask().subscribe(mTaskTestSubscriber);

        // The task is returned
        mTaskTestSubscriber.assertValue(TASK);
    }

    @Test
    public void getTask_doesNotEmit_whenTaskIdNull() {
        // Given no task id set
        mViewModel = new AddEditTaskViewModel(null, mTasksRepository);

        //When subscribing to getTask
        mViewModel.getTask().subscribe(mTaskTestSubscriber);

        // No value is emitted
        mTaskTestSubscriber.assertNoValues();
    }

    @Test
    public void createTask_savesTask_whenTaskNotEmpty() {
        // Given no task id set
        mViewModel = new AddEditTaskViewModel(null, mTasksRepository);

        // When creating a task with non empty title and description
        mViewModel.createTask(TITLE, DESCRIPTION).subscribe();

        // A task is saved in the repository
        verify(mTasksRepository).saveTask(any(Task.class));
    }

    @Test
    public void createTask_emits_whenTaskNotEmpty() {
        // Given no task id set
        mViewModel = new AddEditTaskViewModel(null, mTasksRepository);

        // When creating a task with non empty title and description
        mViewModel.createTask(TITLE, DESCRIPTION).subscribe(mTestSubscriber);

        // A value is emitted
        mTestSubscriber.assertValueCount(1);
    }

    @Test
    public void createTask_doesNotSaveTask_whenTaskEmpty() {
        // Given no task id set
        mViewModel = new AddEditTaskViewModel(null, mTasksRepository);

        // When creating a task with empty title and description
        mViewModel.createTask(null, null).subscribe(mTestSubscriber);

        // No task is saved in the repository
        verify(mTasksRepository, never()).saveTask(any(Task.class));
    }

    @Test
    public void createTask_emitsError_whenTaskEmpty() {
        // Given no task id set
        mViewModel = new AddEditTaskViewModel(null, mTasksRepository);

        // When creating a task with empty title and description
        mViewModel.createTask(null, null).subscribe(mTestSubscriber);

        // An error is emitted
        mTestSubscriber.assertError(Exception.class);
    }

    @Test
    public void updateTask_emitsError_whenTaskIdEmpty() {
        // Given no task id set
        mViewModel = new AddEditTaskViewModel(null, mTasksRepository);

        // When updating a task with title and description
        mViewModel.updateTask(TITLE, DESCRIPTION).subscribe(mTestSubscriber);

        // An error is emitted
        mTestSubscriber.assertError(Exception.class);
    }

    @Test
    public void updateTask_savesTask_whenTaskNotEmpty() {
        // Given a view model with taskId set
        mViewModel = new AddEditTaskViewModel(TASK.getId(), mTasksRepository);

        // When updating a task with non empty title and description
        mViewModel.updateTask(TITLE, DESCRIPTION).subscribe();

        // A task is saved in the repository
        verify(mTasksRepository).saveTask(any(Task.class));
    }

    @Test
    public void updateTask_emits_whenTaskNotEmpty() {
        // Given a view model with taskId set
        mViewModel = new AddEditTaskViewModel(TASK.getId(), mTasksRepository);

        // When updating a task with non empty title and description
        mViewModel.updateTask(TITLE, DESCRIPTION).subscribe(mTestSubscriber);

        // A value is emitted
        mTestSubscriber.assertValueCount(1);
    }

    @Test
    public void updateTask_doesNotSaveTask_whenTaskEmpty() {
        // Given a view model with taskId set
        mViewModel = new AddEditTaskViewModel(TASK.getId(), mTasksRepository);

        // When updating a task with empty title and description
        mViewModel.updateTask(null, null).subscribe(mTestSubscriber);

        // No task is saved in the repository
        verify(mTasksRepository, never()).saveTask(any(Task.class));
    }

    @Test
    public void updateTask_emitsError_whenTaskEmpty() {
        // Given a view model with taskId set
        mViewModel = new AddEditTaskViewModel(TASK.getId(), mTasksRepository);

        // When updating a task with empty title and description
        mViewModel.updateTask(null, null).subscribe(mTestSubscriber);

        // An error is emitted
        mTestSubscriber.assertError(Exception.class);
    }
}
