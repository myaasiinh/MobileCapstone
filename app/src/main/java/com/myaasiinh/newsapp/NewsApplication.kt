package com.myaasiinh.newsapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NewsApplication : Application() /* wraps whole app class, declared to be in manifest*/