package com.lhbros.luckyinside.daily.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.lhbros.luckyinside.R
import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.util.UiUtil
import com.lhbros.luckyinside.common.view.listener.OnIntervalClickListener
import com.lhbros.luckyinside.daily.model.DailyVenue
import com.lhbros.luckyinside.map.model.Venue
import com.lhbros.luckyinside.common.view.decoration.RecyclerViewGridDecoration
import com.lhbros.luckyinside.daily.view.listener.OnDailyVenueClickListener

class DailyVenueAdapter(
    val listener: OnDailyVenueClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var freeExperienceVenues = listOf<DailyVenue>()
    var hotPlaceVenues = listOf<DailyVenue>()
    private var itemStartPosition = 3
    private val ITEM_TYPE_FREE_EXPERIENCE_TITLE = 0
    private val ITEM_TYPE_FREE_EXPERIENCE_LIST = 1
    private val ITEM_TYPE_HOT_PLACE_TITLE = 2
    private val ITEM_TYPE_HOT_PLACE_LIST = 3

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_TYPE_FREE_EXPERIENCE_TITLE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_daily_venue_freeexperience_title, parent, false)
                DailyVenueFreeExperienceTitleViewHolder(
                    view,
                    parent.context
                )
            }
            ITEM_TYPE_FREE_EXPERIENCE_LIST -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_daily_venue_freeexperience, parent, false)
                val holder =
                    DailyVenueFreeExperienceViewHolder(
                        view,
                        parent.context
                    )
                val recyclerView = holder.recyclerView
                recyclerView.layoutManager = LinearLayoutManager(parent.context, LinearLayoutManager.HORIZONTAL, false)
                recyclerView.itemAnimator = null
                recyclerView.addItemDecoration(
                    RecyclerViewGridDecoration(
                        0,
                        UiUtil.dpToPx(parent.context, 14f),
                        isAllSide = false
                    )
                )
                return holder
            }
            ITEM_TYPE_HOT_PLACE_TITLE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_daily_venue_hotplace_title, parent, false)
                DailyVenueHotPlaceTitleViewHolder(
                    view,
                    parent.context
                )
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_daily_venue, parent, false)
                DailyVenueViewHotPlaceHolder(
                    view,
                    parent.context
                )
            }
        }
    }

    // 무료 타이틀 - 1
    // 무료 horizontal scroll - 1
    // 핫플 타이틀 - 1
    // 핫플 grid view - 다수
    override fun getItemCount(): Int = hotPlaceVenues.size + itemStartPosition

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            ITEM_TYPE_FREE_EXPERIENCE_TITLE -> {
                showFreeExperienceTitle(holder)
            }
            ITEM_TYPE_FREE_EXPERIENCE_LIST -> {
                showFreeExperienceList(holder)
            }
            ITEM_TYPE_HOT_PLACE_TITLE -> {
                showHotPlaceTitle(holder)
            }
            ITEM_TYPE_HOT_PLACE_LIST -> {
                showHotPlaceItem(holder, position)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val itemType: Int
        if (freeExperienceVenues.isNullOrEmpty()) {
            itemType = when (position) {
                0 -> ITEM_TYPE_HOT_PLACE_TITLE
                else -> ITEM_TYPE_HOT_PLACE_LIST
            }
        } else if (hotPlaceVenues.isNullOrEmpty()) {
            itemType = when (position) {
                0 ->  ITEM_TYPE_FREE_EXPERIENCE_TITLE
                else -> ITEM_TYPE_FREE_EXPERIENCE_LIST
            }
        } else {
            itemType = when(position) {
                0 -> ITEM_TYPE_FREE_EXPERIENCE_TITLE
                1 -> ITEM_TYPE_FREE_EXPERIENCE_LIST
                2 -> ITEM_TYPE_HOT_PLACE_TITLE
                else -> ITEM_TYPE_HOT_PLACE_LIST
            }
        }
        return itemType
    }

    fun setData(venues: List<DailyVenue>) {
        freeExperienceVenues = venues.filter { it.openYn == LuckyInsideConstant.COMMON_YN_Y }
        hotPlaceVenues = venues.filter { it.openYn == LuckyInsideConstant.COMMON_YN_N }
        itemStartPosition = 3
        if (freeExperienceVenues.isNullOrEmpty()) {
            itemStartPosition -= 2
        }
        if (hotPlaceVenues.isNullOrEmpty()) {
            itemStartPosition = 2
        }
        notifyDataSetChanged()
    }

    private fun showFreeExperienceTitle(holder: RecyclerView.ViewHolder) {
        with (holder as DailyVenueFreeExperienceTitleViewHolder) {
            containerRoot.visibility = View.VISIBLE
            containerSeeAll.setOnClickListener { listener.onDailyFreeExperienceSeeAll() }
        }
    }

    private fun showFreeExperienceList(holder: RecyclerView.ViewHolder) {
        with (holder as DailyVenueFreeExperienceViewHolder) {
            containerRoot.visibility = View.VISIBLE
            val freeExperienceAdapter =
                DailyFreeExperienceAdapter(listener)
            recyclerView.adapter = freeExperienceAdapter
            freeExperienceAdapter.venues = freeExperienceVenues
        }
    }

    private fun showHotPlaceTitle(holder: RecyclerView.ViewHolder) {
        with (holder as DailyVenueHotPlaceTitleViewHolder) {
            containerRoot.visibility = View.VISIBLE
        }
    }

    private fun showHotPlaceItem(holder: RecyclerView.ViewHolder, position: Int) {
        with (holder as DailyVenueViewHotPlaceHolder) {
            if (hotPlaceVenues.isNullOrEmpty()) {
                containerRoot.visibility = View.GONE
            } else {
                containerRoot.visibility = View.VISIBLE
                val realItemPosition = position - itemStartPosition
                val sidePadding = UiUtil.dpToPx(context, 22f)
                val itemPadding = UiUtil.dpToPx(context, 6.5f)
                if (realItemPosition % 2 == 0) {
                    containerRoot.setPadding(sidePadding, 0, itemPadding, 0)
                } else {
                    containerRoot.setPadding(itemPadding, 0, sidePadding, 0)
                }
                val venue = hotPlaceVenues[realItemPosition]

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
                // 마이베뉴 등록, 해제
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
    }

    private fun getBannerImageUrl(dailyVenue: DailyVenue): String? {
        if (!dailyVenue.bannerImageUrl1.isNullOrBlank()) return dailyVenue.bannerImageUrl1
        if (!dailyVenue.bannerImageUrl2.isNullOrBlank()) return dailyVenue.bannerImageUrl2
        if (!dailyVenue.bannerImageUrl3.isNullOrBlank()) return dailyVenue.bannerImageUrl3
        if (!dailyVenue.bannerImageUrl4.isNullOrBlank()) return dailyVenue.bannerImageUrl4
        return null
    }

    class DailyVenueFreeExperienceViewHolder(
        view: View,
        val context: Context
    ) : RecyclerView.ViewHolder(view) {
        val containerRoot by lazy { view.findViewById<ConstraintLayout>(R.id.containerRoot) }
        val recyclerView by lazy { view.findViewById<RecyclerView>(R.id.recyclerView) }
    }

    class DailyVenueFreeExperienceTitleViewHolder(
        view: View,
        val context: Context
    ) : RecyclerView.ViewHolder(view) {
        val containerRoot by lazy { view.findViewById<ConstraintLayout>(R.id.containerRoot) }
        val textViewSeeAll by lazy { view.findViewById<TextView>(R.id.textViewSeeAll) }
        val containerSeeAll by lazy { view.findViewById<ConstraintLayout>(R.id.containerSeeAll) }
    }

    class DailyVenueViewHotPlaceHolder(
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
    }

    class DailyVenueHotPlaceTitleViewHolder(
        view: View,
        val context: Context
    ) : RecyclerView.ViewHolder(view) {
        val containerRoot by lazy { view.findViewById<ConstraintLayout>(R.id.containerRoot) }
    }
}