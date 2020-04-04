package com.lhbros.luckyinside.store.view

import android.animation.Animator
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.text.HtmlCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.jakewharton.rxbinding3.view.clicks
import com.lhbros.luckyinside.R
import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.model.Category
import com.lhbros.luckyinside.common.model.LuckyInsideLog
import com.lhbros.luckyinside.common.util.AndroidUtil
import com.lhbros.luckyinside.common.util.AnimationUtil
import com.lhbros.luckyinside.common.util.UiUtil
import com.lhbros.luckyinside.common.view.CustomViewPagerScroller
import com.lhbros.luckyinside.map.model.*
import com.lhbros.luckyinside.map.view.MapBaseFragment
import com.lhbros.luckyinside.share.view.ShareDialogFragment
import com.lhbros.luckyinside.common.view.adapter.BannerImageAdapter
import com.lhbros.luckyinside.share.model.ShareContent
import com.lhbros.luckyinside.store.model.Store
import com.lhbros.luckyinside.store.model.StoreAdditionalInfo
import com.lhbros.luckyinside.store.model.StoreCaution
import com.lhbros.luckyinside.store.model.StoreInfo
import com.lhbros.luckyinside.store.viewmodel.StoreDetailViewModel
import com.lhbros.luckyinside.store.viewmodel.StoreDetailViewModelFactory
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_store_detail.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.IllegalArgumentException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.ceil

class StoreDetailFragment : MapBaseFragment() {
    companion object {
        const val TAG = "storeDetailFragment"
        const val PAGING_DURATION = 5000L
        const val PAGING_ZOOM = 1.15f
    }

    @Inject
    lateinit var viewModelFactory: StoreDetailViewModelFactory
    private lateinit var viewModel: StoreDetailViewModel

    private var viewPagerHandler = Handler()
    private var bannerAdapter: BannerImageAdapter? = null
    private var currentBannerAnim: ViewPropertyAnimator? = null
    private var currentBannerPosition = 0

    private var storeIndex: Int? = null
    private var deviceHeight: Int = 0

