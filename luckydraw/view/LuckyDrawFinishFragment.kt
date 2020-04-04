package com.lhbros.luckyinside.luckydraw.view

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.jakewharton.rxbinding3.view.clicks
import com.lhbros.luckyinside.R
import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.util.AndroidUtil
import com.lhbros.luckyinside.common.util.UiUtil
import com.lhbros.luckyinside.map.view.MapBaseFragment
import com.lhbros.luckyinside.share.view.ShareDialogFragment
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_luckydraw_finish.*
import timber.log.Timber

class LuckyDrawFinishFragment : MapBaseFragment() {
    companion object {
        const val TAG = "getProductFinishFragment"
    }

    private var exchangePlaceName: String? = null
    private var productName: String? = null
    private var productImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exchangePlaceName = arguments?.getString(LuckyInsideConstant.KEY_EXCHANGE_PLACE_NAME)
        productName = arguments?.getString(LuckyInsideConstant.KEY_GET_PRODUCT_NAME)
        productImageUrl = arguments?.getString(LuckyInsideConstant.KEY_GET_PRODUCT_IMAGE_URL)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_luckydraw_finish, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUi()
        subscribeUi()
    }

    private fun initializeUi() {
        setStatusBarHeight()
        showProduct()
        animateItemView()
    }

    private fun subscribeUi() {
        disposables.add(
            imageButtonShare.clicks()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .flatMap {
                    Observable.fromCallable {
                        val shareBitmap = makeShareBitmap()
                        UiUtil.getBitmapUri(activity!!, shareBitmap, 100)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val dialog = ShareDialogFragment()
                    val bundle = Bundle()
                    bundle.putString(LuckyInsideConstant.KEY_SHARE_TYPE, LuckyInsideConstant.KEY_SHARE_TYPE_IMAGE)
                    bundle.putParcelable(LuckyInsideConstant.KEY_SHARE_IMAGE_URI, it!!)
                    dialog.arguments = bundle
                    dialog.setTargetFragment(this, LuckyInsideConstant.REQ_CODE_DIALOG)
                    dialog.show(activity!!.supportFragmentManager,
                        ShareDialogFragment.TAG
                    )
                }){
                    Timber.d("share uri error ${it.message}")
                }
        )
        disposables.add(
            imageButtonDaily.clicks()
                .subscribe({
                    activity?.onBackPressed()
                }) {
                    showToast(getString(R.string.all_error))
                }
        )
        disposables.add(
            imageButtonMyItem.clicks()
                .subscribe({
                    activity?.onBackPressed()
                    requestListener.onRequestMyItem()
                }) {
                    showToast(getString(R.string.all_error))
                }
        )
    }

    private fun setStatusBarHeight() {
        val statusBarHeight = AndroidUtil.getStatusBarHeight(context!!)
        val params = containerHeader.layoutParams as ConstraintLayout.LayoutParams
        params.topMargin = statusBarHeight
    }

    private fun animateItemView() {
        val itemSize = imageViewProduct.layoutParams.height
        val imageScaleFrom = 1.0f
        val imageScaleTo = 0.95f

        val imageScaleAnim = ScaleAnimation(imageScaleFrom, imageScaleTo, imageScaleFrom, imageScaleTo, itemSize / 2f, itemSize / 2f)
        imageScaleAnim.duration = 1000
        imageScaleAnim.repeatCount = Animation.INFINITE
        imageScaleAnim.repeatMode = Animation.REVERSE

        textViewProduct.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val textWidth = textViewProduct.measuredWidth
        val textHeight = textViewProduct.measuredHeight

        val textScaleFrom = 1.0f
        val textScaleTo = 0.975f

        val textScaleAnim = ScaleAnimation(textScaleFrom, textScaleTo, textScaleFrom, textScaleTo, textWidth / 2f, textHeight / 2f)
        textScaleAnim.duration = 1000
        textScaleAnim.repeatCount = Animation.INFINITE
        textScaleAnim.repeatMode = Animation.REVERSE

        imageViewProduct.startAnimation(imageScaleAnim)
        textViewProduct.startAnimation(textScaleAnim)
    }

    private fun showProduct() {
        textViewProduct.text = "$productName"
        textViewStore.text = "$exchangePlaceName"
        if (!productImageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(productImageUrl)
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true))
                .into(imageViewProduct)
        }
    }

    private fun makeShareBitmap() : Bitmap {
        val shareLayout = layoutInflater.inflate(R.layout.layout_share_get, null) as ConstraintLayout

        val height = AndroidUtil.getDeviceHeight(context!!)
        val width = AndroidUtil.getDeviceWidth(context!!)

        shareLayout.layoutParams = ViewGroup.LayoutParams(width, height)

        val textViewStoreFrom = shareLayout.findViewById<TextView>(R.id.textViewStoreFrom)
        val textViewProduct = shareLayout.findViewById<TextView>(R.id.textViewProduct)
        val imageViewProduct = shareLayout.findViewById<ImageView>(R.id.imageViewProduct)

        textViewStoreFrom.text = "$exchangePlaceName"
        textViewProduct.text = "$productName"

        if (!productImageUrl.isNullOrBlank()) {
            val bitmap = UiUtil.getBitmapByGlide(this, Uri.parse(productImageUrl))
            imageViewProduct.setImageBitmap(bitmap)
        }

        return UiUtil.getViewBitmap(shareLayout)
    }
}