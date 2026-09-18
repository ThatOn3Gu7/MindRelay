package com.mindrelay

import android.app.Application
import android.content.Context
import com.mindrelay.data.graph.AppGraph

class MindRelayApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph.create(applicationContext)
    }
}

/** Convenient app-wide access to the dependency graph. */
val Context.graph: AppGraph
    get() = (applicationContext as MindRelayApp).graph
