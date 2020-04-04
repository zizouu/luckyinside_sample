package com.lhbros.luckyinside.daily.view

import android.animation.Animator
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxbinding3.view.clicks
import com.lhbros.luckyinside.R
import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.constant.FirebaseAnalyticsConst
import com.lhbros.luckyinside.common.util.AndroidUtil
import com.lhbros.luckyinside.common.util.AnimationUtil
import com.lhbros.luckyinside.common.util.UiUtil
import com.lhbros.luckyinside.common.view.CustomViewPagerScroller
import com.lhbros.luckyinside.daily.model.DailyBanner
import com.lhbros.luckyinside.daily.model.DailyVenue
import com.lhbros.luckyinside.map.model.*
import com.lhbros.luckyinside.map.view.MapBaseFragment
import com.lhbros.luckyinside.map.view.MapActivity
import com.lhbros.luckyinside.daily.view.adapter.DailyBannerAdapter
import com.lhbros.luckyinside.daily.view.adapter.DailyVenueAdapter
import com.lhbros.luckyinside.common.view.decoration.RecyclerViewGridDecoration
import com.lhbros.luckyinside.daily.view.listener.OnDailyBannerClickListener
import com.lhbros.luckyinside.daily.view.listener.OnDailyVenueClickListener
import com.lhbros.luckyinside.daily.viewmodel.DailyViewModel
import com.lhbros.luckyinside.daily.viewmodel.DailyViewModelFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_daily.*
import kotlinx.android.synthetic.main.fragment_daily.indicator
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.absoluteValue

