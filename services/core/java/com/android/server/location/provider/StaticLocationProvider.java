/*
 * Copyright (C) 2026 Intel Corporation
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

package com.android.server.location.provider;

import android.location.Location;
import android.location.LocationResult;
import android.location.LocationResult.BadLocationException;
import android.location.provider.ProviderProperties;
import android.location.provider.ProviderRequest;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.util.Log;

import com.android.server.location.LocationHalWrapper;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.concurrent.Executor;

/**
 * Location provider that reports static location from vendor HAL.
 * Reads location once at startup and caches it until updated.
 *
 * @hide
 */
public final class StaticLocationProvider extends AbstractLocationProvider {

    private static final String TAG = "StaticLocationProvider";
    private static final String PROVIDER_NAME = "static";
    private static final float DEFAULT_ACCURACY_METERS = 100.0f;

    private final LocationHalWrapper mHalWrapper;

    public StaticLocationProvider(Executor executor) {
        super(executor, /*identity=*/ null,
                new ProviderProperties.Builder()
                        .setHasAltitudeSupport(false)
                        .setHasSpeedSupport(false)
                        .setHasBearingSupport(false)
                        .setAccuracy(ProviderProperties.ACCURACY_FINE)
                        .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                        .build(),
                Collections.emptySet());
        mHalWrapper = new LocationHalWrapper();
        setAllowed(true);
    }

    @Override
    protected void onStart() {
        reportCurrentLocation();
    }

    @Override
    protected void onStop() {
    }

    @Override
    protected void onSetRequest(ProviderRequest request) {
    }

    @Override
    protected void onFlush(Runnable callback) {
        callback.run();
    }

    @Override
    protected void onExtraCommand(int uid, int pid, String command, Bundle extras) {
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("StaticLocationProvider");
    }

    /**
     * Called by LocationControlService after config file is updated.
     */
    public void refreshLocation() {
        reportCurrentLocation();
    }

    private void reportCurrentLocation() {
        try {
            double latitude = mHalWrapper.getLatitude();
            double longitude = mHalWrapper.getLongitude();

            Location location = new Location(PROVIDER_NAME);
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            location.setAccuracy(DEFAULT_ACCURACY_METERS);
            location.setTime(System.currentTimeMillis());
            location.setElapsedRealtimeNanos(android.os.SystemClock.elapsedRealtimeNanos());

            try {
                LocationResult result = LocationResult.wrap(
                        Collections.singletonList(location)).validate();
                if (result != null) {
                    reportLocation(result);
                }
            } catch (BadLocationException e) {
                Log.w(TAG, "Invalid location from static provider", e);
            }
        } catch (ServiceSpecificException e) {
            Log.e(TAG, "Static location HAL reported a service-specific failure", e);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get location from static provider", e);
        }
    }
}
