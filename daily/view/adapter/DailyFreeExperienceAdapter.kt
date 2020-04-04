package com.lhbros.luckyinside.daily.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.lhbros.luckyinside.R
import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.util.AndroidUtil
import com.lhbros.luckyinside.common.util.UiUtil
import com.lhbros.luckyinside.common.view.listener.OnIntervalClickListener
import com.lhbros.luckyinside.daily.model.DailyVenue
import com.lhbros.luckyinside.daily.view.listener.OnDailyVenueClickListener

class DailyFreeExperienceAdapter(
    val listener: OnDailyVenueClickListener
) : RecyclerView.Adapter<DailyVenueAdapter.DailyVenueViewHotPlaceHolder>() {
    var venues: List<DailyVenue> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyVenueAdapter.DailyVenueViewHotPlaceHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_daily_venue, parent, false)
        // view 사이즈 조절. layout 내부에 2:3 비율있어 width만 지정해주면된다.
        val params = view.layoutParams as RecyclerView.LayoutParams
        val paddingSpace = UiUtil.dpToPx(parent.context, 57f)
        params.width = (AndroidUtil.getDeviceWidth(parent.context) - paddingSpace) / 2
        return DailyVenueAdapter.DailyVenueViewHotPlaceHolder(
            view,
            parent.context
        )
    }

    override fun getItemCount(): Int = venues.size

    override fun onBindViewHolder(holder: DailyVenueAdapter.DailyVenueViewHotPlaceHolder, position: Int) {
        showItem(holder, position)
    }

    override fun getItemViewType(position: Int): Int = position

    private fun showItem(holder: DailyVenueAdapter.DailyVenueViewHotPlaceHolder, position: Int) {
        with (holder) {
            val venue = venues[position]
            val imageUrl = getBannerImageUrl(venue)
            if (!imageUrl.isNullOrBlank()) {
                Glide.with(context)
                    .load(imageUrl)
                    .apply(
                        RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true))
                    .into(imageViewVenue)
            }
            // 마이베뉴 등록/해제
            imageButtonMyVenue.setOnClickListener(object : OnIntervalClickListener() {
                override fun onIntervalClick() {
                    super.onIntervalClick()
                    val index = when (venue.division) {
                        LuckyInsideConstant.DAILY_VENUE_TYPE_STORE -> venue.storeIndex
                        LuckyInsideConstant.DAILY_VENUE_TYPE_LUCKYU -> venue.luckyuIndex
                        else -> null
                    }
                    // 현재 마이 베뉴가 아니면 등록동작으로 전달
                    val isRegist = venue.myRegistrationYn == LuckyInsideConstant.COMMON_YN_N
                    listener.onDailyMyVenueClick(venue.division, index, isRegist)
                    venue.myRegistrationYn = if (isRegist) LuckyInsideConstant.COMMON_YN_Y else LuckyInsideConstant.COMMON_YN_N
                    imageButtonMyVenue.setImageResource(if (isRegist) R.drawable.ic_mystore_green else R.drawable.ic_mystore_white)
                }
            })
            // 아이템 클릭
            containerRoot.setOnClickListener(object : OnIntervalClickListener() {
                override fun onIntervalClick() {
                    super.onIntervalClick()
                    val index = when (venue.division) {
                        LuckyInsideConstant.DAILY_VENUE_TYPE_STORE -> venue.storeIndex
                        LuckyInsideConstant.DAILY_VENUE_TYPE_LUCKYU -> venue.luckyuIndex
                        else -> null
                    }
                    listener.onDailyVenueClick(venue.division, index)
                }
            })
            // 마이베뉴 처리
            imageButtonMyVenue.setImageResource(if (venue.myRegistrationYn == LuckyInsideConstant.COMMON_YN_Y) R.drawable.ic_mystore_green else R.drawable.ic_mystore_white)
            // 베뉴 정보
            textViewVenueCategory.text = "${venue.categoryCode?.toUpperCase()}"
            textViewVenueTitle.text = "${venue.title}"
            textViewVenueName.text = "${venue.name}"
        }
    }

    private fun getBannerImageUrl(dailyVenue: DailyVenue): String? {
        if (!dailyVenue.bannerImageUrl1.isNullOrBlank()) return dailyVenue.bannerImageUrl1
        if (!dailyVenue.bannerImageUrl2.isNullOrBlank()) return dailyVenue.bannerImageUrl2
        if (!dailyVenue.bannerImageUrl3.isNullOrBlank()) return dailyVenue.bannerImageUrl3
        if (!dailyVenue.bannerImageUrl4.isNullOrBlank()) return dailyVenue.bannerImageUrl4
        return null
    }


    /*class DailyVenueViewHotPlaceHolder(
        view: View,
        val context: Context
    ) : RecyclerView.ViewHolder(view) {
        var venue: Venue? = null
        val containerRoot by lazy { view.findViewById<ConstraintLayout>(R.id.containerRoot) }
        val containerVenueOpened by lazy { view.findViewById<ConstraintLayout>(R.id.containerVenueOpened) }
        val containerVenueClosed by lazy { view.findViewById<ConstraintLayout>(R.id.containerVenueClosed) }
        val imageViewVenue by lazy { view.findViewById<ImageView>(R.id.imageViewVenue) }
        val imageButtonMyVenue by lazy { view.findViewById<ImageButton>(R.id.imageButtonMyVenue) }
        val textViewVenueCategory by lazy { view.findViewById<TextView>(R.id.textViewVenueCategory) }
        val textViewVenueTitle by lazy { view.findViewById<TextView>(R.id.textViewVenueTitle) }
        val textViewVenueName by lazy { view.findViewById<TextView>(R.id.textViewVenueName) }
        val textViewOpenTime by lazy { view.findViewById<TextView>(R.id.textViewOpenTime) }
    }*/
}