package com.lhbros.luckyinside.map.view.listener

interface OnTutorialListener {
    fun onRequestIsSawTutorial(viewTag: String): Boolean
    fun onSawTutorial(viewTag: String)
}