package com.lhbros.luckyinside.luckydraw.view

import android.animation.Animator
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxbinding3.view.clicks
import com.lhbros.luckyinside.R
import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.constant.FirebaseAnalyticsConst
import com.lhbros.luckyinside.common.model.Category
import com.lhbros.luckyinside.common.model.ExchangePlaceInfo
import com.lhbros.luckyinside.common.util.*
import com.lhbros.luckyinside.common.view.CustomViewPagerScroller
import com.lhbros.luckyinside.luckydraw.model.LuckyDraw
import com.lhbros.luckyinside.luckydraw.model.ProductCaution
import com.lhbros.luckyinside.map.view.MapBaseFragment
import com.lhbros.luckyinside.common.view.adapter.BannerImageAdapter
import com.lhbros.luckyinside.luckydraw.viewmodel.LuckyDrawDetailViewModel
import com.lhbros.luckyinside.luckydraw.viewmodel.LuckyDrawDetailViewModelFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.bottomsheet_luckydraw_detail.*
import kotlinx.android.synthetic.main.fragment_luckydraw_detail.*
import javax.inject.Inject
import kotlin.math.ceil

class LuckyDrawDetailFragment : MapBaseFragment() {
    companion object {
        const val TAG = "getProductDetailFragment"
        const val PAGING_DURATION = 5000L
        const val PAGING_ZOOM = 1.15f
    }

    @Inject
    lateinit var viewModelFactory: LuckyDrawDetailViewModelFactory
    lateinit var viewModel: LuckyDrawDetailViewModel
    lateinit var bannerAdapter: BannerImageAdapter
    private var rootBottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>? = null
    private var currentBannerAnim: ViewPropertyAnimator? = null
    private var currentBannerPosition = 0
    private var viewPagerHandler = Handler()

    val isBackgroundInactive = PublishSubject.create<Boolean>()
    val draggingRatio = PublishSubject.create<Float>()

