package com.lhbros.luckyinside.map.view

import android.animation.AnimatorSet
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.lhbros.luckyinside.common.util.AndroidUtil
import com.lhbros.luckyinside.common.util.AnimationUtil
import com.lhbros.luckyinside.common.view.BaseDaggerFragment
import com.lhbros.luckyinside.common.view.listener.OnTokenUnAuthorizedListener
import com.lhbros.luckyinside.map.view.listener.OnMapDataChangeListener
import com.lhbros.luckyinside.map.view.listener.OnRequestViewListener
import com.lhbros.luckyinside.map.view.listener.OnTutorialListener

open class MapBaseFragment : BaseDaggerFragment(){
    lateinit var requestListener: OnRequestViewListener
    lateinit var tokenUnAuthorizedListener: OnTokenUnAuthorizedListener
    lateinit var dataChangeListener: OnMapDataChangeListener
    lateinit var tutorialListener: OnTutorialListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestListener = activity as OnRequestViewListener
        dataChangeListener = activity as OnMapDataChangeListener
        tokenUnAuthorizedListener = activity as OnTokenUnAuthorizedListener
        tutorialListener = activity as OnTutorialListener
    }

    fun setStatusBarMargin(target: View) {
        val layoutParams = target.layoutParams
        if (layoutParams is ConstraintLayout.LayoutParams) {
            layoutParams.topMargin += AndroidUtil.getStatusBarHeight(context!!)
        }
    }

    fun isLocatedInRegion(regionIndex: Int?) = (activity as MapActivity).isLocatedInRegion(regionIndex)

    fun getCurrentUserLatLng() = (activity as MapActivity).getCurrentLatLng()
}