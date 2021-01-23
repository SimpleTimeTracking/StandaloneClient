package org.stt.gui.jfx

import com.sun.javafx.scene.control.skin.DatePickerSkin
import javafx.animation.PauseTransition
import javafx.application.Platform
import javafx.beans.binding.*
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.TableColumn.CellDataFeatures
import javafx.scene.control.TableColumn.SortType
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import net.engio.mbassy.bus.MBassador
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Listener
import net.engio.mbassy.listener.References
import org.controlsfx.control.NotificationPane
import org.stt.config.ActivitiesConfig
import org.stt.gui.jfx.binding.MappedListBinding
import org.stt.gui.jfx.binding.ReportBinding
import org.stt.gui.jfx.binding.STTBindings
import org.stt.model.ItemModified
import org.stt.query.TimeTrackingItemQueries
import org.stt.reporting.SummingReportGenerator.Report
import org.stt.text.ItemGrouper
import org.stt.time.DateTimes
import org.stt.time.DateTimes.FORMATTER_PERIOD_HHh_MMm_SSs
import org.stt.time.DurationRounder
import java.io.IOException
import java.io.UncheckedIOException
import java.time.Duration
import java.time.LocalDate
import java.util.*
import java.util.concurrent.Callable
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.stream.Collectors
import javax.inject.Inject
import javax.inject.Named