    private var tutorialIndex = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory)[StoreDetailViewModel::class.java]
        storeIndex = arguments?.getSerializable(LuckyInsideConstant.KEY_STORE_INDEX) as Int
        deviceHeight = arguments?.getInt(LuckyInsideConstant.KEY_DEVICE_HEIGHT) ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_store_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUi()
        subscribeViewModel()
        subscribeUi()
    }

    private fun initializeUi() {
        setStatusBarHeight()
        setViewPagerHeight()
        initializeScrollView()
        processTutorial()
    }

    private fun subscribeUi() {
        disposables.add(
            imageButtonShare.clicks()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .map {
                    Uri.parse(viewModel.storeDetail.value?.bannerImageUrl1)
                }
                .subscribe({
                    showShareDialog(it)
                }){
                    viewModel.message.onNext(getString(R.string.all_error))
                }
        )

        disposables.add(
            imageButtonBack.clicks()
                .subscribe {
                    activity!!.onBackPressed()
                }
        )

        disposables.add(
            imageButtonCopy.clicks()
                .subscribe({
                    copyAddress()
                }){
                    viewModel.message.onNext(getString(R.string.all_error))
                }
        )

        disposables.add(
            imageButtonMyStore.clicks()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    processMyStoreButtonClick()
                }){
                    viewModel.message.onNext(getString(R.string.all_error))
                }
        )
    }

    private fun showShareDialog(imageUri: Uri) {
        val bundle = Bundle().apply {
            putString(LuckyInsideConstant.KEY_SHARE_TYPE, LuckyInsideConstant.KEY_SHARE_TYPE_LINK)
            putSerializable(LuckyInsideConstant.KEY_SHARE_CONTENT, getShareContent())
            putParcelable(LuckyInsideConstant.KEY_SHARE_IMAGE_URI, imageUri)
        }

        val dialog = ShareDialogFragment().apply {
            arguments = bundle
            setTargetFragment(this, LuckyInsideConstant.REQ_CODE_DIALOG)
        }

        dialog.show(activity!!.supportFragmentManager,
            ShareDialogFragment.TAG
        )
    }

    private fun processMyStoreButtonClick() {
        if (viewModel.getIsMyStore()) {
            viewModel.requestDeleteMyStore()
        } else {
            viewModel.requestRegistMyStore()
        }
    }

    private fun subscribeViewModel() {
        disposables.add(
            viewModel.unAuthorizedTokenException
                .subscribe({
                    tokenUnAuthorizedListener.onTokenUnAuthorized()
                }) {
                    viewModel.message.onNext("${it.message}")
                }
        )

        disposables.add(
            viewModel.storeDetail
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showStoreDetail(it)
                }){
                    viewModel.message.onNext(it.message!!)
                }
        )

        disposables.add(
            viewModel.message
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showToast(it)
                }){
                    showToast("message : ${it.message}")
                }
        )

        disposables.add(
            viewModel.isMyStoreRegistSuccess
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    processAfterMyStoreRegist(it)
                }){
                    viewModel.message.onNext("${it.message}")
                }
        )

        disposables.add(
            viewModel.isMyStoreDeleteSuccess
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it) {
                        showMyStorePopup(false)
                        switchMyStore(false)
                    } else {
                        viewModel.message.onNext(getString(R.string.storedetail_mystore_delete_fail))
                    }
                }){
                    viewModel.message.onNext(getString(R.string.storedetail_mystore_delete_fail))
                }
        )

        disposables.add(viewModel.requestStoreDetail(storeIndex!!))
    }

    private fun processAfterMyStoreRegist(isSuccess: Boolean) {
        if (isSuccess) {
            showMyStorePopup(true)
            switchMyStore(true)
        } else {
            viewModel.message.onNext(getString(R.string.storedetail_mystore_regist_fail))
        }
    }

    private fun setStatusBarHeight() {
        val statusBarHeight = AndroidUtil.getStatusBarHeight(context!!)

        val headerHeight = resources.getDimension(R.dimen.common_header_height)
        containerGradient.layoutParams.height = statusBarHeight + headerHeight.toInt()

        val params = indicator.layoutParams as ConstraintLayout.LayoutParams
        params.topMargin += statusBarHeight

        containerTutorials.setPadding(0, statusBarHeight, 0, 0)
    }

    private fun initializeScrollView() {
        val statusBarHeight = AndroidUtil.getStatusBarHeight(context!!)
        val headerHeight = resources.getDimension(R.dimen.common_header_height)

        val subScrollViewHeight = (deviceHeight - (headerHeight + statusBarHeight))
        val subScrollViewParam = scrollViewSub.layoutParams
        subScrollViewParam.height = subScrollViewHeight.toInt()
        scrollViewSub.layoutParams = FrameLayout.LayoutParams(subScrollViewParam)

        scrollView.setOnScrollChangeListener { _: NestedScrollView?, _: Int, scrollY: Int, _: Int, _: Int ->
            if (scrollView != null) {
                if (scrollY == subScrollViewHeight.toInt() || scrollY > subScrollViewHeight) {
                    scrollViewSub?.isNestedScrollingEnabled = true
                } else if (scrollY < subScrollViewHeight) {
                    val alphaChangeStartHeight = subScrollViewHeight / 3f
                    val alphaChangeEndHeight = subScrollViewHeight * (2f / 3f)

                    if (scrollY < alphaChangeEndHeight) {
                        containerStoreIntro.alpha = 1f - (scrollY / alphaChangeEndHeight)
                        hideStoreName()
                    } else if (scrollY > alphaChangeEndHeight) {
                        showStoreName()
                        textViewStoreName.alpha = ((scrollY - alphaChangeEndHeight) / alphaChangeStartHeight)
                    }

                    if (scrollViewSub?.isNestedScrollingEnabled == true) {
                        scrollViewSub?.isNestedScrollingEnabled = false
                    }
                }
            }
        }

        setScrollViewToTop()
    }

    private fun setViewPagerHeight() {
        val viewPagerParams = containerViewPager.layoutParams
        viewPagerParams.height = deviceHeight
        containerViewPager.layoutParams = ConstraintLayout.LayoutParams(viewPagerParams)
    }

    private fun hideStoreName() {
        if (textViewStoreName?.visibility == View.VISIBLE) {
            textViewStoreName?.visibility = View.INVISIBLE
        }
    }

    private fun showStoreName() {
        if (textViewStoreName.visibility == View.INVISIBLE) {
            textViewStoreName.visibility = View.VISIBLE
        }
    }

    private fun setScrollViewToTop() {
        scrollView.viewTreeObserver.addOnPreDrawListener( object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                scrollView.scrollTo(0, 0)
                scrollView.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })
    }

    private fun setPossibleGetView(isPossibleGet: Boolean, hayoLocation: String?) {
        if (isPossibleGet) {
            containerPossibleGet.visibility = View.VISIBLE
            imageViewAddress.setImageResource(R.drawable.ic_address_store)
            imageViewVenue.visibility = View.GONE
            imageViewHeart.visibility = View.VISIBLE
            if (hayoLocation.isNullOrBlank()) {
                containerVenue.visibility = View.INVISIBLE
            }
            textViewVenue.text = hayoLocation
        } else {
            containerPossibleGet.visibility = View.GONE
            imageViewAddress.setImageResource(R.drawable.ic_address_inactive)
            imageViewVenue.visibility = View.VISIBLE
            imageViewHeart.visibility = View.GONE
            textViewVenue.text = getString(R.string.storedetail_hotplace_info)
        }
    }

    private fun animateProductAndLocationView() {
        val offset = UiUtil.dpToPx(context!!, 39f).toFloat()
        containerProduct?.animate()?.translationX(offset)?.setDuration(300L)?.start()
        containerDistance?.animate()?.translationX(-offset)?.setDuration(300L)?.start()
    }

    private fun activateNavigationView() {
        addNavigationButtonRx()
        textViewDistance.visibility = View.VISIBLE
        containerNavigationInactive.visibility = View.GONE
        imageButtonNavigation.setImageResource(R.drawable.ic_navigation_active)
        animateActionButtonScale(imageButtonNavigation)
    }

    private fun addNavigationButtonRx() {
        disposables.add(
            imageButtonNavigation.clicks()
                .throttleFirst(LuckyInsideConstant.RX_THROTTLEFIRST_LONG_DURATION, TimeUnit.MILLISECONDS)
                .subscribe {
                    viewModel.requestLogNavigation(LuckyInsideLog(storeIndex = storeIndex))
                    requestListener.onRequestNavigation(viewModel.storeDetail.value!!)
                }
        )
    }

    private fun showMyStorePopup(isRegist: Boolean) {
        containerMyStorePopup.pivotX = containerMyStorePopup.measuredWidth.toFloat() / 2
        containerMyStorePopup.pivotY = containerMyStorePopup.measuredHeight.toFloat() / 2
        if (isRegist) {
            imageViewMyStorePopup.setImageResource(R.drawable.ic_mystore_popup_green)
            textViewMyStorePopup.text = getString(R.string.storedetail_mystore_regist_success)
        } else {
            imageViewMyStorePopup.setImageResource(R.drawable.ic_mystore_popup_white)
            textViewMyStorePopup.text = getString(R.string.storedetail_mystore_delete_success)
        }
        containerMyStorePopup.visibility = View.VISIBLE
        animateMyStorePopup()
    }

    private fun animateMyStorePopup() {
        val animSet = AnimationUtil.getPopupAnimatorSet(containerMyStorePopup)
        animSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationStart(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                containerMyStorePopup?.visibility = View.INVISIBLE
                containerMyStorePopup?.alpha = 1f
            }
        })
        animSet.start()
    }

    private fun animateImageViews(position: Int) {
        val view = bannerAdapter?.getItem(position)
        view?.pivotX = UiUtil.getViewCenterX(viewPager)
        view?.pivotY = UiUtil.getViewCenterY(viewPager)
        currentBannerAnim = view?.animate()?.scaleX(PAGING_ZOOM)?.scaleY(
            PAGING_ZOOM
        )?.setDuration(PAGING_DURATION)?.setInterpolator(LinearInterpolator())
        currentBannerAnim?.start()
    }

    private fun copyAddress() {
        if (context != null) {
            AndroidUtil.copyText(context!!, textViewAddress.text.toString())
            viewModel.message.onNext(getString(R.string.all_copy))
        }
    }

    private fun getShareContent() : ShareContent {
        val store = viewModel.storeDetail.value
        val keywords = getStoreKeywords(store)
        return ShareContent(
            title = "[${store?.storeName}]${store?.storeTitle}",
            content = getString(R.string.share_kakao_description),
            keywords = keywords,
            venueIndex = store?.storeIndex,
            venueType = Venue.VenueType.STORE
        )
    }

    private fun getStoreKeywords(store: Store?): List<String> {
        val keywords = mutableListOf<String>()

        if (store == null || store.isStoreCategoryListEmpty()) {
            return keywords
        }

        for (category in store.storeCategoryList!!) {
            if (!category.categoryName.isNullOrBlank()) {
                keywords.add(category.categoryName)
            }
        }

        return keywords
    }

    private fun showStoreDetail(store: Store) {
        activateNavigationView()
        animateProductAndLocationView()
        imageButtonNavigation.visibility = View.VISIBLE
        setPossibleGetView(store.possibleGetYn == LuckyInsideConstant.COMMON_YN_Y, store.venueList)
        initializeBannerViews(store)
        initializeCategories(store)
        initializeStoreInfos(store.storeInfoList)
        initializeAdditionalInfo(store.storeAdditionList)
        initializeCaution(store.storeCautionList)
        setMyStoreIcon(viewModel.getIsMyStore())
        setTexts(store)
        requestStoreDistance(store.longitude?.toDouble(), store.latitude?.toDouble())
    }

    private fun setTexts(store: Store) {
        textViewStoreCategory.text = store.storeType?.toUpperCase()
        textViewLuckyComment.text = HtmlCompat.fromHtml(store.storeDescription ?: "", HtmlCompat.FROM_HTML_MODE_LEGACY).trim()
        textViewAddress.text = store.address?.split("(")?.get(0) ?: ""
        textViewIntroStoreName.text = store.storeName
        textViewIntroStoreTitle.text = store.storeTitle
        textViewStoreName.text = store.storeName
        textViewMyStoreCount.text = store.myStoreCnt.toString()
        textViewProduct.text = "x${store.getPossibleCnt?.toString() ?: 0}"
    }

    private fun requestStoreDistance(longitude: Double?, latitude: Double?) {
        longitude ?: return
        latitude ?: return

        val userLatLng = getCurrentUserLatLng()
        userLatLng ?: return

        MapboxDirections.builder()
            .accessToken(getString(R.string.mapbox_access_token))
            .origin(Point.fromLngLat(userLatLng.longitude, userLatLng.latitude))
            .destination(Point.fromLngLat(longitude, latitude))
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .build()
            .enqueueCall(object : Callback<DirectionsResponse> {
                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    showStoreDistance(0.0)
                }

                override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                    val distanceString = response.body()?.routes()?.get(0)?.distance() ?: 0.0
                    showStoreDistance(distanceString)
                }
            })
    }

    private fun showStoreDistance(distance: Double?) {
        val distanceNumber = distance?.toInt() ?: 0
        if (distanceNumber / 1000 < 1) {
            textViewDistance?.text = "${distanceNumber}m"
        } else {
            textViewDistance?.text = "${ceil(distanceNumber / 100f) / 10}km"
        }
    }

    private fun switchMyStore(isMyStore: Boolean) {
        setMyStoreIcon(isMyStore)

        var currentCount = textViewMyStoreCount.text.toString().toInt()
        if (isMyStore) {
            currentCount += 1
            viewModel.storeDetail.value!!.myStoreRegistrationYn = LuckyInsideConstant.COMMON_YN_Y
        } else {
            currentCount -= 1
            viewModel.storeDetail.value!!.myStoreRegistrationYn = LuckyInsideConstant.COMMON_YN_N
        }

        textViewMyStoreCount.text = "$currentCount"
    }

    private fun setMyStoreIcon(isMyStore: Boolean) {
        if (isMyStore) {
            imageButtonMyStore.setImageResource(R.drawable.ic_mystore_green)
        } else {
            imageButtonMyStore.setImageResource(R.drawable.ic_mystore_white)
        }
    }

    private fun initializeAdditionalInfo(additionalInfos: List<StoreAdditionalInfo>?) {
        if (additionalInfos.isNullOrEmpty()) {
            textViewAdditionalInfoTitle.visibility = View.GONE
            containerAdditionalInfo.visibility = View.GONE
            return
        }

        val views = mutableListOf<View>()
        for (info in additionalInfos) {
            val view = layoutInflater.inflate(R.layout.item_addition_info, null) as ConstraintLayout
            view.id = View.generateViewId()

            val textView = view.findViewById<TextView>(R.id.textViewContent)
            textView.text = info.additionInfo

            containerAdditionalInfo.addView(view)
            views.add(view)
        }

        val constraintSet = ConstraintSet()
        for ((index, view) in views.withIndex()) {
            constraintSet.constrainWidth(view.id, ConstraintSet.WRAP_CONTENT)
            constraintSet.constrainHeight(view.id, ConstraintSet.WRAP_CONTENT)
            if (index != 0) {
                constraintSet.connect(view.id, ConstraintSet.TOP,
                    views[index - 1].id, ConstraintSet.BOTTOM,
                    UiUtil.dpToPx(context!!, 16f))
            }
        }

        constraintSet.applyTo(containerAdditionalInfo)
    }

    private fun initializeCaution(cautions: List<StoreCaution>?) {
        if (cautions.isNullOrEmpty()) {
            textViewWarningTitle.visibility = View.GONE
            containerCaution.visibility = View.GONE
            return
        }

        val views = mutableListOf<View>()
        for (caution in cautions) {
            val view = layoutInflater.inflate(R.layout.item_addition_info, null) as ConstraintLayout
            view.id = View.generateViewId()
            val textView = view.findViewById<TextView>(R.id.textViewContent)
            textView.text = caution.caution
            containerCaution.addView(view)
            views.add(view)
        }

        val constraintSet = ConstraintSet()
        for ((index, view) in views.withIndex()) {
            constraintSet.constrainWidth(view.id, ConstraintSet.WRAP_CONTENT)
            constraintSet.constrainHeight(view.id, ConstraintSet.WRAP_CONTENT)
            if (index != 0) {
                constraintSet.connect(view.id, ConstraintSet.TOP,
                    views[index - 1].id, ConstraintSet.BOTTOM,
                    UiUtil.dpToPx(context!!, 16f))
            }
        }

        constraintSet.applyTo(containerCaution)
    }

    private fun initializeCategories(store: Store) {
        val categories = makeCategories(store)

        if (categories.isNullOrEmpty()) {
            return
        }

        val itemLayouts = mutableListOf<View>()
        for (category in categories) {
            val itemLayout = layoutInflater.inflate(R.layout.item_category, null) as ConstraintLayout
            itemLayout.id = View.generateViewId()

            val imageViewItemImage = itemLayout.findViewById<ImageView>(R.id.imageViewCategoryIcon)

            if (!category.categoryImageUrl.isNullOrBlank()) {
                Glide.with(this)
                    .load(Uri.parse(category.categoryImageUrl))
                    .apply(RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true))
                    .into(imageViewItemImage)
            }

            val textViewItemName = itemLayout.findViewById<TextView>(R.id.textViewCategoryName)
            textViewItemName.text = category.categoryName

            itemLayouts.add(itemLayout)
            containerStoreCategories.addView(itemLayout)
        }

        val constraintSet = ConstraintSet()

        for ((index, itemLayout) in itemLayouts.withIndex()) {
            constraintSet.constrainWidth(itemLayout.id, resources.getDimension(R.dimen.category_size).toInt())
            constraintSet.constrainHeight(itemLayout.id, resources.getDimension(R.dimen.category_size).toInt())
            if (index != 0) {
                constraintSet.connect(itemLayout.id, ConstraintSet.LEFT, itemLayouts[index - 1].id, ConstraintSet.RIGHT)
            }
            constraintSet.connect(itemLayout.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, UiUtil.dpToPx(activity!!, 5f))
            constraintSet.connect(itemLayout.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, UiUtil.dpToPx(activity!!, 5f))
        }

        constraintSet.applyTo(containerStoreCategories)
    }

    private fun makeCategories(store: Store) : List<Category> {
        val categories = mutableListOf<Category>()

        // 지역
        if (!store.regionInfo?.regionName.isNullOrBlank() && !store.regionInfo?.iconImageUrl.isNullOrBlank()) {
            val regionCategory = Category(
                categoryName = store.regionInfo?.regionName,
                categoryImageUrl = store.regionInfo?.iconImageUrl
            )
            categories.add(regionCategory)
        }

        // 대분류
        val storeCategories = store.storeCategoryList
        if (!storeCategories.isNullOrEmpty()) {
            val storeCategory = Category(
                categoryName = storeCategories[0].categoryName,
                categoryImageUrl = storeCategories[0].categoryImageUrl
            )
            categories.add(storeCategory)
        }

        // 오감 1개
        val senseCategories = store.storeCategoryFiveSensesList
        if (!senseCategories.isNullOrEmpty()) {
            val senseCategory = Category(
                categoryName = senseCategories[0].categoryFiveSensesName,
                categoryImageUrl = senseCategories[0].categoryImageUrl
            )
            categories.add(senseCategory)
        }

        // 감정 1개
        val emotionCategories = store.storeCategoryEmotionList
        if (!emotionCategories.isNullOrEmpty()) {
            val emotionCategory = Category(
                categoryName = emotionCategories[0].categoryEmotionName,
                categoryImageUrl = emotionCategories[0].categoryImageUrl
            )
            categories.add(emotionCategory)
        }

        return categories
    }

    private fun initializeStoreInfos(storeInfos: List<StoreInfo>?) {
        if (storeInfos == null) {
            return
        }

        val itemLayouts = mutableListOf<View>()
        for (storeInfo in storeInfos) {
            val itemLayout = layoutInflater.inflate(R.layout.item_store_info, null) as ConstraintLayout
            itemLayout.id = View.generateViewId()

            val imageViewItemImage = itemLayout.findViewById<ImageView>(R.id.imageViewInfoIcon)
            if (!storeInfo.storeInfoUrl.isNullOrBlank()) {
                Glide.with(this)
                    .load(Uri.parse(storeInfo.storeInfoUrl))
                    .apply(RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true))
                    .into(imageViewItemImage)
            }

            val textViewItemDescription = itemLayout.findViewById<TextView>(R.id.textViewInfoDescription)
            textViewItemDescription.text = storeInfo.description
            itemLayouts.add(itemLayout)
            containerStoreInfoSub.addView(itemLayout)
        }

        val constraintSet = ConstraintSet()

        for ((index, itemLayout) in itemLayouts.withIndex()) {
            constraintSet.constrainWidth(itemLayout.id, ConstraintSet.MATCH_CONSTRAINT)
            constraintSet.constrainHeight(itemLayout.id, ConstraintSet.WRAP_CONTENT)
            if (index != 0) {
                constraintSet.connect(itemLayout.id, ConstraintSet.TOP, itemLayouts[index - 1].id, ConstraintSet.BOTTOM, UiUtil.dpToPx(activity!!, 9f))
            }
        }

        constraintSet.applyTo(containerStoreInfoSub)
    }

    private fun initializeBannerViews(store: Store) {
        setViewPagerScroller()

        val views = makeImageViews(store)
        bannerAdapter = BannerImageAdapter()
        bannerAdapter?.setViews(views)
        viewPager.adapter = bannerAdapter
        indicator.setViewPager(viewPager)

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener{
            override fun onPageScrollStateChanged(state: Int) { }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) { }
            override fun onPageSelected(position: Int) {
                viewPagerHandler.removeCallbacksAndMessages(null)
                val view = bannerAdapter?.getItem(currentBannerPosition)
                currentBannerAnim?.cancel()
                view?.scaleX = 1.0f
                view?.scaleY = 1.0f
                currentBannerPosition = position
                animateImageViews(currentBannerPosition)
                postViewPagerHandler()
            }
        })

        val viewCount = bannerAdapter?.count
        viewCount ?: return

        if (viewCount > 0) {
            animateImageViews(0)
        }

        if (viewCount > 1) {
            postViewPagerHandler()
        }
    }

    private fun makeImageViews(store: Store): MutableList<View> {
        val imageUrls = mutableListOf<String>()
        if (!store.bannerImageUrl1.isNullOrEmpty()) imageUrls.add(store.bannerImageUrl1!!)
        if (!store.bannerImageUrl2.isNullOrEmpty()) imageUrls.add(store.bannerImageUrl2!!)
        if (!store.bannerImageUrl3.isNullOrEmpty()) imageUrls.add(store.bannerImageUrl3!!)
        if (!store.bannerImageUrl4.isNullOrEmpty()) imageUrls.add(store.bannerImageUrl4!!)

        val views = mutableListOf<View>()
        for (url in imageUrls) {
            val view = ImageView(activity)
            view.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(this)
                .load(Uri.parse(url))
                .apply(RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true))
                .into(view)
            views.add(view)
        }

        return views
    }

    private fun setViewPagerScroller() {
        try {
            val scroller = ViewPager::class.java.getDeclaredField("mScroller")
            scroller.isAccessible = true
            val customScroller = CustomViewPagerScroller(context!!)
            customScroller.customDuration = 1000
            scroller.set(viewPager, customScroller)
        } catch (e: NoSuchFieldException) {

        } catch (e: IllegalArgumentException) {

        } catch (e: IllegalAccessException) {

        }
    }

    private fun postViewPagerHandler() {
        viewPagerHandler.postDelayed({
            showNextImage()
        }, PAGING_DURATION)
    }

    private fun showNextImage() {
        val curPos = viewPager.currentItem
        val viewCount = bannerAdapter?.count
        viewCount ?: return
        val nextPos = if (curPos == viewCount - 1) 0 else curPos + 1
        viewPager.setCurrentItem(nextPos, false)
    }

    private fun processTutorial() {
        if (!tutorialListener.onRequestIsSawTutorial(TAG)) {
            containerTutorials.visibility = View.VISIBLE
            disposables.add(
                containerTutorials.clicks()
                    .subscribe({
                        tutorialIndex++
                        showNextTutorial()
                    }) {
                        viewModel.message.onNext(getString(R.string.all_error))
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
                    containerTutorial3.visibility = View.GONE
                    containerTutorial4.visibility = View.GONE
                    1
                }
                2 -> {
                    containerTutorial1.visibility = View.GONE
                    containerTutorial2.visibility = View.VISIBLE
                    containerTutorial3.visibility = View.GONE
                    containerTutorial4.visibility = View.GONE
                    2
                }
                3 -> {
                    containerTutorial1.visibility = View.GONE
                    containerTutorial2.visibility = View.GONE
                    containerTutorial3.visibility = View.VISIBLE
                    containerTutorial4.visibility = View.GONE
                    3
                }
                4 -> {
                    containerTutorial1.visibility = View.GONE
                    containerTutorial2.visibility = View.GONE
                    containerTutorial3.visibility = View.GONE
                    containerTutorial4.visibility = View.VISIBLE
                    4
                }
                else -> null
            }
        if (resId == null) {
            containerTutorials.visibility = View.GONE
            tutorialListener.onSawTutorial(TAG)
        }
    }

    override fun onDestroyView() {
        viewPagerHandler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }
}