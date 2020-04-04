package com.lhbros.luckyinside.map.view.listener

import android.net.Uri
import com.lhbros.luckyinside.daily.model.DailyBanner
import com.lhbros.luckyinside.map.model.*
import com.lhbros.luckyinside.myitem.model.MyItem
import com.lhbros.luckyinside.popup.model.PopupNotice
import com.lhbros.luckyinside.share.model.ShareContent

interface OnRequestViewListener {
    fun onRequestStoreDetail(storeIndex: Int?, isAdd: Boolean = false)
    fun onRequestLuckyUDetail(luckyUIndex: Int?, isAdd: Boolean = false, isSlideAnim: Boolean = false)
    fun onRequestMyItemDetail(item: MyItem)
    fun onRequestNavigation(venue: Venue, isAdd: Boolean = false)
    fun onRequestAr(venue: Venue, isNavigationFinished: Boolean)
    fun onRequestMyStore()
    fun onRequestMyItem(isAdd: Boolean = false)
    fun onRequestLuckyU()
    fun onRequestSetting()
    fun onRequestHelp()
    fun onRequestNews()
    fun onRequestSignin()
    fun onRequestShare(type: String, shareImageUri: Uri, content: ShareContent? = null)
    fun onRequestRegionSearch(region: Region)
    fun onRequestRegionSearch(regionIndex: Int?)
    fun onRequestReDrawMarkers()
    fun onRequestBoardDetail(boardIndex: Int?, isAdd: Boolean = false)
    fun onRequestDaily(isSlideAnim: Boolean = false)
    fun onRequestMenu()
    fun onRequestGetProduct(index: Int?)
    fun onRequestGetProductDetail(luckyDrawIndex: Int, productIndex: Int)
    fun onRequestGetProductFinish(exchangePlaceName: String?, productName: String?, productImageUrl: String?)
    fun onRequestDailyVenueList(regionIndex: Int)
    fun onRequestDailyBannerImage(banner: DailyBanner)
    fun onRequestPopupNotice(popupNotice: PopupNotice)
}