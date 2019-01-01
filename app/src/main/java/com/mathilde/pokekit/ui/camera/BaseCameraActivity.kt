package com.mathilde.pokekit.ui.camera

import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v7.app.AppCompatActivity
import com.mathilde.pokekit.R
import com.otaliastudios.cameraview.Gesture
import com.otaliastudios.cameraview.GestureAction
import kotlinx.android.synthetic.main.main_activity.*

/**
 * @author mathilde
 * @version 01/01/2019
 */
abstract class BaseCameraActivity : AppCompatActivity() {
    lateinit var sheetBehavior: BottomSheetBehavior<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        sheetBehavior = BottomSheetBehavior.from(bottomLayout)
        sheetBehavior.isHideable = true
        cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM)
//        cameraFrame.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        cameraView.start()
    }

    override fun onPause() {
        cameraView.stop()
        super.onPause()
    }
}