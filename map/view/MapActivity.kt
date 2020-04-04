package com.lhbros.luckyinside.map.view

import android.animation.AnimatorSet
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxbinding3.view.clicks
import com.lhbros.luckyinside.R
import com.lhbros.luckyinside.ar.view.ArActivity
import com.lhbros.luckyinside.ar.view.ArFinishFragment
import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.constant.FirebaseAnalyticsConst
import com.lhbros.luckyinside.common.util.AndroidUtil
import com.lhbros.luckyinside.common.util.AnimationUtil
import com.lhbros.luckyinside.common.util.DataUtil
import com.lhbros.luckyinside.common.util.UiUtil
import com.lhbros.luckyinside.common.view.BaseDaggerActivity
import com.lhbros.luckyinside.help.view.HelpFragment
import com.lhbros.luckyinside.common.view.listener.OnTokenUnAuthorizedListener
import com.lhbros.luckyinside.daily.model.DailyBanner
import com.lhbros.luckyinside.daily.view.DailyBannerImageFragment
import com.lhbros.luckyinside.daily.view.DailyFragment
import com.lhbros.luckyinside.daily.view.DailyVenuesFragment
import com.lhbros.luckyinside.launch.model.Landing
import com.lhbros.luckyinside.luckydraw.view.LuckyDrawDetailFragment
import com.lhbros.luckyinside.luckydraw.view.LuckyDrawFinishFragment
import com.lhbros.luckyinside.luckydraw.view.LuckyDrawFragment
import com.lhbros.luckyinside.luckyu.model.LuckyU
import com.lhbros.luckyinside.luckyu.view.LuckyUDetailFragment
import com.lhbros.luckyinside.luckyu.view.LuckyUMainFragment
import com.lhbros.luckyinside.map.model.*
import com.lhbros.luckyinside.map.view.listener.OnMapDataChangeListener
import com.lhbros.luckyinside.map.view.listener.OnRequestViewListener
import com.lhbros.luckyinside.map.view.listener.OnTutorialListener
import com.lhbros.luckyinside.map.view.mapbox.MapPresenter
import com.lhbros.luckyinside.map.view.mapbox.MapboxMapPresenter
import com.lhbros.luckyinside.map.viewmodel.MapMainViewModel
import com.lhbros.luckyinside.map.viewmodel.MapMainViewModelFactory
import com.lhbros.luckyinside.menu.view.MenuFragment
import com.lhbros.luckyinside.myitem.model.MyItem
import com.lhbros.luckyinside.myitem.view.MyItemDetailFragment
import com.lhbros.luckyinside.myitem.view.MyItemFragment
import com.lhbros.luckyinside.mystore.view.MyStoreFragment
import com.lhbros.luckyinside.navigation.view.NavigationFragment
import com.lhbros.luckyinside.news.view.BoardDetailFragment
import com.lhbros.luckyinside.news.view.NewsFragment
import com.lhbros.luckyinside.popup.model.PopupNotice
import com.lhbros.luckyinside.popup.view.PopupNoticeFragment
import com.lhbros.luckyinside.setting.view.SettingActivity
import com.lhbros.luckyinside.share.model.ShareContent
import com.lhbros.luckyinside.signin.model.User
import com.lhbros.luckyinside.signin.view.main.SigninActivity
import com.lhbros.luckyinside.store.model.Store
import com.lhbros.luckyinside.store.view.StoreDetailFragment
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.bottomsheet_venue_preview.*
import timber.log.Timber
import java.lang.NumberFormatException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MapActivity :
    BaseDaggerActivity(),
    PermissionsListener,
    OnRequestViewListener,
    OnMapDataChangeListener,
    OnTokenUnAuthorizedListener,
    OnTutorialListener
{
    companion object {
        const val TAG = "mapMainActivity"
        private const val BACKBUTTON_PRESS_INTERVAL = 2000

        private val REQ_CODE_AR = 1000
        private val REQ_CODE_SETTING = 2000
    }

    @Inject
    lateinit var viewModelFactory: MapMainViewModelFactory
    var selectedRegionIndex: Int? = null
    var deviceHeight: Int? = null

    private lateinit var viewModel: MapMainViewModel
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var mapPresenter: MapPresenter


    private val isLocatedInAnyRegion: BehaviorSubject<Boolean> = BehaviorSubject.create()
    private val userImageRequest = PublishSubject.create<Boolean>()
    private var actionButtonAnimtorSet: AnimatorSet? = null
    private var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>? = null
    private var tutorialIndex: Int = 1
    private var backButtonPressTime = 0L

    @SuppressWarnings( "MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_map)

        viewModel = ViewModelProviders.of(this, viewModelFactory)[MapMainViewModel::class.java]

        val user = intent.getSerializableExtra(LuckyInsideConstant.KEY_USER) as User?
        if (user != null) viewModel.user = user

        mapPresenter = MapboxMapPresenter(
            mapView,
            getString(R.string.mapbox_style_url),
            this
        )

        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(mapPresenter as MapboxMapPresenter)

        initializeUi()
        subscribeUi()
        subscribeViewModel()
        subscribeMapPresenter()
    }

    private fun initializeUi() {
        val layoutParams = imageButtonMenu.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.topMargin += AndroidUtil.getStatusBarHeight(this)

        containerRoot.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                deviceHeight = containerRoot.height

                onRequestDaily()
                processLuckyDraw()

                val landing = intent?.getSerializableExtra(LuckyInsideConstant.KEY_LANDING) as Landing?
                if (shouldShowLanding(landing)) {
                    processLanding(landing)
                }

                viewModel.requestGetPopupNotice()

                initializeBottomSheet()
                containerRoot.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    private fun shouldShowLanding(landing: Landing?): Boolean {
        landing ?: return false

        val isLuckyDraw = landing.contentType == Landing.ContentType.LUCKYDRAW
        val isShowingLuckyDraw = viewModel.user.todayLuckyDrawGetYn == LuckyInsideConstant.COMMON_YN_N

        return !(isLuckyDraw && isShowingLuckyDraw)
    }

    private fun processLuckyDraw() {
        if (viewModel.user.todayLuckyDrawGetYn == LuckyInsideConstant.COMMON_YN_N) {
            val bundle = Bundle().apply {
                putString(FirebaseAnalyticsConst.PARAM_GETLUCKY_ENTER_TYPE, FirebaseAnalyticsConst.VALUE_LANDING_GETLUCKY)
            }
            viewModel.requestLogAnalytics(FirebaseAnalyticsConst.EVENT_ENTER_GETLUCKY, bundle)

            onRequestGetProduct(viewModel.user.todayLuckyDrawIndex)
        }
    }

    private fun processLanding(landing: Landing?) {
        landing ?: return

        val pushIndex = landing.pushIndex
        pushIndex ?: return

        try {
            when (landing.pushType) {
                Landing.PushType.NOTIFICATION -> viewDisposables.add(viewModel.requestReadNotificationPush(pushIndex.toInt()))
                Landing.PushType.REGULAR -> viewDisposables.add(viewModel.requestReadRegularPush(pushIndex.toInt()))
                Landing.PushType.NONE -> { /* nothing */ }
            }

            showLandingPage(landing)
        } catch (e: NumberFormatException) {
            Timber.d(e.message)
        }
    }

    private fun subscribeUi() {
        disposables.add(
            isLocatedInAnyRegion
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //toggleLocateSignal(!it)
                }){
                    viewModel.message.onNext("${it.message}")
                }
        )

        disposables.add(
            imageButtonDaily.clicks()
                .subscribe {
                    onRequestDaily()
                }
        )

        disposables.add(
            imageButtonMenu.clicks()
                .subscribe {
                    onRequestMenu()
                }
        )

        disposables.add(
            imageButtonRefresh.clicks()
                .filter { viewModel.isRefreshFinished != false }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    animateRefreshButton()
                    mapPresenter.refreshMapStart()
                    viewModel.requestRegions()
                }) {
                    viewModel.isRefreshError.onNext(true)
                    viewModel.message.onNext("${it.message}")
                }
        )

        disposables.add(
            imageButtonUserLocation.clicks()
                .subscribe({
                    if (getCurrentLatLng() == null) {
                        viewModel.message.onNext(getString(R.string.mapmain_checkgps))
                    } else {
                        mapPresenter.animateUserLocation()
                    }
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
                }){
                    showToast("${it.message}")
                }
        )

        disposables.add(
            viewModel.isUnAuthorizedToken
                .throttleFirst(LuckyInsideConstant.RX_THROTTLEFIRST_LONG_DURATION, TimeUnit.MILLISECONDS)
                .subscribe({
                    onTokenUnAuthorized()
                }) {
                    viewModel.message.onNext("${it.message}")
                }
        )

        disposables.add(
            viewModel.isRefreshError
                .subscribe({
                    stopRefreshButtonAnimation()
                    viewModel.isRefreshFinished = true
                }) {

                }
        )

        disposables.add(
            viewModel.regions
                .observeOn(Schedulers.io())
                .map {
                    setRegionImage(it)
                    it
                }
                .observeOn(AndroidSchedulers.mainThread())
                .map { mapPresenter.setRegions(it) }
                .observeOn(Schedulers.io())
                .subscribe({
                    viewModel.requestVenues()
                }) {
                    stopRefreshButtonAnimation()
                    viewModel.error.onNext(true)
                }
        )

        disposables.add(
            viewModel.venues
                .observeOn(Schedulers.io())
                .map {
                    setVenueImage()
                    it
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    mapPresenter.setVenues(it)
                    stopRefreshButtonAnimation()
                }) {
                    viewModel.error.onNext(true)
                }
        )

        disposables.add(
            viewModel.updateUser
                .subscribe({
                    val isProfileUpdate = it.profileImageUrl != viewModel.user.profileImageUrl
                    viewModel.user = it
                    if (isProfileUpdate) {
                        userImageRequest.onNext(true)
                    }
                }) {
                    viewModel.error.onNext(true)
                }
        )

        disposables.add(
            viewModel.popupNotice
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    onRequestPopupNotice(it)
                }) {
                    // nothing
                }
        )
    }

    private fun subscribeMapPresenter() {
        disposables.add(
            mapPresenter.mapReady
                .observeOn(AndroidSchedulers.mainThread())
                .map { enableLocationComponent() }
                .observeOn(Schedulers.io())
                .subscribe({
                    userImageRequest.onNext(true)
                    viewModel.requestRegions()
                }) {
                    viewModel.error.onNext(true)
                }
        )

        disposables.add(
            mapPresenter.venueMarkerClicked
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showVenuePreviewBottomSheet(it)
                }) {
                    viewModel.error.onNext(true)
                }
        )

        disposables.add(
            mapPresenter.mapClicked
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    hideVenuePreviewBottomSheet()
                }) {
                    viewModel.error.onNext(true)
                }
        )

        disposables.add(
            userImageRequest
                .map { viewModel.user.profileImageUrl ?: "" }
                .observeOn(Schedulers.io())
                .map {
                    try {
                        val size = UiUtil.dpToPx(this, resources.getDimension(R.dimen.map_profile_size))
                        UiUtil.getBitmapByGlide(this, it, size)
                    } catch (e: Exception) {
                        ""
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it is Bitmap) {
                        mapPresenter.addUserImage(it)
                    }
                }) {
                    viewModel.error.onNext(true)
                }
        )

        disposables.add(
            mapPresenter.regionClicked
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    processTutorial()
                }) {
                    viewModel.error.onNext(true)
                }
        )
    }

    private fun setVenueImage() {
        val markerWidth = resources.getDimension(R.dimen.map_storemarker_width).toInt()
        val markerHeight = resources.getDimension(R.dimen.map_storemarker_height).toInt()
        val clickedMarkerWidth = (markerWidth * 1.25).toInt()
        val clickedMarkerHeight = (markerHeight * 1.25).toInt()

        mapPresenter.activeMarker =
            Glide.with(this)
                .asBitmap()
                .load(R.drawable.ic_marker_active)
                .apply(RequestOptions().skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE))
                .submit(markerWidth, markerHeight)
                .get()

        mapPresenter.inactiveMarker =
            Glide.with(this)
                .asBitmap()
                .load(R.drawable.ic_marker_inactive)
                .apply(RequestOptions().skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE))
                .submit(markerWidth, markerHeight)
                .get()

        mapPresenter.clickedActiveMarker =
            Glide.with(this)
                .asBitmap()
                .load(R.drawable.ic_marker_active)
                .apply(RequestOptions().skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE))
                .submit(clickedMarkerWidth, clickedMarkerHeight)
                .get()

        mapPresenter.clickedInactiveMarker =
            Glide.with(this)
                .asBitmap()
                .load(R.drawable.ic_marker_inactive)
                .apply(RequestOptions().skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE))
                .submit(clickedMarkerWidth, clickedMarkerHeight)
                .get()
    }

    private fun setRegionImage(regions: List<Region>) {
        val width = resources.getDimension(R.dimen.map_region_marker_size).toInt()
        val height = resources.getDimension(R.dimen.map_region_marker_size).toInt()
        for (region in regions) {
            val imageUrl = region.representativeImageUrl
            if (imageUrl.isNullOrBlank()) continue
            region.representativeImageBitmap =
                Glide.with(this)
                    .asBitmap()
                    .load(imageUrl)
                    .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true))
                    .submit(width, height)
                    .get()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val landing = intent?.getSerializableExtra(LuckyInsideConstant.KEY_LANDING) as Landing?
        processLanding(landing)
    }

    private fun animateRefreshButton() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.map_refresh_rotate)
        imageButtonRefresh.startAnimation(anim)
    }

    private fun stopRefreshButtonAnimation() {
        imageButtonRefresh.clearAnimation()
    }

    private fun initializeBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetRoot)
        bottomSheetBehavior?.skipCollapsed = true
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback(){
            override fun onSlide(p0: View, p1: Float) { }
            override fun onStateChanged(p0: View, p1: Int) {
                if (p1 == BottomSheetBehavior.STATE_HIDDEN) {
                    containerBottomSheet.visibility = View.INVISIBLE
                    if (containerActionButton.translationY != 0f) {
                        setBottomSheetContentMargin(0f)
                    }
                    mapPresenter.setSelectedMarkerUnselected()
                }
            }
        })
    }

    private fun setBottomSheetContentMargin(margin: Float) {
        containerActionButton.animate().translationY(UiUtil.dpToPx(this, margin).toFloat())
    }

    private fun showVenuePreviewBottomSheet(venue: Venue) {
        containerBottomSheet.visibility = View.VISIBLE
        setBottomSheetContentMargin(-108f)
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        if (venue is Store) {
            showStorePreviewData(venue)
        } else if (venue is LuckyU) {
            showLuckyUPreviewData(venue)
        }
    }

    private fun hideVenuePreviewBottomSheet() {
        if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
            setBottomSheetContentMargin(0f)
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun showStorePreviewData(store: Store) {
        disposables.add(
            bottomSheetRoot.clicks()
                .throttleFirst(LuckyInsideConstant.RX_THROTTLEFIRST_SHORT_DURATION, TimeUnit.MILLISECONDS)
                .subscribe({
                    mapPresenter.mapClicked.onNext(true)
                    mapPresenter.setSelectedMarkerUnselected()
                    onRequestStoreDetail(store.storeIndex)
                }) {
                    viewModel.message.onNext("${it.message}")
                }
        )

        val bannerImageUrl = DataUtil.getExistImageUrl(store)
        if (!bannerImageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(bannerImageUrl)
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true))
                .into(circleImageViewVenue)
        }

        containerVenueCategory.background = getDrawable(R.drawable.shape_storepreview_category)
        textViewVenueCategory.setTextColor(getColor(R.color.luckyinside_symbol))
        textViewVenueCategory.text = store.storeType?.toUpperCase()
        textViewVenueName.text = store.storeName
        if (!store.businessStartHour.isNullOrEmpty() && !store.businessEndHour.isNullOrEmpty()) {
            textViewWorkingTime.text = "${store.businessStartHour} - ${store.businessEndHour}"
        } else {
            textViewWorkingTime.text = getString(R.string.all_empty_info)
        }

        if (store.possibleGetYn == LuckyInsideConstant.COMMON_YN_Y) {
            containerBenefit.visibility = View.VISIBLE
            containerHotPlace.visibility = View.GONE
        } else {
            containerBenefit.visibility = View.GONE
            containerHotPlace.visibility = View.VISIBLE
        }

        val userLocation = mapPresenter.getUserLatLng()
        val storeRegion = mapPresenter.getRegion(store.regionIndex!!)

        if (userLocation == null || storeRegion == null || !storeRegion.isLocatedInRegion(userLocation)) {
            textViewDistance.visibility = View.GONE
            containerNotInRegion.visibility = View.VISIBLE
        } else {
            textViewDistance.visibility = View.VISIBLE
            containerNotInRegion.visibility = View.GONE
            val userLatLng = LatLng(userLocation.latitude, userLocation.longitude)
            val storeLatLng = LatLng(store.latitude!!.toDouble(), store.longitude!!.toDouble())
            var distance = userLatLng.distanceTo(storeLatLng)
            distance = Math.round(distance).toDouble()
            if (distance / 1000 < 1) {
                textViewDistance.text = "${distance}m"
            } else {
                distance = Math.ceil(distance / 100) / 10
                textViewDistance.text = "${distance}km"
            }
        }
    }

    private fun showLuckyUPreviewData(luckyU: LuckyU) {
        disposables.add(
            bottomSheetRoot.clicks()
                .throttleFirst(LuckyInsideConstant.RX_THROTTLEFIRST_SHORT_DURATION, TimeUnit.MILLISECONDS)
                .subscribe({
                    mapPresenter.mapClicked.onNext(true)
                    mapPresenter.setSelectedMarkerUnselected()
                    onRequestLuckyUDetail(luckyU.luckyuIndex)
                }) {
                    viewModel.message.onNext("${it.message}")
                }
        )

        val bannerImageUrl = DataUtil.getExistImageUrl(luckyU)
        if (!bannerImageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(bannerImageUrl)
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true))
                .into(circleImageViewVenue)
        }

        containerVenueCategory.background = getDrawable(R.drawable.shape_storepreview_category)
        textViewVenueCategory.setTextColor(getColor(R.color.luckyinside_symbol))
        textViewVenueCategory.text = luckyU.showKinds?.toUpperCase()
        textViewVenueName.text = luckyU.luckyuName

        val startTime = luckyU.showStartDatetime
        val endTime = luckyU.showEndDatetime
        if (startTime.isNullOrBlank() || endTime.isNullOrBlank()) {
            textViewWorkingTime.text = getString(R.string.all_empty_info)
        } else {
            textViewWorkingTime.text = "${startTime} - ${endTime}"
        }

        if (luckyU.possibleGetYn == LuckyInsideConstant.COMMON_YN_Y) {
            containerBenefit.visibility = View.VISIBLE
            containerHotPlace.visibility = View.GONE
        } else {
            containerBenefit.visibility = View.GONE
            containerHotPlace.visibility = View.VISIBLE
        }

        val userLocation = mapPresenter.getUserLatLng()
        val luckyURegion = mapPresenter.getRegion(luckyU.regionIndex!!)

        if (userLocation == null || luckyURegion == null || !luckyURegion.isLocatedInRegion(userLocation)) {
            textViewDistance.visibility = View.GONE
            containerNotInRegion.visibility = View.VISIBLE
        } else {
            textViewDistance.visibility = View.VISIBLE
            containerNotInRegion.visibility = View.GONE
            val userLatLng = LatLng(userLocation.latitude, userLocation.longitude)
            val luckyULatLng = LatLng(luckyU.latitude!!.toDouble(), luckyU.longitude!!.toDouble())
            var distance = userLatLng.distanceTo(luckyULatLng)
            distance = Math.round(distance).toDouble()
            if (distance / 1000 < 1) {
                textViewDistance.text = "${distance}m"
            } else {
                distance = Math.ceil(distance / 100) / 10
                textViewDistance.text = "${distance}km"
            }
        }
    }

    fun isLocatedInRegion(regionIndex: Int?) : Boolean {
        if (regionIndex == null) {
            return false
        }
        val region = mapPresenter.getRegion(regionIndex)
        val userLocation = getCurrentLatLng()
        if (region == null || userLocation == null) {
            return false
        }
        return region.isLocatedInRegion(userLocation)
    }

    @SuppressWarnings( "MissingPermission")
    fun getCurrentLatLng() : LatLng? {
        val lastLocation = mapPresenter.getUserLatLng()
        lastLocation ?: return null
        return LatLng(lastLocation.latitude, lastLocation.longitude)
    }

    fun getCurrentLocatedInRegion() : Region? {
        return mapPresenter.getLocatedInRegion()
    }

    @SuppressWarnings( "MissingPermission")
    private fun enableLocationComponent() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            mapPresenter.activateLocationComponent()
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    private fun showLandingPage(landing: Landing) {
        when (landing.contentType) {
            Landing.ContentType.STORE -> {
                onRequestStoreDetail(landing.data?.toInt())
            }
            Landing.ContentType.LUCKYU -> {
                onRequestLuckyUDetail(landing.data?.toInt())
            }
            Landing.ContentType.BOARD -> {
                onRequestBoardDetail(landing.data?.toInt())
            }
            Landing.ContentType.MYITEM -> {
                onRequestMyItem()
            }
            Landing.ContentType.LUCKYDRAW -> {
                onRequestGetProduct(landing.data?.toInt())
            }
            Landing.ContentType.NONE -> return
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent()
        } else {
            finish()
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) { }

    override fun onRequestRegionSearch(region: Region) { }

    override fun onRequestRegionSearch(regionIndex: Int?) {
        mapPresenter.animateRegion(regionIndex)
    }

    private fun processTutorial() {
        if (!onRequestIsSawTutorial(TAG)) {
            containerTutorials.visibility = View.VISIBLE
            disposables.add(
                containerTutorials.clicks()
                    .subscribe({
                        tutorialIndex++
                        showNextTutorial()
                    }) {
                        viewModel.message.onNext("${it.message}")
                    }
            )
        }
    }

    private fun showNextTutorial() {
        val resId =
            when (tutorialIndex) {
                1 -> {
                    containerTutorial1.visibility = View.VISIBLE
                    containerTutorial2.visibility = View.GONE
                    1
                }
                2 -> {
                    containerTutorial1.visibility = View.GONE
                    containerTutorial2.visibility = View.VISIBLE
                    2
                }
                else -> null
            }
        if (resId == null) {
            imageButtonDaily.visibility = View.VISIBLE
            containerTutorials.visibility = View.GONE
            onSawTutorial(TAG)
        }
    }

    override fun onRequestShare(
        type: String,
        shareImageUri: Uri,
        content: ShareContent?
    ) { }

    override fun onRequestSetting() {
        val intent = Intent(this, SettingActivity::class.java)
        intent.putExtra(LuckyInsideConstant.KEY_USER, viewModel.user)
        startActivityForResult(intent, REQ_CODE_SETTING)
    }

    override fun onRequestHelp() {
        val fragment = HelpFragment()
        replaceFragment(fragment, R.id.container, tag = HelpFragment.TAG, isSlideAnim = true)
    }

    override fun onRequestNews() {
        val fragment = NewsFragment()
        replaceFragment(fragment, R.id.container, tag = NewsFragment.TAG, isSlideAnim = true)
    }

    override fun onRequestAr(venue: Venue, isNavigationFinished: Boolean) {
        if (isNavigationFinished) {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        val intent = Intent(this, ArActivity::class.java)
        intent.putExtra(LuckyInsideConstant.KEY_USER, viewModel.user)
        intent.putExtra(LuckyInsideConstant.KEY_VENUE, venue)
        startActivityForResult(intent, REQ_CODE_AR)
        this.overridePendingTransition(0, 0)
    }

    override fun onRequestSignin() {
        viewModel.removeTokens()
        val intent = Intent(this, SigninActivity::class.java)
        startActivity(intent)
        finish()
    }

    @SuppressWarnings( "MissingPermission")
    override fun onRequestNavigation(venue: Venue, isAdd: Boolean) {
        val fragment = NavigationFragment()
        fragment.arguments = Bundle().apply {
            putString(LuckyInsideConstant.KEY_PROFILE_URL, viewModel.user.profileImageUrl)
            putInt(LuckyInsideConstant.KEY_DEVICE_HEIGHT, deviceHeight ?: 0)
            putSerializable(LuckyInsideConstant.KEY_VENUE, venue)
            putParcelable(LuckyInsideConstant.KEY_USER_LATLNG, mapPresenter.getUserLatLng())
        }
        replaceFragment(fragment, R.id.container, tag = NavigationFragment.TAG, isAdd = true, isSlideAnim = true)
    }

    override fun onRequestReDrawMarkers() { }

    override fun onRequestBoardDetail(boardIndex: Int?, isAdd: Boolean) {
        if (boardIndex == null) {
            viewModel.message.onNext(getString(R.string.all_error))
            return
        }
        val fragment = BoardDetailFragment()
        fragment.arguments = Bundle().apply {
            putInt(LuckyInsideConstant.KEY_DEVICE_HEIGHT, deviceHeight ?: 0)
            putInt(LuckyInsideConstant.KEY_BOARD_INDEX, boardIndex)
        }

        replaceFragment(fragment, R.id.container, tag = BoardDetailFragment.TAG, isSlideAnim = true, isAdd = isAdd)
    }

    override fun onRequestDaily(isSlideAnim: Boolean) {
        val fragment = DailyFragment()
        fragment.arguments = Bundle().apply {
            putInt(LuckyInsideConstant.KEY_DEVICE_HEIGHT, deviceHeight ?: 0)
        }
        replaceFragment(fragment, R.id.container, tag = DailyFragment.TAG, isSlideAnim = isSlideAnim, isVerticalAnim = true)
    }

    override fun onRequestMenu() {
        val fragment = MenuFragment()
        replaceFragment(fragment, R.id.container, tag = MenuFragment.TAG, isSlideAnim = false, isAdd = true)
    }

    override fun onRequestGetProduct(index: Int?) {
        index ?: return
        val fragment = LuckyDrawFragment()
        fragment.arguments = Bundle().apply {
            putInt(LuckyInsideConstant.KEY_LUCKY_DRAW_INDEX, index)
            putInt(LuckyInsideConstant.KEY_DEVICE_HEIGHT, deviceHeight ?: 0)
        }
        replaceFragment(fragment, R.id.container, tag = LuckyDrawFragment.TAG, isSlideAnim = true)
    }

    override fun onRequestGetProductDetail(luckyDrawIndex: Int, productIndex: Int) {
        val fragment = LuckyDrawDetailFragment()
        fragment.arguments = Bundle().apply {
            putInt(LuckyInsideConstant.KEY_LUCKY_DRAW_INDEX, luckyDrawIndex)
            putInt(LuckyInsideConstant.KEY_GET_PRODUCT_INDEX, productIndex)
            putInt(LuckyInsideConstant.KEY_DEVICE_HEIGHT, deviceHeight ?: 0)
        }
        replaceFragment(fragment, R.id.container, tag = LuckyDrawDetailFragment.TAG, isSlideAnim = true)
    }

    override fun onRequestGetProductFinish(
        exchangePlaceName: String?,
        productName: String?,
        productImageUrl: String?
    ) {
        val fragment = LuckyDrawFinishFragment()
        fragment.arguments = Bundle().apply {
            putString(LuckyInsideConstant.KEY_EXCHANGE_PLACE_NAME, exchangePlaceName)
            putString(LuckyInsideConstant.KEY_GET_PRODUCT_NAME, productName)
            putString(LuckyInsideConstant.KEY_GET_PRODUCT_IMAGE_URL, productImageUrl)
        }
        replaceFragment(fragment, R.id.container, tag = LuckyDrawFinishFragment.TAG, isSlideAnim = true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQ_CODE_AR) {
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                val product = data?.getSerializableExtra(LuckyInsideConstant.KEY_PRODUCT)
                val venue = data?.getSerializableExtra(LuckyInsideConstant.KEY_VENUE) as Venue?
                venue ?: return
                val fragment = ArFinishFragment()
                fragment.arguments = Bundle().apply {
                    putSerializable(LuckyInsideConstant.KEY_PRODUCT, product)
                    putSerializable(LuckyInsideConstant.KEY_VENUE, venue)
                }
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment, null)
                    .addToBackStack(null)
                    .commitAllowingStateLoss()
            } else if (requestCode == REQ_CODE_SETTING) {
                if (data?.getBooleanExtra(LuckyInsideConstant.KEY_IS_LOGOUT, false)!!) {
                    onRequestSignin()
                } else {
                    val user = data.getSerializableExtra(LuckyInsideConstant.KEY_USER) as User
                    viewModel.updateUser.onNext(user)
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            if (requestCode == REQ_CODE_AR) {
                val isUnAuthorizedToken = data?.getBooleanExtra(LuckyInsideConstant.KEY_UNAUTHORIZED_TOKEN, false)
                if (isUnAuthorizedToken == true) {
                    onTokenUnAuthorized()
                }
            } else if (requestCode == REQ_CODE_SETTING) {
                val isUnAuthorizedToken = data?.getBooleanExtra(LuckyInsideConstant.KEY_UNAUTHORIZED_TOKEN, false)
                if (isUnAuthorizedToken == true) {
                    onTokenUnAuthorized()
                }
            }
        }
    }

    override fun onRequestMyStore() {
        val fragment = MyStoreFragment()

        val currentUserLatLng = getCurrentLatLng()
        val currentRegion =
            if (currentUserLatLng == null) {
                null
            } else {
                getCurrentLocatedInRegion()
            }

        val bundle = Bundle().apply {
            putSerializable(LuckyInsideConstant.KEY_USER, viewModel.user)
            putParcelable(LuckyInsideConstant.KEY_USER_LATLNG, currentUserLatLng)
            putSerializable(LuckyInsideConstant.KEY_REGION, currentRegion)
        }
        fragment.arguments = bundle

        replaceFragment(fragment, R.id.container, tag = MyStoreFragment.TAG, isSlideAnim = true)
    }

    override fun onRequestMyItem(isAdd: Boolean) {
        val fragment = MyItemFragment()

        val currentUserLatLng = getCurrentLatLng()
        val currentRegion =
            if (currentUserLatLng == null) {
                null
            } else {
                getCurrentLocatedInRegion()
            }

        val bundle = Bundle().apply {
            putSerializable(LuckyInsideConstant.KEY_USER, viewModel.user)
            putParcelable(LuckyInsideConstant.KEY_USER_LATLNG, currentUserLatLng)
            putSerializable(LuckyInsideConstant.KEY_REGION, currentRegion)
        }
        fragment.arguments = bundle

        replaceFragment(fragment, R.id.container, tag = MyItemFragment.TAG, isSlideAnim = true, isAdd = isAdd)
    }

    override fun onRequestMyItemDetail(item: MyItem) {
        val fragment = MyItemDetailFragment()

        val bundle = Bundle().apply {
            putInt(LuckyInsideConstant.KEY_DEVICE_HEIGHT, deviceHeight ?: 0)
            putSerializable(LuckyInsideConstant.KEY_MYITEM, item)
        }
        fragment.arguments = bundle

        replaceFragment(fragment, R.id.container, tag = MyItemDetailFragment.TAG, isSlideAnim = true)
    }

    override fun onRequestLuckyU() {
        val fragment = LuckyUMainFragment()
        val bundle = Bundle()
        fragment.arguments = bundle
        replaceFragment(fragment, R.id.container, tag = LuckyUMainFragment.TAG, isSlideAnim = false)
    }

    override fun onRequestLuckyUDetail(luckyUIndex: Int?, isAdd: Boolean, isSlideAnim: Boolean) {
        if (luckyUIndex == null) {
            viewModel.message.onNext(getString(R.string.all_error))
            return
        }
        val fragment = LuckyUDetailFragment()
        val bundle = Bundle().apply {
            putInt(LuckyInsideConstant.KEY_DEVICE_HEIGHT, deviceHeight ?: 0)
            putInt(LuckyInsideConstant.KEY_LUCKYU_INDEX, luckyUIndex)
        }
        fragment.arguments = bundle
        replaceFragment(fragment, R.id.container, tag = LuckyUDetailFragment.TAG, isSlideAnim = isSlideAnim, isAdd = isAdd)
    }

    @SuppressWarnings( "MissingPermission")
    override fun onRequestStoreDetail(storeIndex: Int?, isAdd: Boolean) {
        if (storeIndex == null) {
            viewModel.message.onNext(getString(R.string.all_error))
            return
        }
        val fragment = StoreDetailFragment()
        val bundle = Bundle().apply {
            putSerializable(LuckyInsideConstant.KEY_STORE_INDEX, storeIndex)
            putInt(LuckyInsideConstant.KEY_DEVICE_HEIGHT, deviceHeight ?: 0)
        }
        fragment.arguments = bundle
        replaceFragment(fragment, R.id.container, tag = StoreDetailFragment.TAG, isSlideAnim = true, isAdd = isAdd)
    }

    override fun onRequestDailyVenueList(regionIndex: Int) {
        val fragment = DailyVenuesFragment()
        val bundle = Bundle().apply {
            putInt(LuckyInsideConstant.KEY_REGION_INDEX, regionIndex)
        }
        fragment.arguments = bundle
        replaceFragment(fragment, R.id.container, tag = DailyVenuesFragment.TAG, isSlideAnim = true)
    }

    override fun onRequestDailyBannerImage(banner: DailyBanner) {
        val fragment = DailyBannerImageFragment()
        val bundle = Bundle().apply {
            putSerializable(LuckyInsideConstant.KEY_DAILY_BANNER, banner)
        }
        fragment.arguments = bundle
        replaceFragment(fragment, R.id.container, tag = DailyBannerImageFragment.TAG, isSlideAnim = true)
    }

    override fun onRequestPopupNotice(popupNotice: PopupNotice) {
        val fragment = PopupNoticeFragment()
        val bundle = Bundle().apply {
            putSerializable(LuckyInsideConstant.KEY_POPUP_NOTICE, popupNotice)
        }
        fragment.arguments = bundle
        replaceFragment(fragment, R.id.container, tag = PopupNoticeFragment.TAG, isSlideAnim = false, isAdd = true)
    }

    override fun onRequestIsSawTutorial(viewTag: String): Boolean =
        when (viewTag) {
            TAG -> viewModel.loadIsSawMapHelp()
            MyStoreFragment.TAG -> viewModel.loadIsSawMyStoreHelp()
            MyItemFragment.TAG -> viewModel.loadIsSawMyItemHelp()
            StoreDetailFragment.TAG -> viewModel.loadIsSawStoreHelp()
            LuckyUDetailFragment.TAG -> viewModel.loadIsSawStoreHelp()
            NavigationFragment.TAG -> viewModel.loadIsSawNavigationHelp()
            LuckyUMainFragment.TAG -> viewModel.loadIsSawLuckyUHelp()
            ArFinishFragment.TAG -> viewModel.loadIsSawGetHelp()
            else -> true
        }

    override fun onSawTutorial(viewTag: String) {
        when (viewTag) {
            TAG -> viewModel.updateIsSawMapHelp()
            MyStoreFragment.TAG -> viewModel.updateIsSawMyStoreHelp()
            MyItemFragment.TAG -> viewModel.updateIsSawMyItemHelp()
            StoreDetailFragment.TAG -> viewModel.updateIsSawStoreHelp()
            LuckyUDetailFragment.TAG -> viewModel.updateIsSawStoreHelp()
            NavigationFragment.TAG -> viewModel.updateIsSawNavigationHelp()
            LuckyUMainFragment.TAG -> viewModel.updateIsSawLuckyUHelp()
            ArFinishFragment.TAG -> viewModel.updateIsSawGetHelp()
        }
    }

    override fun onTokenUnAuthorized() {
        viewModel.message.onNext(getString(R.string.all_token_unauthorized))
        onRequestSignin()
    }

    private fun isRunningActionButtonAnimtor(): Boolean {
        if (actionButtonAnimtorSet == null) {
            return false
        }
        return actionButtonAnimtorSet?.isRunning == true
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (!isRunningActionButtonAnimtor()) {
            actionButtonAnimtorSet = AnimationUtil.getActionButtonAnimator(imageButtonDaily)
            actionButtonAnimtorSet?.start()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        mapView.onSaveInstanceState(outState!!)
    }

    override fun onBackPressed() {
        val myItemDetailFragment = supportFragmentManager.findFragmentByTag(MyItemDetailFragment.TAG)
        val luckyUMainDetailFragment = supportFragmentManager.findFragmentByTag(LuckyUMainFragment.TAG)
        val getProductDetailFragment = supportFragmentManager.findFragmentByTag(
            LuckyDrawDetailFragment.TAG)
        val dailyFragment = supportFragmentManager.findFragmentByTag(DailyFragment.TAG)
        val menuFragment = supportFragmentManager.findFragmentByTag(MenuFragment.TAG)
        // 상점정보 보여지는 경우
        if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
            mapPresenter.mapClicked.onNext(true)
            mapPresenter.setSelectedMarkerUnselected()
        }
        // 지도에서 지역반경이 표시되는 경우 (현재 상점마커들이 보일때)
        else if (supportFragmentManager.backStackEntryCount == 0 && mapPresenter.isShowingRegionArea()) {
            mapPresenter.animateDefaultMap()
        }
        // 지도에서 지역반경이 표시되지 않은경우 (현재 지역마커들이 보일때)
        else if (supportFragmentManager.backStackEntryCount == 0 && mapPresenter.isShowingRegionMarker()) {
            onRequestDaily()
        }
        // 마이아이템 상세 (교환 처리)
        else if (myItemDetailFragment != null && myItemDetailFragment.isVisible) {
            val fragment = myItemDetailFragment as MyItemDetailFragment
            if (fragment.isBottomSheetVisible()) {
                fragment.hideBottomSheet()
            } else {
                super.onBackPressed()
            }
        }
        // 럭키유 메인 (필터화면 처리)
        else if (luckyUMainDetailFragment != null && luckyUMainDetailFragment.isVisible) {
            val fragment = luckyUMainDetailFragment as LuckyUMainFragment
            if (fragment.isVisibleFilterView()) {
                fragment.switchFilterViewVisibility(false)
            } else {
                super.onBackPressed()
            }
        }
        // 겟 프로덕트 상세 (겟 처리)
        else if (getProductDetailFragment != null && getProductDetailFragment.isVisible) {
            val fragment = getProductDetailFragment as LuckyDrawDetailFragment
            if (fragment.isBottomSheetVisible()) {
                fragment.hideBottomSheet()
            } else {
                super.onBackPressed()
            }
        }
        // 데일리
        else if (dailyFragment != null) {
            // fragment add로 추가될경우 isVisible 이라도 뒤에 깔린경우가 있어서 count = 0 체크 (데일리 화면 보이는 상태)
            if (dailyFragment.isVisible && supportFragmentManager.backStackEntryCount < 2) {
                if (System.currentTimeMillis() > backButtonPressTime + BACKBUTTON_PRESS_INTERVAL) {
                    viewModel.message.onNext(getString(R.string.all_backbutton_press))
                    backButtonPressTime = System.currentTimeMillis()
                } else {
                    finish()
                }
            } else {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }
}