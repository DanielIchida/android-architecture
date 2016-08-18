package com.example.android.architecture.blueprints.todoapp;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Base view model for all view models that create bindings.
 */
public abstract class AbstractViewModel implements BaseViewModel {

    @Nullable
    private CompositeSubscription mSubscriptions;

    @Override
    public void dispose() {
        unsubscribeFromDataStore();
    }

    @Override
    public final void subscribeToDataStore() {
        unsubscribeFromDataStore();
        mSubscriptions = new CompositeSubscription();
        subscribeToData(mSubscriptions);
    }

    @Override
    public void unsubscribeFromDataStore() {
        if (mSubscriptions != null) {
            mSubscriptions.clear();
            mSubscriptions = null;
        }
    }

    /**
     * Provides {@link CompositeSubscription} that all bindings should be registered to.
     *
     * @param subscription that holds the {@link Subscription}s created by view model
     */
    protected abstract void subscribeToData(@NonNull final CompositeSubscription subscription);

    @Nullable
    @VisibleForTesting
    CompositeSubscription getSubscriptions() {
        return mSubscriptions;
    }

}
