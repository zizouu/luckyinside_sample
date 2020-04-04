package com.lhbros.luckyinside.luckydraw.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.lhbros.luckyinside.R
import com.lhbros.luckyinside.common.util.DataUtil
import com.lhbros.luckyinside.common.util.UiUtil
import com.lhbros.luckyinside.luckydraw.view.listener.OnLuckyDrawClickListener
import com.lhbros.luckyinside.luckydraw.model.LuckyDraw

class LuckyDrawAdapter(
    val listener: OnLuckyDrawClickListener,
    val deviceHeight: Int?,
    val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var products: MutableList<LuckyDraw> = mutableListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_luckydraw, parent, false)
        return LuckyDrawViewHolder(
            view,
            parent.context
        )
    }

    override fun getItemCount(): Int = products.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        with (holder as LuckyDrawViewHolder) {
            if (getItemViewType(position) == 0 || getItemViewType(position) == 1) {
                val params = containerRoot.layoutParams as RecyclerView.LayoutParams
                params.topMargin = UiUtil.dpToPx(context, 561f)
            }

            val product = products[position]
            val imageUrl = getProductImageUrl(product)
            if (!imageUrl.isNullOrBlank()) {
                Glide.with(context)
                    .load(imageUrl)
                    .apply(RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true))
                    .into(imageViewProduct)
            }
            val remainCount = product.dailyGetCnt
            if (remainCount == null || remainCount == 0) {
                containerRemain.visibility = View.GONE
                containerPrice.visibility = View.GONE
                containerSoldout.visibility = View.VISIBLE
            } else {
                containerRemain.visibility = View.VISIBLE
                containerPrice.visibility = View.VISIBLE
                containerSoldout.visibility = View.GONE
                textViewRegionName.text = "${product.regionName}"
                textViewProductName.text = "${product.productName}"
                textViewExchange.text = "${product.exchangePlaceName}"
                textViewOriginalPrice.text = DataUtil.getPriceUnitString(product.price)
                textViewDiscountPrice.text = DataUtil.getPriceUnitString(product.discountPrice)
            }
            containerRoot.setOnClickListener {
                listener.onProductClickListener(product.productIndex, product.productName)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    private fun getProductImageUrl(product: LuckyDraw): String? {
        if (!product.productImageUrl1.isNullOrBlank()) return product.productImageUrl1
        if (!product.productImageUrl2.isNullOrBlank()) return product.productImageUrl2
        if (!product.productImageUrl3.isNullOrBlank()) return product.productImageUrl3
        if (!product.productImageUrl4.isNullOrBlank()) return product.productImageUrl4
        return null
    }

    class LuckyDrawViewHolder(
        view: View,
        val context: Context
    ) : RecyclerView.ViewHolder(view) {
        var product: LuckyDraw? = null
        val containerRoot by lazy { view.findViewById<ConstraintLayout>(R.id.containerRoot) }
        val imageViewProduct by lazy { view.findViewById<ImageView>(R.id.imageViewProduct) }
        val textViewRegionName by lazy { view.findViewById<TextView>(R.id.textViewRegionName) }
        val textViewProductName by lazy { view.findViewById<TextView>(R.id.textViewProductName) }
        val textViewExchange by lazy { view.findViewById<TextView>(R.id.textViewExchange) }
        val containerRemain by lazy { view.findViewById<ConstraintLayout>(R.id.containerExchange) }
        val containerSoldout by lazy { view.findViewById<ConstraintLayout>(R.id.containerSoldout) }
        val containerPrice by lazy { view.findViewById<ConstraintLayout>(R.id.containerPrice) }
        val textViewOriginalPrice by lazy { view.findViewById<TextView>(R.id.textViewOriginalPrice) }
        val textViewDiscountPrice by lazy { view.findViewById<TextView>(R.id.textViewDiscountPrice) }
    }
}