package org.stt.cli

import dagger.Component
import org.stt.BaseModule
import org.stt.command.CommandModule
import org.stt.config.ConfigModule
import org.stt.config.ConfigServiceFacade
import org.stt.persistence.BackupCreator
import org.stt.persistence.stt.STTPersistenceModule
import org.stt.text.TextModule

import javax.inject.Singleton

@Singleton
@Component(modules = [STTPersistenceModule::class, ConfigModule::class, BaseModule::class, TextModule::class, CommandModule::class])
interface CLIApplication {
    fun backupCreator(): BackupCreator

    fun configService(): ConfigServiceFacade

    fun main(): Main
}