class ReportController @Inject
internal constructor(private val localization: ResourceBundle,
                     private val timeTrackingItemQueries: TimeTrackingItemQueries,
                     private val rounder: DurationRounder,
                     private val itemGrouper: @JvmSuppressWildcards ItemGrouper,
                     private val activitiesConfig: ActivitiesConfig,
                     @param:Named("glyph") private val fontaweSome: Font,
                     private val eventBus: MBassador<Any>) {
    @FXML
    private lateinit var columnForRoundedDuration: TableColumn<ListItem, String>
    @FXML
    private lateinit var columnForDuration: TableColumn<ListItem, String>
    @FXML
    private lateinit var columnForComment: TableColumn<ListItem, String>
    @FXML
    private lateinit var tableForReport: TableView<ListItem>
    @FXML
    private lateinit var left: VBox
    @FXML
    private lateinit var startOfReport: Label
    @FXML
    private lateinit var endOfReport: Label
    @FXML
    private lateinit var uncoveredTime: Label
    @FXML
    private lateinit var roundedDurationSum: Label
    private lateinit var datePicker: DatePicker

    internal val panel: NotificationPane by lazy {
        loadAndInjectFXML()
    }
    private val notificationPause = PauseTransition(javafx.util.Duration.seconds(2.0))
    private lateinit var trackedDays: Set<LocalDate>

    private fun loadAndInjectFXML(): NotificationPane {
        val loader = FXMLLoader(javaClass.getResource(
                "/org/stt/gui/jfx/ReportPanel.fxml"), localization)
        loader.setController(this)
        val reportPane: Pane
        try {
            reportPane = loader.load()
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }

        notificationPause.setOnFinished { panel.hide() }
        return NotificationPane(reportPane)
    }

    @FXML
    fun initialize() {
        setupNavigation()

        tableForReport.selectionModel.selectedIndexProperty().addListener { _ -> Platform.runLater { tableForReport.selectionModel.clearSelection() } }

        val reportModel = createReportModel()
        eventBus.subscribe(OnItemChangeListener(reportModel))
        val startBinding = createBindingForStartOfReport(reportModel)
        val endBinding = createBindingForEndOfReport(reportModel)
        val uncoveredTimeBinding = createBindingForUncoveredTimeOfReport(reportModel)
        val formattedUncoveredTimeBinding = STTBindings
                .formattedDuration(uncoveredTimeBinding)
        val uncoveredTimeTextFillBinding = When(
                uncoveredTimeBinding.isEqualTo(Duration.ZERO)).then(
                Color.BLACK).otherwise(Color.RED)

        startOfReport.textProperty().bind(startBinding)
        endOfReport.textProperty().bind(endBinding)
        uncoveredTime.textFillProperty().bind(uncoveredTimeTextFillBinding)
        uncoveredTime.textProperty().bind(formattedUncoveredTimeBinding)
        startOfReport.setOnMouseClicked { setClipboard(startBinding.get()) }
        endOfReport.setOnMouseClicked { setClipboard(endBinding.get()) }

        val reportListModel = createReportingItemsListModel(reportModel)
        tableForReport.items = reportListModel

        roundedDurationSum
                .textProperty()
                .bind(STTBindings
                        .formattedDuration(createBindingForRoundedDurationSum(reportListModel)))

        columnForRoundedDuration.style = "-fx-alignment: TOP-RIGHT;"
        columnForDuration.style = "-fx-alignment: TOP-RIGHT;"

        setRoundedDurationColumnCellFactoryToConvertDurationToString()
        setDurationColumnCellFactoryToConvertDurationToString()
        setCommentColumnCellFactory()

        presetSortingToAscendingCommentColumn()

        applyClipboardTooltip(Consumer { columnForComment.setGraphic(it) }, "report.tooltips.copyActivity")
        applyClipboardTooltip(Consumer { columnForDuration.setGraphic(it) }, "report.tooltips.copyRow")
        applyClipboardTooltip(Consumer { columnForRoundedDuration.setGraphic(it) }, "report.tooltips.copyRow")
        applyClipboardTooltip(Consumer { startOfReport.graphic = it }, "report.tooltips.copy")
        applyClipboardTooltip(Consumer { endOfReport.graphic = it }, "report.tooltips.copy")
    }

    private fun applyClipboardTooltip(on: Consumer<Node>, tooltipKey: String) {
        val tooltipLabel = Glyph.glyph(fontaweSome, Glyph.CLIPBOARD)
        Tooltips.install(tooltipLabel, localization.getString(tooltipKey))
        on.accept(tooltipLabel)
    }

    private fun setupNavigation() {
        datePicker = DatePicker(LocalDate.now())
        trackedDays = timeTrackingItemQueries.queryAllTrackedDays().collect(Collectors.toSet())
        datePicker.setDayCellFactory {
            object : DateCell() {
                override fun updateItem(item: LocalDate, empty: Boolean) {
                    super.updateItem(item, empty)

                    if (trackedDays.contains(item) && !isSelected) {
                        style = "-fx-background-color: #006699;"
                    }
                }
            }
        }
        datePicker.valueProperty().addListener { _ -> trackedDays = timeTrackingItemQueries.queryAllTrackedDays().collect(Collectors.toSet()) }
        val popupContent = DatePickerSkin(datePicker).popupContent
        left.children.add(0, popupContent)
    }

    private fun createReportModel(): ObjectBinding<Report> {
        val nextDay = Bindings.createObjectBinding<LocalDate>(
                Callable {
                    if (datePicker.value != null)
                        datePicker
                                .value.plusDays(1)
                    else
                        null
                }, datePicker.valueProperty())
        return ReportBinding(datePicker.valueProperty(), nextDay, timeTrackingItemQueries)
    }

    private fun createReportingItemsListModel(
            report: ObservableValue<Report>): ListBinding<ListItem> {
        return MappedListBinding({
            report.value
                    .reportingItems
                    .map { reportingItem ->
                        ListItem(
                                reportingItem.comment, reportingItem.duration,
                                rounder.roundDuration(reportingItem.duration))
                    }
        }, report)
    }


    private fun createBindingForRoundedDurationSum(
            items: ObservableList<ListItem>): ObservableValue<Duration> {
        return Bindings.createObjectBinding(Callable {
            items.map { it.roundedDuration }
                    .foldRight(Duration.ZERO) { obj, duration -> obj.plus(duration) }
        }, items)
    }

    private fun createBindingForUncoveredTimeOfReport(
            reportModel: ObservableValue<Report>): ObjectBinding<Duration> {
        return Bindings.createObjectBinding(Callable { reportModel.value.uncoveredDuration }, reportModel)
    }

    private fun createBindingForEndOfReport(
            reportModel: ObservableValue<Report>): StringBinding {
        return Bindings.createStringBinding(Callable {
            val end = reportModel.value.end
            if (end != null)
                DateTimes.DATE_TIME_FORMATTER_HH_MM_SS
                        .format(end)
            else
                ""
        }, reportModel)
    }

    private fun createBindingForStartOfReport(
            reportModel: ObservableValue<Report>): StringBinding {
        return Bindings.createStringBinding(Callable {
            val start = reportModel.value.start
            if (start != null)
                DateTimes.DATE_TIME_FORMATTER_HH_MM_SS
                        .format(start)
            else
                ""
        }, reportModel)
    }

    private fun presetSortingToAscendingCommentColumn() {
        columnForComment.sortType = SortType.ASCENDING
        tableForReport.sortOrder.add(columnForComment)
    }

    private fun setCommentColumnCellFactory() {
        columnForComment.cellValueFactory = PropertyValueFactory(
                "comment")
        if (activitiesConfig.isGroupItems) {
            setItemGroupingCellFactory()
        } else {
            addClickToCopy(columnForComment, BiConsumer { item, _ -> setClipboard(item.comment) })
        }
    }

    private fun addClickToCopy(column: TableColumn<ListItem, String>, clickHandler: BiConsumer<ListItem, MouseEvent>) {
        column.setCellFactory { param ->
            val tableCell = TableColumn.DEFAULT_CELL_FACTORY.call(param) as TableCell<ListItem, String>
            tableCell.setOnMouseClicked { event ->
                val item = tableCell.tableRow.item as? ListItem ?: return@setOnMouseClicked
                clickHandler.accept(item, event)
            }
            tableCell
        }
    }

    private fun setItemGroupingCellFactory() {
        columnForComment.setCellFactory { ActivityTableCell() }
    }

    private fun setClipboard(comment: String) {
        val content = ClipboardContent()
        content.putString(comment)
        setClipboardContentTo(content)

        notifyUserOfClipboardContent(comment)
    }

    private fun notifyUserOfClipboardContent(comment: String) {
        panel.graphic = Glyph.glyph(fontaweSome, Glyph.CLIPBOARD, 20.0)
        panel.text = String.format("'%s'", comment)
        panel.show()
        notificationPause.playFromStart()
    }

    private fun copyDurationToClipboard(duration: Duration) {
        setClipboard(DateTimes.prettyPrintDuration(duration))
    }

    private fun setClipboardContentTo(content: ClipboardContent) {
        val clipboard = Clipboard.getSystemClipboard()
        clipboard.setContent(content)
    }

    private fun setDurationColumnCellFactoryToConvertDurationToString() {
        columnForDuration.cellValueFactory = object : PropertyValueFactory<ListItem, String>(
                "duration") {
            override fun call(
                    cellDataFeatures: CellDataFeatures<ListItem, String>): ObservableValue<String> {
                val duration = FORMATTER_PERIOD_HHh_MMm_SSs(cellDataFeatures.value.duration)
                return SimpleStringProperty(duration)
            }
        }
        addClickToCopy(columnForDuration, BiConsumer { item, _ -> copyDurationToClipboard(item.duration) })
    }

    private fun setRoundedDurationColumnCellFactoryToConvertDurationToString() {
        columnForRoundedDuration.cellValueFactory = object : PropertyValueFactory<ListItem, String>(
                "roundedDuration") {
            override fun call(
                    cellDataFeatures: CellDataFeatures<ListItem, String>): ObservableValue<String> {
                val duration = FORMATTER_PERIOD_HHh_MMm_SSs(cellDataFeatures.value.roundedDuration)
                return SimpleStringProperty(duration)
            }
        }
        addClickToCopy(columnForRoundedDuration, BiConsumer { item, _ -> copyDurationToClipboard(item.roundedDuration) })
    }

    private inner class ActivityTableCell internal constructor() : TableCell<ListItem, String>() {
        private val textFlow = object : TextFlow() {
            // Textflow bug: Tries to place each character on a separate line if != USE_COMPUTED_SIZE by delivering width < -1...
            override fun computePrefHeight(width: Double): Double {
                if (width > Region.USE_COMPUTED_SIZE) {
                    return super.computePrefHeight(width)
                }
                val parent = parent as ActivityTableCell
                var prefWidth = parent.computePrefWidth(Region.USE_COMPUTED_SIZE)
                val insets = parent.insets
                // See javafx.scene.control.Control.layoutChildren()
                prefWidth = snapSize(prefWidth) - snapSize(insets.left) - snapSize(insets.right)
                return super.computePrefHeight(prefWidth)
            }
        }

        init {
            styleClass.add("activity-table-cell")
            graphic = textFlow

            setOnMouseClicked {
                val item = this@ActivityTableCell.tableRow.item as? ListItem ?: return@setOnMouseClicked
                setClipboard(item.comment)
            }
        }

        override fun updateItem(item: String?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty || item == null) {
                graphic = null
            } else {
                val textList = textFlow.children
                textList.clear()
                val itemGroups = itemGrouper(item).map { it.content }
                for (i in itemGroups.indices) {
                    val partToShow: String
                    val part = itemGroups[i]
                    partToShow = if (i > 0) " $part" else part
                    val partLabel = Text(partToShow)
                    addClickListener(itemGroups, partLabel, i)
                    if (i < itemGroups.size - 1) {
                        partLabel.styleClass.addAll("reportGroup", "reportGroup$i")
                    }
                    textList.add(partLabel)
                }
                graphic = textFlow
            }
        }

        private fun addClickListener(itemGroups: List<String>, partLabel: Node, fromIndex: Int) {
            partLabel.setOnMouseClicked { event ->
                val commentRemainder = if (event.isControlDown)
                    itemGroups.subList(0, fromIndex + 1).joinToString(" ")
                else
                    itemGroups.subList(fromIndex, itemGroups.size).joinToString(" ")
                setClipboard(commentRemainder)
                event.consume()
            }
        }
    }

    class ListItem internal constructor(val comment: String, val duration: Duration,
                                        internal val roundedDuration: Duration)

    @Listener(references = References.Strong)
    private class OnItemChangeListener internal constructor(private val binding: ObjectBinding<*>) {

        @Handler
        fun onItemChanged(changeEvent: ItemModified) {
            binding.invalidate()
        }
    }
}