    var deviceHeight = 0
    var subScrollViewHeight: Int = 0
    var imageViewHeight: Int = 0
    val bottomSheetMinMargin by lazy { activity!!.resources.getDimension(R.dimen.myitemdetail_bottomsheetcontent_minmargin) }
    val bottomSheetMaxMargin by lazy { activity!!.resources.getDimension(R.dimen.myitemdetail_bottomsheetcontent_maxmargin) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory)[LuckyDrawDetailViewModel::class.java]
        viewModel.luckyDrawIndex = arguments?.getInt(LuckyInsideConstant.KEY_LUCKY_DRAW_INDEX)
        viewModel.getProductIndex = arguments?.getInt(LuckyInsideConstant.KEY_GET_PRODUCT_INDEX)
        deviceHeight = arguments?.getInt(LuckyInsideConstant.KEY_DEVICE_HEIGHT) ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_luckydraw_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUi()
        subscribeUi()
        subscribeViewModel()
        requestData()
    }

    private fun initializeUi() {
        setStatusBarHeight()
        initializeScrollView()
    }

    private fun subscribeUi() {
        disposables.add(
            buttonGetActive.clicks()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    containerBottomSheet.visibility = View.VISIBLE
                    rootBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }){
                    viewModel.error.onNext(true)
                }
        )

        disposables.add(
            buttonGetInActive.clicks()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val product = viewModel.product.value
                    if (product?.possibleGetYn == LuckyInsideConstant.COMMON_YN_Y && product.dailyGetCnt == 0) {
                        viewModel.message.onNext(getString(R.string.luckydraw_detail_soldout_popup))
                    } else {
                        viewModel.message.onNext(getString(R.string.luckydraw_detail_inactive))
                    }
                }){
                    viewModel.error.onNext(true)
                }
        )

        disposables.add(
            isBackgroundInactive
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    imageViewInactive.visibility = if (it) View.VISIBLE else View.GONE
                }){
                    viewModel.error.onNext(true)
                }
        )

        disposables.add(
            buttonBottomSheetConfirm.clicks()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (!viewModel.isGetProcessing) {
                        viewModel.isGetProcessing = true
                        val luckyDrawIndex = viewModel.luckyDrawIndex
                        val getProductIndex = viewModel.getProductIndex
                        if (luckyDrawIndex == null || getProductIndex == null) {
                            viewModel.error.onNext(true)
                        } else {
                            viewModel.requestGetGetProduct(luckyDrawIndex, getProductIndex)
                        }
                        logProductGetToAnalytics(getProductIndex, viewModel.product.value?.productName)
                    }
                }){
                    viewModel.error.onNext(true)
                }
        )

        disposables.add(
            imageButtonCopy.clicks()
                .subscribe({
                    copyAddress()
                }){
                    viewModel.error.onNext(true)
                }
        )

        disposables.add(
            imageViewInactive.clicks()
                .subscribe({
                    hideBottomSheet()
                }){
                    viewModel.error.onNext(true)
                }
        )

        disposables.add(
            imageButtonBackArrow.clicks()
                .subscribe({
                    activity!!.onBackPressed()
                }){
                    viewModel.error.onNext(true)
                }
        )
    }

    private fun logProductGetToAnalytics(index: Int?, productName: String? = "") {
        index ?: return

        val bundle = Bundle().apply {
            putInt(FirebaseAnalyticsConst.PARAM_PRODUCT_INDEX, index)
            putString(FirebaseAnalyticsConst.PARAM_PRODUCT_NAME, productName)
        }

        viewModel.requestLogAnalytics(FirebaseAnalyticsConst.EVENT_CLICK_GETLUCKY_GET, bundle)
    }

    private fun subscribeViewModel() {
        disposables.add(
            viewModel.isUnAuthorizedToken
                .subscribe({
                    tokenUnAuthorizedListener.onTokenUnAuthorized()
                }) {
                    viewModel.error.onNext(true)
                }
        )

        disposables.add(
            viewModel.message
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showToast(it)
                }){
                    viewModel.error.onNext(true)
                }
        )

        disposables.add(
            viewModel.error
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showToast(getString(R.string.all_error))
                }){
                    showToast(getString(R.string.all_error))
                }
        )

        disposables.add(
            viewModel.product
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showProductDetail(it)
                }){
                    viewModel.error.onNext(true)
                }
        )

        disposables.add(
            viewModel.isGetProductGet
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it) {
                        processGetFinish()
                    } else {
                        viewModel.error.onNext(true)
                    }
                    viewModel.isGetProcessing = false
                }){
                    viewModel.error.onNext(true)
                    viewModel.isGetProcessing = false
                }
        )

        disposables.add(
            viewModel.isGetProductSoldout
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    processSoldout()
                    viewModel.isGetProcessing = false
                }) {
                    viewModel.error.onNext(true)
                    viewModel.isGetProcessing = false
                }
        )
    }

    private fun requestData() {
        val luckyDrawIndex = viewModel.luckyDrawIndex
        val getProductIndex = viewModel.getProductIndex
        if (luckyDrawIndex == null || getProductIndex == null) {
            viewModel.error.onNext(true)
            return
        }

        disposables.add(viewModel.requestGetProduct(luckyDrawIndex, getProductIndex))
    }

    private fun processSoldout() {
        rootBottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        viewModel.product.value?.dailyGetCnt = 0
        viewModel.product.value?.possibleGetYn = LuckyInsideConstant.COMMON_YN_Y
        switchButton(false)
        showSoldoutPopup()
    }

    private fun setStatusBarHeight() {
        val statusBarHeight = AndroidUtil.getStatusBarHeight(context!!)

        val headerHeight = resources.getDimension(R.dimen.common_header_height)
        containerGradient.layoutParams.height = statusBarHeight + headerHeight.toInt()

        val params = indicator.layoutParams as ConstraintLayout.LayoutParams
        params.topMargin += statusBarHeight
    }

    private fun copyAddress() {
        if (context != null) {
            AndroidUtil.copyText(context!!, textViewAddress.text.toString())
            viewModel.message.onNext(getString(R.string.all_copy))
        }
    }

    private fun showProductDetail(product: LuckyDraw) {
        initializeProductInfo(product)
        initializeCategories(product)
        initializeExchangeInfos(product.exchangePlaceInfo)
        initializeBannerImages(product)
        initializeGetButton(product)
        initializeCaution(product.productCautionList)
        setVenueView(product.exchangePlaceName)
        setTexts(product)
    }

    private fun initializeProductInfo(product: LuckyDraw) {
        setProductInfoView(containerCouponType, textViewCouponType, product.couponType)
        val validText = "${DateUtil.convertValidDateToFormattedDate(product.couponUseTime)}까지"
        setProductInfoView(containerValidDate, textViewValidDate, validText)
        setProductInfoView(containerDeliveryTime, textViewDeliveryTime, product.provisionHour)
        setProductInfoView(containerExchangeWay, textViewExchangeWay, product.provisionMethod)
        setProductInfoView(containerUsageLimit, textViewUsageLimit, product.useLimit)
        setDday(product.couponUseTime)
    }

    private fun setProductInfoView(container: ConstraintLayout, textView: TextView, text: String?) {
        if (text.isNullOrEmpty()) {
            container.visibility = View.GONE
        } else {
            container.visibility = View.VISIBLE
            textView.text = "$text"
        }
    }

    private fun setDday(validDate: String?) {
        if (DateUtil.isInDday(validDate)) {
            val dDay = DateUtil.getDday(validDate)
            if (dDay == DateUtil.NONE_DDAY) {
                containerDday.visibility = View.GONE
            } else {
                if (dDay == 0) {
                    textViewDday.text = "${getString(R.string.myitem_dday_prefix)}${getString(R.string.myitem_dday)}"
                } else {
                    textViewDday.text = "${getString(R.string.myitem_dday_prefix)}$dDay"
                }
                containerDday.visibility = View.VISIBLE
            }

        } else {
            containerDday.visibility = View.GONE
        }
    }

    private fun setTexts(product: LuckyDraw) {
        textViewAddress.text = "${product.address?.split("(")?.get(0)}"
        textViewGetComment.text = "${product.introduceComment}"
        textViewProductName.text = "${product.productName}"
        textViewOriginalPrice.text = DataUtil.getPriceUnitString(product.price)

        val discountPercent = product.discountPercent
        if (discountPercent.isNullOrBlank()) {
            textViewDiscountPercent.visibility = View.GONE
        } else {
            textViewDiscountPercent.text = "$discountPercent%"
        }

        textViewDiscountPrice.text = DataUtil.getPriceUnitString(product.discountPrice)
    }

    private fun setVenueView(venue: String?) {
        if (venue.isNullOrBlank()) {
            containerVenue.visibility = View.GONE
        } else {
            containerVenue.visibility = View.VISIBLE
            textViewVenue.text = venue
        }
    }

    private fun initializeCaution(cautions: List<ProductCaution>?) {
        if (cautions.isNullOrEmpty()) {
            textViewCautionTitle.visibility = View.GONE
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

    private fun initializeGetButton(product: LuckyDraw) {
        val isPossibleGet = (product.possibleGetYn == LuckyInsideConstant.COMMON_YN_Y) && (product.dailyGetCnt ?: 0 > 0)
        switchButton(isPossibleGet)
        if (isPossibleGet) {
            initializeBottomSheet(product)
        }
    }

    private fun switchButton(isPossibleGet: Boolean) {
        if (isPossibleGet) {
            buttonGetActive.visibility = View.VISIBLE
            buttonGetInActive.visibility = View.GONE
        } else {
            buttonGetActive.visibility = View.GONE
            buttonGetInActive.visibility = View.VISIBLE
        }
    }

    private fun initializeBottomSheet(product: LuckyDraw) {
        textViewBottomSheetItemName.text = "${product.productName}"
        textViewBottomSheetExchangeName.text = "${product.exchangePlaceName}"

        rootBottomSheetBehavior = BottomSheetBehavior.from(bottomSheetRoot)
        rootBottomSheetBehavior?.skipCollapsed = true
        rootBottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        rootBottomSheetBehavior?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback(){
            override fun onSlide(p0: View, p1: Float) {
                if (p1 > 0) {
                    setBottomSheetContentMargin(bottomSheetMinMargin + bottomSheetMaxMargin * p1)
                }
                draggingRatio.onNext(p1)
            }

            override fun onStateChanged(p0: View, p1: Int) {
                if (p1 == BottomSheetBehavior.STATE_COLLAPSED || p1 == BottomSheetBehavior.STATE_EXPANDED) {
                    isBackgroundInactive.onNext(true)
                } else if (p1 == BottomSheetBehavior.STATE_HIDDEN) {
                    setBottomSheetContentMargin(bottomSheetMinMargin)
                    isBackgroundInactive.onNext(false)
                }
            }
        })
    }

    private fun initializeScrollView() {
        val statusBarHeight = AndroidUtil.getStatusBarHeight(context!!)
        val headerHeight = resources.getDimension(R.dimen.common_header_height).toInt()

        imageViewHeight = ceil(deviceHeight * 0.63).toInt()

        val imageParams = containerImage.layoutParams
        imageParams.height = imageViewHeight
        containerImage.layoutParams = ConstraintLayout.LayoutParams(imageParams)

        subScrollViewHeight = deviceHeight - (headerHeight + statusBarHeight)

        val subScrollViewParam = scrollViewSub.layoutParams
        subScrollViewParam.height = subScrollViewHeight
        scrollViewSub.layoutParams = FrameLayout.LayoutParams(subScrollViewParam)

        val subScrollableHeight = imageViewHeight - (headerHeight + statusBarHeight)

        scrollViewSub?.isNestedScrollingEnabled = false

        scrollView.setOnScrollChangeListener { _: NestedScrollView?, _: Int, scrollY: Int, _: Int, _: Int ->
            if (scrollY == subScrollableHeight || scrollY > subScrollableHeight) {
                scrollViewSub?.isNestedScrollingEnabled = true
            } else if (scrollY < subScrollableHeight) {
                if (scrollViewSub?.isNestedScrollingEnabled == true) {
                    scrollViewSub?.isNestedScrollingEnabled = false
                }
            }
        }
    }

    private fun initializeBannerImages(product: LuckyDraw) {
        setViewPagerScroller()

        val imageUrls = mutableListOf<String>()
        val views = mutableListOf<View>()

        if (!product.productImageUrl1.isNullOrEmpty()) imageUrls.add(product.productImageUrl1!!)
        if (!product.productImageUrl2.isNullOrEmpty()) imageUrls.add(product.productImageUrl2!!)
        if (!product.productImageUrl3.isNullOrEmpty()) imageUrls.add(product.productImageUrl3!!)
        if (!product.productImageUrl4.isNullOrEmpty()) imageUrls.add(product.productImageUrl4!!)

        bannerAdapter = BannerImageAdapter()

        for ((idx, url) in imageUrls.withIndex()) {
            val view = ImageView(activity)
            view.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(this)
                .load(Uri.parse(url))
                .apply(
                    RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true))
                .into(view)
            if (idx == 0) {
                Glide.with(this)
                    .load(Uri.parse(url))
                    .apply(
                        RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true))
                    .into(imageViewBottomSheetItem)
            }
            views.add(view)
        }

        bannerAdapter.setViews(views)
        viewPager.adapter = bannerAdapter
        indicator.setViewPager(viewPager)

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener{
            override fun onPageScrollStateChanged(state: Int) { }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) { }
            override fun onPageSelected(position: Int) {
                viewPagerHandler.removeCallbacksAndMessages(null)
                val view = bannerAdapter.getItem(currentBannerPosition)
                currentBannerAnim?.cancel()
                view.scaleX = 1.0f
                view.scaleY = 1.0f
                currentBannerPosition = position
                animateImageViews(currentBannerPosition)
                postViewPagerHandler()
            }
        })

        if (bannerAdapter.count > 0) {
            animateImageViews(0)
        }

        if (bannerAdapter.count > 1) {
            postViewPagerHandler()
        }
    }

    private fun animateImageViews(position: Int) {
        val view = bannerAdapter.getItem(position)
        view.pivotX = UiUtil.getViewCenterX(viewPager)
        view.pivotY = UiUtil.getViewCenterY(viewPager)
        currentBannerAnim = view.animate().scaleX(PAGING_ZOOM).scaleY(
            PAGING_ZOOM
        ).setDuration(PAGING_DURATION).setInterpolator(
            LinearInterpolator()
        )
        currentBannerAnim?.start()
    }

    private fun initializeExchangeInfos(exchangeInfos: List<ExchangePlaceInfo>?) {
        if (exchangeInfos.isNullOrEmpty()) {
            containerExchangeInfoSub.visibility = View.GONE
            return
        }

        val itemLayouts = mutableListOf<View>()
        for (exchangeInfo in exchangeInfos) {
            val itemLayout = layoutInflater.inflate(R.layout.item_store_info, null) as ConstraintLayout
            itemLayout.id = View.generateViewId()
            val imageViewItemImage = itemLayout.findViewById<ImageView>(R.id.imageViewInfoIcon)
            if (!exchangeInfo.infoImageUrl.isNullOrBlank()) {
                Glide.with(this)
                    .load(Uri.parse(exchangeInfo.infoImageUrl))

                    .apply(
                        RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true))
                    .into(imageViewItemImage)
            }
            val textViewItemDescription = itemLayout.findViewById<TextView>(R.id.textViewInfoDescription)
            textViewItemDescription.text = exchangeInfo.description
            itemLayouts.add(itemLayout)
            containerExchangeInfoSub.addView(itemLayout)
        }

        val constraintSet = ConstraintSet()

        for ((index, itemLayout) in itemLayouts.withIndex()) {
            constraintSet.constrainWidth(itemLayout.id, ConstraintSet.MATCH_CONSTRAINT)
            constraintSet.constrainHeight(itemLayout.id, ConstraintSet.WRAP_CONTENT)
            if (index != 0) {
                constraintSet.connect(itemLayout.id, ConstraintSet.TOP, itemLayouts[index - 1].id, ConstraintSet.BOTTOM, UiUtil.dpToPx(activity!!, 9f))
            }
        }

        constraintSet.applyTo(containerExchangeInfoSub)
    }

    private fun makeCategories(product: LuckyDraw) : List<Category> {
        val categories = mutableListOf<Category>()
        // 지역 1개
        if (!product.regionName.isNullOrBlank() && !product.regionImageUrl.isNullOrBlank()) {
            val regionCategory = Category(
                categoryName = product.regionName,
                categoryImageUrl = product.regionImageUrl
            )
            categories.add(regionCategory)
        }
        // 대분류 1개
        if (!product.categoryName.isNullOrBlank() && !product.categoryImageUrl.isNullOrBlank()) {
            val storeCategory = Category(
                categoryName = product.categoryName,
                categoryImageUrl = product.categoryImageUrl
            )
            categories.add(storeCategory)
        }
        // 오감 1개
        if (!product.categoryFiveSensesName.isNullOrBlank() && !product.categoryFiveSenseImageUrl.isNullOrBlank()) {
            val senseCategory = Category(
                categoryName = product.categoryFiveSensesName,
                categoryImageUrl = product.categoryFiveSenseImageUrl
            )
            categories.add(senseCategory)
        }
        // 감정 1개
        if (!product.categoryEmotionName.isNullOrBlank() && !product.categoryEmotionImageUrl.isNullOrBlank()) {
            val emotionCategory = Category(
                categoryName = product.categoryEmotionName,
                categoryImageUrl = product.categoryEmotionImageUrl
            )
            categories.add(emotionCategory)
        }
        return categories
    }

    private fun initializeCategories(product: LuckyDraw) {
        val categories = makeCategories(product)

        if (categories.isNullOrEmpty()) {
            return
        }

        val itemLayouts = mutableListOf<View>()
        for ((idx, category) in categories.withIndex()) {
            if (idx == 4) {
                break
            }
            val itemLayout = layoutInflater.inflate(R.layout.item_category, null) as ConstraintLayout
            itemLayout.id = View.generateViewId()

            val imageViewItemImage = itemLayout.findViewById<ImageView>(R.id.imageViewCategoryIcon)
            if (!category.categoryImageUrl.isNullOrBlank()) {
                Glide.with(this)
                    .load(Uri.parse(category.categoryImageUrl))
                    .apply(
                        RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true))
                    .into(imageViewItemImage)
            }

            val textViewItemName = itemLayout.findViewById<TextView>(R.id.textViewCategoryName)
            textViewItemName.text = category.categoryName
            itemLayouts.add(itemLayout)
            containerProductCategory.addView(itemLayout)
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

        constraintSet.applyTo(containerProductCategory)
    }

    private fun setBottomSheetContentMargin(margin: Float) {
        val params = containerBottomSheetContent.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(0, margin.toInt(), 0, UiUtil.dpToPx(context!!, -30f))
        containerBottomSheetContent.requestLayout()
    }

    private fun processGetFinish () {
        val fragmentManager = activity?.supportFragmentManager
        fragmentManager?.popBackStack(fragmentManager.getBackStackEntryAt(1).id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        val product = viewModel.product.value
        product ?: return
        requestListener.onRequestGetProductFinish(product.exchangePlaceName, product.productName, getProductImageUrl(product))
    }

    private fun getProductImageUrl(product: LuckyDraw): String? {
        if (!product.productImageUrl1.isNullOrBlank()) return product.productImageUrl1
        if (!product.productImageUrl2.isNullOrBlank()) return product.productImageUrl2
        if (!product.productImageUrl3.isNullOrBlank()) return product.productImageUrl3
        if (!product.productImageUrl4.isNullOrBlank()) return product.productImageUrl4
        return null
    }

    fun hideBottomSheet() {
        if (isBottomSheetVisible()) {
            rootBottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    fun isBottomSheetVisible() : Boolean {
        if (rootBottomSheetBehavior == null) {
            return false
        } else {
            return rootBottomSheetBehavior?.state != BottomSheetBehavior.STATE_HIDDEN
        }
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

    private fun showSoldoutPopup() {
        containerSoldoutPopup.pivotX = containerSoldoutPopup.measuredWidth.toFloat() / 2
        containerSoldoutPopup.pivotY = containerSoldoutPopup.measuredHeight.toFloat() / 2
        containerSoldoutPopup.visibility = View.VISIBLE
        animateSoldoutPopup()
    }

    private fun animateSoldoutPopup() {
        val animSet = AnimationUtil.getPopupAnimatorSet(containerSoldoutPopup)
        animSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationStart(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                containerSoldoutPopup?.visibility = View.INVISIBLE
                containerSoldoutPopup?.alpha = 1f
            }
        })
        animSet.start()
    }

    private fun showNextImage() {
        val curPos = viewPager.currentItem
        val nextPos = if (curPos == bannerAdapter.count - 1) 0 else curPos + 1
        viewPager.setCurrentItem(nextPos, false)
    }

    override fun onDestroyView() {
        viewPagerHandler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }
}