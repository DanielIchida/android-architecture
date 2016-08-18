package com.example.android.architecture.blueprints.todoapp;

/**
 * Interface for view model for all view models that create bindings.
 */
public interface BaseViewModel {

    /**
     * Cleans up the view model
     */
    void dispose();

    /**
     * Cleans up current subscriptions and subscribes to new events.
     */
    void subscribeToDataStore();

    /**
     * Cleans up / unsubscribes from existing notifications.
     */
    void unsubscribeFromDataStore();
}
