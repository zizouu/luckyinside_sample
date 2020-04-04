package com.lhbros.luckyinside.daily.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.lhbros.luckyinside.R
import com.lhbros.luckyinside.daily.model.DailyBanner
import com.lhbros.luckyinside.daily.view.listener.OnDailyBannerClickListener

class DailyBannerAdapter(
    val context: Context?,
    val dailyBanners: MutableList<DailyBanner>,
    val listener: OnDailyBannerClickListener
) : PagerAdapter() {
    private val views: Array<View?> = arrayOfNulls(dailyBanners.size)

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = container.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.item_daily_banner, container, false)
        views[position] = view
        container.addView(view)
        showBanner(views[position], position)
        return views[position] ?: view
    }

    private fun showBanner(view: View?, position: Int) {
        view ?: return
        val rootView = view.findViewById<ConstraintLayout>(R.id.containerRoot)
        val banner = dailyBanners[position]
        rootView.setOnClickListener { listener.onDailyBannerClick(banner) }
        val imageViewBanner = view.findViewById<ImageView>(R.id.imageViewBanner)
        if (!banner.bannerImage.isNullOrBlank()) {
            Glide.with(context!!)
                .load(banner.bannerImage)
                .apply(
                    RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true))
                .into(imageViewBanner)
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        container.removeView(obj as View)
    }

    override fun isViewFromObject(view: View, obj: Any) = view == obj as View
    override fun getCount() : Int = dailyBanners.size

    fun getView(position: Int): View? {
        if (position > count || position == count) {
            return null
        } else {
            return views[position]
        }
    }
}