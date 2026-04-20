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

package com.android.server.location;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;

import vendor.intel.location.ILocationHal;

/**
 * Wrapper for binder communication with vendor location HAL.
 *
 * @hide
 */
public final class LocationHalWrapper {

    private static final String HAL_SERVICE = "vendor.intel.location.ILocationHal/default";

    private ILocationHal mHal;

    /**
     * Gets latitude from vendor HAL.
     */
    public double getLatitude() throws RemoteException {
        return getHal().getLatitude();
    }

    /**
     * Gets longitude from vendor HAL.
     */
    public double getLongitude() throws RemoteException {
        return getHal().getLongitude();
    }

    private ILocationHal getHal() throws RemoteException {
        if (mHal == null) {
            final IBinder binder = ServiceManager.waitForService(HAL_SERVICE);
            if (binder == null) {
                throw new RemoteException("LocationHal service not available: " + HAL_SERVICE);
            }

            mHal = ILocationHal.Stub.asInterface(binder);
            if (mHal == null) {
                throw new RemoteException("Failed to get ILocationHal interface");
            }
        }
        return mHal;
    }
}
