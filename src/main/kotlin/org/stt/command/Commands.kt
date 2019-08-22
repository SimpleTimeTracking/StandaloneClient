package org.stt.command

import org.stt.model.TimeTrackingItem
import java.time.LocalDateTime

interface Command {
    fun accept(commandHandler: CommandHandler)
}


object DoNothing : Command {
    override fun accept(commandHandler: CommandHandler) {
        // DO NOTHING
    }
}

class EndCurrentItem(val endAt: LocalDateTime) : Command {
    override fun accept(commandHandler: CommandHandler) {
        commandHandler.endCurrentActivity(this)
    }
}

class NewActivity(val newItem: TimeTrackingItem) : Command {
    override fun accept(commandHandler: CommandHandler) {
        commandHandler.addNewActivity(this)
    }
}

class RemoveActivity(val itemToDelete: TimeTrackingItem) : Command {
    override fun accept(commandHandler: CommandHandler) {
        commandHandler.removeActivity(this)
    }
}

class ResumeActivity(val itemToResume: TimeTrackingItem, val beginningWith: LocalDateTime) : Command {
    override fun accept(commandHandler: CommandHandler) {
        commandHandler.resumeActivity(this)
    }
}

class ResumeLastActivity(val resumeAt: LocalDateTime) : Command {

    override fun accept(commandHandler: CommandHandler) {
        commandHandler.resumeLastActivity(this)
    }
}