class DailyFragment :
    MapBaseFragment(),
    OnDailyVenueClickListener,
    OnDailyBannerClickListener,
    SwipeRefreshLayout.OnRefreshListener
{
    companion object {
        const val TAG = "curatingFragment"
        const val PAGING_DURATION = 5000L
    }

    @Inject
    lateinit var viewModelFactory: DailyViewModelFactory
    private lateinit var viewModel: DailyViewModel
    private var viewPagerHandler: Handler? = null
    private var dailyVenueAdapter: DailyVenueAdapter? = null
    private var dailyBannerAdapter: DailyBannerAdapter? = null
    private var currentBannerPosition = 0
    private var deviceHeight = 0
    private var appbarHeight = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory)[DailyViewModel::class.java]
        deviceHeight = arguments?.getInt(LuckyInsideConstant.KEY_DEVICE_HEIGHT) ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_daily, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.selectedRegionIndex = getSavedSelectedRegionIndex()
        initializeUi()
        subscribeUi()
        subscribeViewModel()
        requestData()
    }

    private fun initializeUi() {
        setAppbarHeight()
        setToolbarMargin()
        setMenuButtonMargin()
        setScrollTopButton()
        initializeRecyclerView()
        initializeRefreshView()
        animateActionButtonScale(imageButtonMap)
    }

    private fun subscribeUi() {
        disposables.add(
            imageButtonMap.clicks()
                .subscribe({
                    AndroidUtil.popBackStackFragment(activity!!, this)
                    requestListener.onRequestRegionSearch(viewModel.selectedRegionIndex)
                }) {
                    viewModel.error.onNext(true)
                }
        )
        disposables.add(
            imageButtonToolbarMenu.clicks()
                .subscribe({
                    requestListener.onRequestMenu()
                }) {
                    viewModel.error.onNext(true)
                }
        )
        disposables.add(
            imageButtonScrollTop.clicks()
                .subscribe({
                    recyclerView.scrollToPosition(0)
                    appbarLayout.setExpanded(true)
                }) {
                    viewModel.error.onNext(true)
                }
        )
    }

    private fun subscribeViewModel() {
        disposables.add(
            viewModel.isUnAuthorizedToken
                .subscribe({
                    tokenUnAuthorizedListener.onTokenUnAuthorized()
                }) {
                    viewModel.message.onNext("${it.message}")
                }
        )
        disposables.add(
            viewModel.message
                .subscribe({
                    showToast(it)
                }) {
                    viewModel.error.onNext(true)
                }
        )
        disposables.add(
            viewModel.error
                .subscribe({
                    swipeRefreshLayout.isRefreshing = false
                    showToast(getString(R.string.all_error))
                }) {
                    showToast(getString(R.string.all_error))
                }
        )
        disposables.add(
            viewModel.banners
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    initializeBanner(it.toMutableList())
                }) {
                    viewModel.error.onNext(true)
                }
        )
        disposables.add(
            viewModel.venues
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    swipeRefreshLayout.isRefreshing = false
                    showDailyVenues(it)
                }) {
                    swipeRefreshLayout.isRefreshing = false
                    viewModel.error.onNext(true)
                }
        )
        disposables.add(
            viewModel.regions
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showDailyRegions(it)
                }) {
                    viewModel.error.onNext(true)
                }
        )
    }

    private fun requestData() {
        disposables.add(viewModel.requestBanners())
        disposables.add(viewModel.requestDailyRegions())
    }

    private fun setAppbarHeight() {
        appbarHeight = (deviceHeight * 0.79).toInt()
        if (appbarHeight == 0) return
        val params = appbarLayout.layoutParams as CoordinatorLayout.LayoutParams
        params.height = appbarHeight
    }

    private fun setScrollTopButton() {
        val toolbarHeight = resources.getDimension(R.dimen.daily_toolbar_height)
        val toolbarMarginBottom =  resources.getDimension(R.dimen.daily_toolbar_marginbottom)
        val statusbarHeight = AndroidUtil.getStatusBarHeight(context!!)
        val pinHeaderHeight = toolbarHeight + toolbarMarginBottom + statusbarHeight
        val topButtonDelta = resources.getDimension(R.dimen.daily_scrolltop_button_translationz)

        val maxToolbarPosition = if (appbarHeight == 0) resources.getDimension(R.dimen.daily_parallax_height) else appbarHeight.toFloat()
        val minToolbarPosition = maxToolbarPosition - pinHeaderHeight

        appbarLayout.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            override fun onOffsetChanged(p0: AppBarLayout?, p1: Int) {
                val offset = p1.absoluteValue
                if (offset < maxToolbarPosition && offset > minToolbarPosition) {
                    val delta = offset - minToolbarPosition
                    val ratio = delta / pinHeaderHeight
                    imageButtonScrollTop?.translationY = UiUtil.dpToPx(context!!, ratio * topButtonDelta).toFloat()
                } else if (offset < minToolbarPosition) {
                    imageButtonScrollTop?.visibility = View.INVISIBLE
                    imageButtonScrollTop?.translationY = UiUtil.dpToPx(context!!, topButtonDelta).toFloat()
                } else if (offset.toFloat() == maxToolbarPosition) {
                    imageButtonScrollTop?.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun setToolbarMargin() {
        val params = toolbar.layoutParams as FrameLayout.LayoutParams
        params.topMargin = AndroidUtil.getStatusBarHeight(context!!)
    }

    private fun setMenuButtonMargin() {
        val params = imageButtonToolbarMenu.layoutParams as CoordinatorLayout.LayoutParams
        params.topMargin += AndroidUtil.getStatusBarHeight(context!!)
    }

    private fun initializeRecyclerView() {
        dailyVenueAdapter = DailyVenueAdapter(this)
        val gridLayoutManager = GridLayoutManager(context!!, 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == 0 || position == 1 || position == 2) 2 else 1
            }
        }
        recyclerView.layoutManager = gridLayoutManager
        recyclerView.adapter = dailyVenueAdapter
        recyclerView.itemAnimator = null
        recyclerView.addItemDecoration(
            RecyclerViewGridDecoration(
                resources.getDimension(
                    R.dimen.luckyu_main_tobeopend_divide_height
                ).toInt(),
                0
            )
        )
    }

    private fun initializeBanner(banners: MutableList<DailyBanner>) {
        setViewPagerScroller()

        viewPagerHandler = Handler()
        dailyBannerAdapter =
            DailyBannerAdapter(context, banners, this)
        viewPagerBanner.adapter = dailyBannerAdapter
        indicator.setViewPager(viewPagerBanner)

        viewPagerBanner.addOnPageChangeListener(object : ViewPager.OnPageChangeListener{
            override fun onPageScrollStateChanged(state: Int) { }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) { }
            override fun onPageSelected(position: Int) {
                viewPagerHandler?.removeCallbacksAndMessages(null)
                currentBannerPosition = position
                postViewPagerHandler()
            }
        })

        val viewCount = dailyBannerAdapter?.count
        viewCount ?: return

        if (viewCount > 1) {
            postViewPagerHandler()
        }
    }

    private fun setViewPagerScroller() {
        try {
            val scroller = ViewPager::class.java.getDeclaredField("mScroller")
            scroller.isAccessible = true
            val customScroller = CustomViewPagerScroller(context!!)
            customScroller.customDuration = 1000
            scroller.set(viewPagerBanner, customScroller)
        } catch (e: NoSuchFieldException) {

        } catch (e: IllegalArgumentException) {

        } catch (e: IllegalAccessException) {

        }
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

    private fun postViewPagerHandler() {
        viewPagerHandler?.postDelayed({
            showNextImage()
        }, PAGING_DURATION)
    }

    private fun showNextImage() {
        val curPos = viewPagerBanner?.currentItem
        if (curPos == null) {
            viewPagerHandler?.removeCallbacksAndMessages(null)
            viewPagerHandler = null
            return
        }
        val viewCount = dailyBannerAdapter?.count
        viewCount ?: return
        val nextPos = if (curPos == viewCount - 1) 0 else curPos + 1
        viewPagerBanner.setCurrentItem(nextPos, false)
    }

    private fun showDailyVenues(venues: List<DailyVenue>) {
        val freeExperienceVenues = venues.filter { it.openYn == LuckyInsideConstant.COMMON_YN_Y }
        val hotPlaceVenues = venues.filter { it.openYn == LuckyInsideConstant.COMMON_YN_N }
        // 핫플레이스와 지금 무료경험 없을 경우 레이아웃 변경
        val gridLayoutManager = GridLayoutManager(context!!, 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                if (freeExperienceVenues.isNullOrEmpty()){
                    return if (position == 0) 2 else 1
                } else if (hotPlaceVenues.isNullOrEmpty()) {
                    return if (position == 0 || position == 1) 2 else 1
                } else {
                    return if (position == 0 || position == 1 || position == 2) 2 else 1
                }
            }
        }
        recyclerView.layoutManager = gridLayoutManager
        dailyVenueAdapter?.setData(venues)
    }

    private fun showDailyRegions(regions: List<Region>) {
        tabLayoutRegion.removeAllTabs()
        for (region in regions) {
            tabLayoutRegion.addTab(tabLayoutRegion.newTab().setText("${region.regionName}").setTag(region.regionIndex))
        }
        tabLayoutRegion.clearOnTabSelectedListeners()
        tabLayoutRegion.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) { }
            override fun onTabUnselected(p0: TabLayout.Tab?) { }
            override fun onTabSelected(p0: TabLayout.Tab?) {
                p0 ?: return
                val regionIndex = p0.tag as Int
                viewModel.selectedRegionIndex = regionIndex
                disposables.add(viewModel.requestDailyVenues(regionIndex))
            }
        })
        setSavedRegion(viewModel.selectedRegionIndex)
        requestCurrentRegionVenues()
    }

    /**
     * 전에 보던 지역이 있다면 그 지역으로 select
     */
    private fun setSavedRegion(regionIndex: Int?) {
        regionIndex ?: return
        val count = tabLayoutRegion?.tabCount
        count ?: return
        for (index in 0 until count) {
            val regionTab = tabLayoutRegion?.getTabAt(index)
            if (regionTab?.tag == regionIndex) {
                regionTab.select()
                break
            }
        }
    }

    private fun getCurrentSelectedRegionIndex(): Int? {
        return tabLayoutRegion?.getTabAt(tabLayoutRegion.selectedTabPosition)?.tag as? Int?
    }

    private fun requestCurrentRegionVenues() {
        val regionIndex = getCurrentSelectedRegionIndex()
        regionIndex ?: return
        viewModel.selectedRegionIndex = regionIndex
        disposables.add(viewModel.requestDailyVenues(regionIndex))
    }

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
        val animSet = AnimationUtil.getPopupAnimatorSet(containerMyVenuePopup)
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

    private fun saveSelectedRegionIndex() {
        (activity as? MapActivity?)?.selectedRegionIndex = viewModel.selectedRegionIndex
    }

    private fun getSavedSelectedRegionIndex(): Int? {
        return (activity as? MapActivity?)?.selectedRegionIndex
    }

    override fun onRefresh() {
        requestCurrentRegionVenues()
    }

    override fun onDailyVenueClick(type: String?, index: Int?) {
        type ?: return
        index ?: return
        when (type) {
            LuckyInsideConstant.DAILY_VENUE_TYPE_STORE -> requestListener.onRequestStoreDetail(index, isAdd = true)
            LuckyInsideConstant.DAILY_VENUE_TYPE_LUCKYU -> requestListener.onRequestLuckyUDetail(index, isAdd = true, isSlideAnim = true)
        }
    }

    override fun onDailyMyVenueClick(type: String?, index: Int?, isRegist: Boolean) {
        type ?: return
        index ?: return
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

    override fun onDailyBannerClick(banner: DailyBanner) {
        when (banner.type) {
            DailyBanner.TYPE_DRAW -> {
                val bundle = Bundle().apply {
                    putString(FirebaseAnalyticsConst.PARAM_GETLUCKY_ENTER_TYPE, FirebaseAnalyticsConst.VALUE_BANNER_GETLUCKY)
                }
                viewModel.requestLogAnalytics(FirebaseAnalyticsConst.EVENT_ENTER_GETLUCKY, bundle)
                requestListener.onRequestGetProduct(banner.luckyDrawIndex)
            }
            DailyBanner.TYPE_STORE -> requestListener.onRequestStoreDetail(banner.storeIndex)
            DailyBanner.TYPE_LUCKYU -> requestListener.onRequestLuckyUDetail(banner.luckyuIndex, isSlideAnim = true)
            DailyBanner.TYPE_IMAGE -> requestListener.onRequestDailyBannerImage(banner)
        }
    }

    override fun onDailyFreeExperienceSeeAll() {
        val regionIndex = getCurrentSelectedRegionIndex()
        regionIndex ?: return
        requestListener.onRequestDailyVenueList(regionIndex)
    }

    override fun onDestroyView() {
        viewPagerHandler?.removeCallbacksAndMessages(null)
        viewPagerHandler = null
        super.onDestroyView()
        saveSelectedRegionIndex()
    }
}