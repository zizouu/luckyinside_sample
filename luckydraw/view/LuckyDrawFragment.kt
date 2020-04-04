package com.lhbros.luckyinside.luckydraw.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.jakewharton.rxbinding3.view.clicks
import com.lhbros.luckyinside.R
import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.constant.FirebaseAnalyticsConst
import com.lhbros.luckyinside.common.util.AndroidUtil
import com.lhbros.luckyinside.common.util.UiUtil
import com.lhbros.luckyinside.map.view.MapBaseFragment
import com.lhbros.luckyinside.luckydraw.view.adapter.LuckyDrawAdapter
import com.lhbros.luckyinside.common.view.decoration.RecyclerViewGridDecoration
import com.lhbros.luckyinside.luckydraw.view.listener.OnLuckyDrawClickListener
import com.lhbros.luckyinside.luckydraw.viewmodel.LuckyDrawViewModel
import com.lhbros.luckyinside.luckydraw.viewmodel.LuckyDrawViewModelFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_luckydraw.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.ceil

class LuckyDrawFragment : MapBaseFragment(),
    OnLuckyDrawClickListener, SwipeRefreshLayout.OnRefreshListener {
    companion object {
        const val TAG = "getFragment"
    }

    @Inject
    lateinit var viewModelFactory: LuckyDrawViewModelFactory
    lateinit var viewModel: LuckyDrawViewModel

    private var deviceHeight: Int? = null
    private var productAdapter: LuckyDrawAdapter? = null
    private var recyclerViewScrollPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory)[LuckyDrawViewModel::class.java]
        viewModel.luckyDrawIndex = arguments?.getInt(LuckyInsideConstant.KEY_LUCKY_DRAW_INDEX)
        deviceHeight = arguments?.getInt(LuckyInsideConstant.KEY_DEVICE_HEIGHT, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_luckydraw, container, false)
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
        setStatusBarMargin(containerPopHeaderBar)
        setStatusBarMargin(containerPopRound)
        setStatusBarMargin(containerBackgroundHayo)
        setStatusBarMargin(swipeRefreshLayout)
        initializeRecyclerView()
        initializeRefreshView()
    }

    private fun subscribeUi() {
        disposables.add(
            imageViewPopHeaderBack.clicks()
                .subscribe({
                    activity?.onBackPressed()
                }) {
                    viewModel.error.onNext(true)
                }
        )
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
                .subscribe({
                    showToast(it)
                }) {
                    viewModel.error.onNext(true)
                }
        )
        disposables.add(
            viewModel.error
                .filter { it }
                .subscribe({
                    swipeRefreshLayout.isRefreshing = false
                    showToast(getString(R.string.all_error))
                }) {
                    showToast(getString(R.string.all_error))
                }
        )
        disposables.add(
            viewModel.isUnAuthorizedToken
                .filter { it }
                .subscribe({
                    tokenUnAuthorizedListener.onTokenUnAuthorized()
                }) {
                    viewModel.error.onNext(true)
                }
        )
        disposables.add(
            viewModel.products
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    swipeRefreshLayout.isRefreshing = false
                    recyclerView.setPadding(0, 0, 0, getRecyclerViewMarginBottom(it.size))
                    productAdapter?.products = it.toMutableList()
                }) {
                    swipeRefreshLayout.isRefreshing = false
                    viewModel.error.onNext(true)
                }
        )
    }

    private fun requestData() {
        val luckyDrawIndex = viewModel.luckyDrawIndex
        if (luckyDrawIndex == null) {
            viewModel.error.onNext(true)
            return
        }
        disposables.add(viewModel.requestProducts(luckyDrawIndex))
    }

    private fun getRecyclerViewMarginBottom(productCount: Int): Int {
        val remainSpaceHeight = deviceHeight!!.toFloat() - UiUtil.dpToPx(context!!, 105f) - AndroidUtil.getStatusBarHeight(context!!)
        val rowCount = ceil(productCount / 2f)
        val rowHeight = ((AndroidUtil.getDeviceWidth(context!!) - UiUtil.dpToPx(context!!, 68f)) / 2) * (3f / 2f) + UiUtil.dpToPx(context!!, 16f)
        if (remainSpaceHeight > (rowCount * rowHeight)) {
            return (remainSpaceHeight - (rowCount * rowHeight)).toInt()
        } else {
            return 0
        }
    }

    private fun initializeRecyclerView() {
        recyclerViewScrollPosition = 0
        val alphaStartPosition = 0
        val alphaEndPosition = AndroidUtil.getDeviceHeight(context!!) / 3

        productAdapter = LuckyDrawAdapter(
            this,
            deviceHeight,
            context!!
        )
        val gridLayoutManager = GridLayoutManager(context!!, 2)
        recyclerView.layoutManager = gridLayoutManager
        recyclerView.adapter = productAdapter
        recyclerView.itemAnimator = null
        recyclerView.addItemDecoration(
            RecyclerViewGridDecoration(
                resources.getDimension(
                    R.dimen.luckyu_main_tobeopend_divide_height
                ).toInt(),
                resources.getDimension(
                    R.dimen.get_product_divide_side
                ).toInt()
            )
        )

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                recyclerViewScrollPosition += dy
                val alphaTotalDiff = (alphaEndPosition - alphaStartPosition).toFloat()
                val currentAlphaDiff = (recyclerViewScrollPosition - alphaStartPosition).toFloat()
                val alphaRatio = currentAlphaDiff / alphaTotalDiff
                if (recyclerViewScrollPosition > alphaStartPosition && recyclerViewScrollPosition < alphaEndPosition) {
                    containerPopHeader?.alpha = alphaRatio
                    viewRedGradient?.alpha = 1f - alphaRatio
                    containerBackgroundHayo?.alpha = 1f - alphaRatio
                } else if (recyclerViewScrollPosition < alphaStartPosition || recyclerViewScrollPosition == alphaStartPosition) {
                    containerPopHeader?.alpha = 0f
                    viewRedGradient?.alpha = 1f
                    containerBackgroundHayo?.alpha = 1f
                } else if (recyclerViewScrollPosition > alphaEndPosition || recyclerViewScrollPosition == alphaEndPosition) {
                    containerPopHeader?.alpha = 1f
                    viewRedGradient?.alpha = 0f
                    containerBackgroundHayo?.alpha = 0f
                }
            }
        })
    }

    private fun initializeRefreshView() {
        val pullDistance = UiUtil.dpToPx(context!!, 56f) + AndroidUtil.getStatusBarHeight(context!!)
        swipeRefreshLayout.setProgressViewOffset(false, 0, pullDistance)
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

    override fun onProductClickListener(index: Int?, productName: String?) {
        val luckyDrawIndex = viewModel.luckyDrawIndex
        if (index == null || luckyDrawIndex == null) {
            viewModel.error.onNext(true)
            return
        }

        logProductClickToAnalytics(index, productName)
        requestListener.onRequestGetProductDetail(luckyDrawIndex, index)
    }

    private fun logProductClickToAnalytics(index: Int, productName: String? = "") {
        val bundle = Bundle().apply {
            putInt(FirebaseAnalyticsConst.PARAM_PRODUCT_INDEX, index)
            putString(FirebaseAnalyticsConst.PARAM_PRODUCT_NAME, productName)
        }

        viewModel.requestLogAnalytics(FirebaseAnalyticsConst.EVENT_CLICK_GETLUCKY_CARD, bundle)
    }

    override fun onRefresh() {
        requestData()
    }
}