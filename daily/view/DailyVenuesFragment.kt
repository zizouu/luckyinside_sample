package com.lhbros.luckyinside.daily.view

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.jakewharton.rxbinding3.view.clicks
import com.lhbros.luckyinside.R
import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.util.UiUtil
import com.lhbros.luckyinside.map.view.MapBaseFragment
import com.lhbros.luckyinside.daily.view.adapter.DailyVenueListAdapter
import com.lhbros.luckyinside.common.view.decoration.RecyclerViewGridDecoration
import com.lhbros.luckyinside.daily.view.listener.OnDailyVenueClickListener
import com.lhbros.luckyinside.daily.viewmodel.DailyVenueListViewModel
import com.lhbros.luckyinside.daily.viewmodel.DailyVenueListViewModelFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_daily_venues.*
import timber.log.Timber
import javax.inject.Inject

class DailyVenuesFragment : MapBaseFragment(),
    OnDailyVenueClickListener, SwipeRefreshLayout.OnRefreshListener {
    companion object {
        const val TAG = "dailyVenueListFragment"
    }

    @Inject
    lateinit var viewModelFactory: DailyVenueListViewModelFactory
    lateinit var viewModel: DailyVenueListViewModel
    private var adapter: DailyVenueListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory)[DailyVenueListViewModel::class.java]
        viewModel.regionIndex = arguments?.getInt(LuckyInsideConstant.KEY_REGION_INDEX)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_daily_venues, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUi()
        subscribeUi()
        subscribeViewModel()
        requestData()
    }

    private fun initializeUi() {
        setStatusBarMargin(containerHeader)
        setStatusBarMargin(viewHeader)
        initializeRefreshView()
        initializeRecyclerView()
    }

    private fun subscribeUi() {
        disposables.add(
            imageViewBack.clicks()
                .subscribe({
                    activity?.onBackPressed()
                }) {
                    viewModel.error.onNext(true)
                }
        )
    }

    private fun subscribeViewModel() {
        disposables.add(
            viewModel.message
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showToast(it)
                }) {
                    viewModel.error.onNext(true)
                }
        )
        disposables.add(
            viewModel.error
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    swipeRefreshLayout.isRefreshing = false
                    showToast(getString(R.string.all_error))
                }) {
                    swipeRefreshLayout.isRefreshing = false
                    showToast(getString(R.string.all_error))
                }
        )
        disposables.add(
            viewModel.isUnAuthorizedToken
                .subscribe({
                    if (it) {
                        tokenUnAuthorizedListener.onTokenUnAuthorized()
                    }
                }) {
                    viewModel.error.onNext(true)
                }
        )
        disposables.add(
            viewModel.venues
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    adapter?.venues = it
                    swipeRefreshLayout.isRefreshing = false
                }) {
                    viewModel.error.onNext(true)
                    swipeRefreshLayout.isRefreshing = false
                }
        )
    }

    private fun requestData() {
        val regionIndex = viewModel.regionIndex
        if (regionIndex == null) {
            viewModel.error.onNext(true)
            swipeRefreshLayout.isRefreshing = false
            return
        }
        disposables.add(viewModel.requestFreeExperienceVenues(regionIndex))
    }

    private fun initializeRecyclerView() {
        adapter = DailyVenueListAdapter(this)
        val gridLayoutManager = GridLayoutManager(context!!, 2)
        recyclerView.layoutManager = gridLayoutManager
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(
            RecyclerViewGridDecoration(
                UiUtil.dpToPx(context!!, 16f),
                0,
                isAllSide = false
            )
        )
    }

    private fun initializeRefreshView() {
        try {
            val field = swipeRefreshLayout.javaClass.getDeclaredField("mCircleView")
            field.isAccessible = true
            val imageView = field.get(swipeRefreshLayout) as ImageView

            Glide.with(this)
                .asGif()
                .load(R.drawable.ic_loading)
                .into(imageView)
        } catch (e: NoSuchFieldException) {
            Timber.d("${e.message}")
        } catch (e: IllegalArgumentException) {
            Timber.d("${e.message}")
        }
        swipeRefreshLayout.setOnRefreshListener(this)
    }

    ///////////////////////// my venue Animation /////////////////////////

    private fun showMyVenuePopup(type: String, index: Int, isRegist: Boolean) {
        containerMyVenuePopup.pivotX = containerMyVenuePopup.measuredWidth.toFloat() / 2
        containerMyVenuePopup.pivotY = containerMyVenuePopup.measuredHeight.toFloat() / 2
        if (isRegist) {
            imageViewMyVenuePopup.setImageResource(R.drawable.ic_mystore_popup_green)
            textViewMyVenuePopup.text = getString(R.string.storedetail_mystore_regist_success)
        } else {
            imageViewMyVenuePopup.setImageResource(R.drawable.ic_mystore_popup_white)
            textViewMyVenuePopup.text = getString(R.string.storedetail_mystore_delete_success)
        }
        containerMyVenuePopup.visibility = View.VISIBLE
        animateMyVenuePopup()
    }

    private fun animateMyVenuePopup() {
        val animSet = getPopupAnimatorSet(containerMyVenuePopup)
        animSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationStart(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                containerMyVenuePopup?.visibility = View.INVISIBLE
                containerMyVenuePopup?.alpha = 1f
            }
        })
        animSet.start()
    }

    private fun getPopupAnimatorSet(target: View) : AnimatorSet {
        val propertyScaleX = "scaleX"
        val propertyScaleY = "scaleY"
        val propertyAlpha = "alpha"
        val firstAnimDuration = 200L
        val secondAnimDuration = 200L
        val thirdAnimDuration = 500L

        val firstScaleXAnim = getObjectAnimator(target, 1.2f, propertyScaleX, firstAnimDuration)
        val firstScaleYAnim = getObjectAnimator(target, 1.2f, propertyScaleY, firstAnimDuration)
        val secondScaleXAnim = getObjectAnimator(target, 1.0f, propertyScaleX, secondAnimDuration)
        val secondScaleYAnim = getObjectAnimator(target, 1.0f, propertyScaleY, secondAnimDuration)
        val thirdScaleXAnim = getObjectAnimator(target, 0.0f, propertyScaleX, thirdAnimDuration)
        val thirdScaleYAnim = getObjectAnimator(target, 0.0f, propertyScaleY, thirdAnimDuration)
        val fourthAlphaAnim = ObjectAnimator.ofFloat(target, propertyAlpha, 1f, 0f)

        val animSet = AnimatorSet()
        animSet.apply {
            play(firstScaleXAnim).with(firstScaleYAnim)
            play(secondScaleXAnim).with(secondScaleYAnim).after(firstScaleXAnim)
            play(thirdScaleXAnim).with(thirdScaleYAnim).after(secondScaleXAnim).after(1000)
            play(fourthAlphaAnim).with(thirdScaleXAnim)
        }
        return animSet
    }

    private fun getObjectAnimator(target: View, animFactor: Float, property: String, duration: Long) : ObjectAnimator {
        val animator = ObjectAnimator.ofFloat(target, property, animFactor)
        animator.duration = duration
        return animator
    }

    ///////////////////////// implementation /////////////////////////

    override fun onRefresh() {
        requestData()
    }

    override fun onDailyVenueClick(type: String?, index: Int?) {
        if (type.isNullOrBlank() || index == null) {
            viewModel.error.onNext(true)
            return
        }
        when (type) {
            LuckyInsideConstant.DAILY_VENUE_TYPE_STORE -> requestListener.onRequestStoreDetail(index)
            LuckyInsideConstant.DAILY_VENUE_TYPE_LUCKYU -> requestListener.onRequestLuckyUDetail(index)
        }
    }

    override fun onDailyMyVenueClick(type: String?, index: Int?, isRegist: Boolean) {
        if (type.isNullOrBlank() || index == null) {
            viewModel.error.onNext(true)
            return
        }
        if (isRegist) {
            if (type == LuckyInsideConstant.DAILY_VENUE_TYPE_STORE) {
                disposables.add(
                    viewModel.requestRegistMyStore(index)
                )
            } else if (type == LuckyInsideConstant.DAILY_VENUE_TYPE_LUCKYU) {
                disposables.add(
                    viewModel.requestRegistMyLuckyU(index)
                )
            }
        } else {
            if (type == LuckyInsideConstant.DAILY_VENUE_TYPE_STORE) {
                disposables.add(
                    viewModel.requestDeleteMyStore(index)
                )
            } else if (type == LuckyInsideConstant.DAILY_VENUE_TYPE_LUCKYU) {
                disposables.add(
                    viewModel.requestDeleteMyLuckyU(index)
                )
            }
        }
        showMyVenuePopup(type, index, isRegist)
    }

    override fun onDailyFreeExperienceSeeAll() {
        // not use here
    }
}