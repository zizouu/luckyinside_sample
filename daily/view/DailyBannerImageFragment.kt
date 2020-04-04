package com.lhbros.luckyinside.daily.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.jakewharton.rxbinding3.view.clicks
import com.lhbros.luckyinside.R
import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.util.AndroidUtil
import com.lhbros.luckyinside.daily.model.DailyBanner
import com.lhbros.luckyinside.map.view.MapBaseFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_daily_bannerimage.*

class DailyBannerImageFragment(

) : MapBaseFragment() {
    companion object {
        const val TAG = "dailyBannerImageFragment"
    }

    private var banner: DailyBanner? = null
    private var scrollViewScrollPosition = 0

    private val error = PublishSubject.create<Boolean>()
    private val message = PublishSubject.create<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        banner = arguments?.getSerializable(LuckyInsideConstant.KEY_DAILY_BANNER) as DailyBanner
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_daily_bannerimage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUi()
        subscribeUi()
        subscribeRx()
    }

    private fun initializeUi() {
        setStatusBarMargin(imageButtonBack)
        setStatusBarMargin(textViewTitle)
        setStatusBarMargin(imageViewPopHeader)
        setStatusBarMargin(imageButtonPopHeaderBack)
        setStatusBarMargin(textViewPopHeaderTitle)
        showImages()
    }

    private fun subscribeUi() {
        disposables.add(
            imageButtonBack.clicks()
                .subscribe({
                    activity?.onBackPressed()
                }) {
                    error.onNext(true)
                }
        )
        disposables.add(
            imageButtonPopHeaderBack.clicks()
                .subscribe({
                    activity?.onBackPressed()
                }) {
                    error.onNext(true)
                }
        )
    }

    private fun subscribeRx() {
        disposables.add(
            error
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showToast(getString(R.string.all_error))
                }) {
                    showToast(getString(R.string.all_error))
                }
        )
        disposables.add(
            message
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showToast(it)
                }) {
                    error.onNext(true)
                }
        )
    }

    private fun showImages() {
        val images = banner?.imageList
        if (images.isNullOrEmpty()) {
            error.onNext(true)
            return
        }
        textViewTitle.text = banner?.title
        textViewPopHeaderTitle.text = banner?.title
        for (dailyBannerImage in images) {
            val imageUrl = dailyBannerImage.imageUrl
            imageUrl ?: continue
            val view = ImageView(context)
            view.adjustViewBounds = true
            Glide.with(this)
                .load(dailyBannerImage.imageUrl)
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true))
                .into(view)
            containerScrollView.addView(view)
        }

        val alphaStartPosition = 0
        val alphaEndPosition = AndroidUtil.getDeviceHeight(context!!) / 3

        nestedScrollView.setOnScrollChangeListener(object : NestedScrollView.OnScrollChangeListener {
            override fun onScrollChange(
                v: NestedScrollView?,
                scrollX: Int,
                scrollY: Int,
                oldScrollX: Int,
                oldScrollY: Int
            ) {
                scrollViewScrollPosition += (scrollY - oldScrollY)
                val alphaTotalDiff = (alphaEndPosition - alphaStartPosition).toFloat()
                val currentAlphaDiff = (scrollViewScrollPosition - alphaStartPosition).toFloat()
                val alphaRatio = currentAlphaDiff / alphaTotalDiff
                if (scrollViewScrollPosition > alphaStartPosition && scrollViewScrollPosition < alphaEndPosition) {
                    containerPopHeader.alpha = alphaRatio
                } else if (scrollViewScrollPosition < alphaStartPosition || scrollViewScrollPosition == alphaStartPosition) {
                    containerPopHeader.alpha = 0f
                } else if (scrollViewScrollPosition > alphaEndPosition || scrollViewScrollPosition == alphaEndPosition) {
                    containerPopHeader.alpha = 1f
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}