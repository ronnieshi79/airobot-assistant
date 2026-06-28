package com.airobot.agent.di

import com.airobot.agent.brain.AiBrain
import com.airobot.agent.brain.xiaozhi.XiaozhiCloudBrain
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AgentModule {
    @Binds
    @Singleton
    abstract fun bindAiBrain(
        xiaozhiCloudBrain: XiaozhiCloudBrain
    ): AiBrain

    companion object {
        @Provides
        @Singleton
        fun provideGson(): Gson = Gson()
    }
}
