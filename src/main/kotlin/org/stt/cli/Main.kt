package org.stt.cli

import org.stt.command.CommandFormatter
import org.stt.command.CommandHandler
import org.stt.model.TimeTrackingItem
import org.stt.query.Criteria
import org.stt.query.TimeTrackingItemQueries
import java.io.ByteArrayInputStream
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.logging.LogManager
import javax.inject.Inject

class Main @Inject constructor(private val timeTrackingItemQueries: TimeTrackingItemQueries,
                               private val reportPrinter: ReportPrinter,
                               private val commandFormatter: CommandFormatter,
                               private val activities: CommandHandler) {

    companion object {
        /*
         *
         * CLI use (example from ti usage):
         *
         * ti on long text containing comment //starts a new entry and inserts
         * comment
         *
         * ti on other comment //sets end time to the previous item and starts the
         * new one
         *
         * ti fin // sets end time of previous item
         */
        @JvmStatic
        fun main(args: Array<String>) {
            //switch off logging (the quick & dirty way)
            LogManager.getLogManager().readConfiguration(ByteArrayInputStream(".level = SEVERE".toByteArray(StandardCharsets.UTF_8)))

            val cliApplication = DaggerCLIApplication.builder().build()
            // accept the desired encoding for all System.out calls
            // this is necessary if one wants to output non ASCII
            // characters on a Windows console
            cliApplication.configService().start()
            val configuration = cliApplication.configService().config
            //configuration.setSttFile(new PathSetting("-"));
            System.setOut(PrintStream(FileOutputStream(FileDescriptor.out),
                    true, configuration.cli.systemOutEncoding))

            val main = cliApplication.main()
            val argsList = ArrayList(Arrays.asList(*args))
            main.prepareAndExecuteCommand(argsList, System.out)

            //store config
            cliApplication.configService().stop()
            // perform backup
            cliApplication.backupCreator().start()
        }
    }

    private fun on(args: Collection<String>, printTo: PrintStream) {
        val comment = args.joinToString(" ")
        val currentItem = timeTrackingItemQueries
                .ongoingItem

        executeCommand(comment)

        if (currentItem != null) prettyPrintTimeTrackingItem(printTo, currentItem)

        val ongoingItem = timeTrackingItemQueries.ongoingItem
        if (ongoingItem != null) {
            printTo.println("start working on " + ongoingItem.activity)
        }
    }

    fun executeCommand(command: String) {
        val parsedCommand = commandFormatter.parse(command)
        parsedCommand.accept(activities)

        timeTrackingItemQueries.sourceChanged(null)
    }

    private fun fin(args: Collection<String>, printTo: PrintStream) {
        val comment = args.joinToString(" ")
        executeCommand(comment)
        val ongoingItem = timeTrackingItemQueries.ongoingItem
        if (ongoingItem != null) prettyPrintTimeTrackingItem(printTo, ongoingItem)
    }

    private fun prettyPrintTimeTrackingItem(printTo: PrintStream, updatedItem: TimeTrackingItem) {
        printTo.println("stopped working on " + ItemFormattingHelper.prettyPrintItem(updatedItem))
    }

    internal fun prepareAndExecuteCommand(args: MutableList<String>, printTo: PrintStream) {
        if (args.isEmpty()) {
            usage(printTo)
            return
        }

        val mainOperator = args.removeAt(0)
        if (mainOperator.equals("rl", ignoreCase = true) || mainOperator.startsWith("resume")) {
            // resume last
            // add the proper command for execution
            args.add(0, "resume")
            executeCommand(args.joinToString(" "))
            val ongoingItem = timeTrackingItemQueries.ongoingItem
            if (ongoingItem != null)
                printTo.println("resumed: " + ItemFormattingHelper.prettyPrintItem(ongoingItem))
        } else if (mainOperator.matches("on?".toRegex())) {
            // no command needed for "on"
            on(args, printTo)
        } else if (mainOperator.matches("re?p?o?r?t?".toRegex())) {
            // report
            reportPrinter.report(args, printTo)
        } else if (mainOperator.matches("fi?n?".toRegex())) {
            // add the proper command for execution
            args.add(0, "fin")
            fin(args, printTo)
        } else if (mainOperator.startsWith("s")) {
            // search
            search(args, printTo)
        } else if (mainOperator.startsWith("c")) {
            // convert
            FormatConverter(args).convert()
        } else {
            usage(printTo)
        }
    }


    /**
     * output all items where the comment contains (ignoring case) the given
     * args.
     * <p>
     * Only unique comments are printed.
     * <p>
     * The ordering of the output is from newest to oldest.
     * <p>
     * Useful for completion.
     */
    private fun search(args: Collection<String>, printTo: PrintStream) {
        val searchFilter = Criteria()
        searchFilter.withActivityContains(args.joinToString(" "))
        timeTrackingItemQueries.queryItems(searchFilter).use {
            it.sorted { o1, o2 -> o2.start.compareTo(o1.start) }
                    .map(TimeTrackingItem::activity)
                    .distinct()
                    .forEach(printTo::println);
        }
    }

    /**
     * Prints usage information to the given Stream
     */
    private fun usage(printTo: PrintStream) {
        val usage = ("Usage:\n"
                + "on comment\tto start working on something\n"
                + "report [X days] [searchstring]\tto display a report\n"
                + "fin\t\tto stop working\n"
                + "search [searchstring]\tto get a list of all comments of items matching the given search string\n"
                + "resume last\tstart the previous work item if not already started")

        printTo.println(usage)
    }

}