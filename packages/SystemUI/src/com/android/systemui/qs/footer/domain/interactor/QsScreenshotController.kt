/* Copyright (C) 2026 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.qs.footer.domain.interactor

import android.content.Context
import android.os.Handler
import android.view.WindowManager.ScreenshotSource.SCREENSHOT_OTHER
import com.android.internal.util.ScreenshotHelper
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.shade.ShadeController
import javax.inject.Inject

/** Zendroid QS footer: tap captures the app; hold captures the expanded shade. */
@SysUISingleton
class QsScreenshotController
@Inject
constructor(
    @Application context: Context,
    @Main private val handler: Handler,
    private val shadeController: ShadeController,
) {
    private val helper = ScreenshotHelper(context)
    private var pending = false
    private var generation = 0
    private val finish = Runnable { pending = false }
    private val capture = Runnable {
        if (pending) {
            val request = generation
            helper.takeScreenshot(SCREENSHOT_OTHER, handler) {
                handler.post {
                    if (request == generation) {
                        handler.removeCallbacks(finish)
                        pending = false
                    }
                }
            }
        }
    }

    fun requestScreenshot(collapseShade: Boolean) {
        if (pending) return
        pending = true
        val request = ++generation
        handler.postDelayed(finish, 10_000L)
        if (collapseShade) {
            shadeController.addPostCollapseAction {
                if (pending && request == generation) handler.postDelayed(capture, 480L)
            }
            shadeController.collapseShade(true)
        } else {
            handler.postDelayed(capture, 80L)
        }
    }
}
