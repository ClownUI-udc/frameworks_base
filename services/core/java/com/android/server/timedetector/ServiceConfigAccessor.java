/*
 * Copyright 2021 The Android Open Source Project
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
package com.android.server.timedetector;

import android.annotation.NonNull;
import android.annotation.UserIdInt;
import android.app.time.TimeConfiguration;

import com.android.server.timedetector.TimeDetectorStrategy.Origin;
import com.android.server.timezonedetector.ConfigurationChangeListener;

import java.time.Instant;

/**
 * An interface that provides access to service configuration for time detection. This hides
 * how configuration is split between static, compile-time config, dynamic server-pushed flags and
 * user settings. It provides listeners to signal when values that affect different components have
 * changed.
 */
public interface ServiceConfigAccessor {

    /**
     * Adds a listener that will be invoked when {@link ConfigurationInternal} may have changed.
     * The listener is invoked on the main thread.
     */
    void addConfigurationInternalChangeListener(@NonNull ConfigurationChangeListener listener);

    /**
     * Removes a listener previously added via {@link
     * #addConfigurationInternalChangeListener(ConfigurationChangeListener)}.
     */
    void removeConfigurationInternalChangeListener(@NonNull ConfigurationChangeListener listener);

    /**
     * Returns a snapshot of the {@link ConfigurationInternal} for the current user. This is only a
     * snapshot so callers must use {@link
     * #addConfigurationInternalChangeListener(ConfigurationChangeListener)} to be notified when it
     * changes.
     */
    @NonNull
    ConfigurationInternal getCurrentUserConfigurationInternal();

    /**
     * Returns the absolute threshold below which the system clock need not be updated. i.e. if
     * setting the system clock would adjust it by less than this (either backwards or forwards)
     * then it need not be set.
     */
    int systemClockUpdateThresholdMillis();

    /**
     * Returns a lower bound for valid automatic times. It is guaranteed to be in the past,
     * i.e. it is unrelated to the current system clock time.
     * It holds no other meaning; it could be related to when the device system image was built,
     * or could be updated by a mainline module.
     */
    @NonNull
    Instant autoTimeLowerBound();

    /**
     * Returns the order to look at time suggestions when automatically detecting time.
     * See {@code #ORIGIN_} constants
     */
    @NonNull
    @Origin int[] getOriginPriorities();

    /**
     * Updates the configuration properties that control a device's time behavior.
     *
     * <p>This method returns {@code true} if the configuration was changed,
     * {@code false} otherwise.
     */
    boolean updateConfiguration(
            @UserIdInt int userId, @NonNull TimeConfiguration requestedConfiguration);

    /**
     * Returns a snapshot of the configuration that controls time zone detector behavior for the
     * specified user.
     */
    @NonNull
    ConfigurationInternal getConfigurationInternal(@UserIdInt int userId);
}
