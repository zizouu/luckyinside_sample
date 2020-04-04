package com.lhbros.luckyinside.store.model

import com.lhbros.luckyinside.common.model.Category
import com.lhbros.luckyinside.common.model.EmotionCategory
import com.lhbros.luckyinside.common.model.FiveSensesCategory
import com.lhbros.luckyinside.map.model.*
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import java.io.Serializable

data class Store(
    @JvmField var storeIndex: Int? = null,
    @JvmField var userIndex: Int? = null,
    @JvmField var regionIndex: Int? = null,
    @JvmField var storeId: String? = null,
    @JvmField var storeName: String? = null,
    @JvmField var latitude: String? = null,
    @JvmField var longitude: String? = null,
    @JvmField var arPerception: String? = null,
    @JvmField var address: String? = null,
    @JvmField var addressDetail: String? = null,
    @JvmField var storeTitle: String? = null,
    @JvmField var storeType: String? = null,
    @JvmField var storeComment: String? = null,
    @JvmField var additionService: String? = null,
    @JvmField var additionInfo: String? = null,
    @JvmField var rest: String? = null,
    @JvmField var businessStartHour: String? = null,
    @JvmField var businessEndHour: String? = null,
    @JvmField var bannerImageUrl: String? = null,
    @JvmField var bannerImageUrl1: String? = null,
    @JvmField var bannerImageUrl2: String? = null,
    @JvmField var bannerImageUrl3: String? = null,
    @JvmField var bannerImageUrl4: String? = null,
    @JvmField var bannerImage: String? = null,
    @JvmField var bannerImagePath: String? = null,
    @JvmField var ceoName: String? = null,
    @JvmField var tel: String? = null,
    @JvmField var representativeMail: String? = null,
    @JvmField var registId: String? = null,
    @JvmField var registDatetime: String? = null,
    @JvmField var modifyId: String? = null,
    @JvmField var modifyDatetime: String? = null,
    @JvmField var heart: Int? = null,
    @JvmField var arIndex: String? = null,
    @JvmField var eventIndex: Int? = null,
    @JvmField var productIndex: Int? = null,
    @JvmField var storeCategoryList: List<Category>? = null,
    @JvmField var acquisitionType: String? = null,
    @JvmField var categoryIndex: Int? = null,
    @JvmField var categoryName: String? = null,

    @JvmField var type: String? = null,
    @JvmField var imageIndex: Int? = null,
    @JvmField var conversionName: String? = null,
    @JvmField var conversionPath: String? = null,
    @JvmField var realName: String? = null,
    @JvmField var realPath: String? = null,
    @JvmField var storeDescription: String? = null,
    @JvmField var storeInfoList: List<StoreInfo>? = null,
    var storeCaution: String? = null,
    var storeCategoryFiveSensesList: List<FiveSensesCategory>? = null,
    var storeCategoryEmotionList: List<EmotionCategory>? = null,
    @JvmField var frontDoorImageUrl1: String? = null,
    @JvmField var frontDoorImageUrl2: String? = null,
    @JvmField var frontDoorImageUrl3: String? = null,
    @JvmField var frontDoorImageUrl4: String? = null,
    var regionInfo: Region? = null,
    var storeCautionList: List<StoreCaution>? = null,
    var storeAdditionList: List<StoreAdditionalInfo>? = null,
    var variation: String? = null,
    @Transient var isUserLocateIn: Boolean = false,
    // MyStore
    var myStoreCnt: Int? = null,
    var myStoreRegistrationYn: String? = null,
    var regionName: String? = null,
    var categoryCode: String? = null,
    var possibleGetYn: String? = null,
    var getPossibleCnt: Int? = null,
    var venueList: String? = null
) : Serializable, Venue(MarkerType.STORE) {
    override fun getIndex(): Int? {
        return storeIndex
    }

    override fun getMyRegistration(): String? {
        return myStoreRegistrationYn
    }

    override fun getLocationPoint(): Point? {
        if (longitude != null || latitude != null) {
            return Point.fromLngLat(longitude!!.toDouble(), latitude!!.toDouble())
        }
        return null
    }

    override fun getName(): String? {
        return storeName
    }

    override fun getPossibleYn(): String? {
        return possibleGetYn
    }

    override fun getLatLng(): LatLng? {
        val lat = latitude
        val lng = longitude
        lat ?: return null
        lng ?: return null
        return LatLng(lat.toDouble(), lng.toDouble())
    }

    override fun getAddress(): String? {
        return address
    }

    fun getStoreInputModel(): StoreInput =
        StoreInput(
            storeIndex = this.storeIndex,
            userIndex = this.userIndex,
            regionIndex = this.regionIndex,
            storeType = this.storeType,
            heart = this.heart,
            eventIndex = this.eventIndex,
            productIndex = this.productIndex,
            acquisitionType = this.acquisitionType,
            variation = this.variation,
            type = this.type
        )

    fun isStoreCategoryListEmpty(): Boolean {
        return storeCategoryList.isNullOrEmpty()
    }

    fun isStoreCategoryListNotEmpty(): Boolean {
        return !isStoreCategoryListEmpty()
    }
}
