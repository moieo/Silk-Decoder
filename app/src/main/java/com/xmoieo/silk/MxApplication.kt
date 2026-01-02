package com.xmoieo.silk;

import android.app.Application
import androidx.multidex.MultiDex

class MxApplication: Application(){

    override fun onCreate(){
        MultiDex.install(this)
    }
}