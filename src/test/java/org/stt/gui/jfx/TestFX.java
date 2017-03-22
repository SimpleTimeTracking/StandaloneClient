package org.stt.gui.jfx;


import com.sun.glass.ui.CommonDialogs;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.embed.HostInterface;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.perf.PerformanceTracker;
import com.sun.javafx.runtime.async.AsyncOperation;
import com.sun.javafx.runtime.async.AsyncOperationListener;
import com.sun.javafx.scene.text.HitInfo;
import com.sun.javafx.scene.text.TextLayoutFactory;
import com.sun.javafx.tk.*;
import com.sun.scenario.DelayedRunnable;
import com.sun.scenario.animation.AbstractMasterTimer;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;
import javafx.geometry.Dimension2D;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.shape.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.File;
import java.io.InputStream;
import java.security.AccessControlContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

public class TestFX {
    private TestFX() {
    }

    public static void installTK() {
        System.setProperty("javafx.toolkit", TestFXToolkit.class.getName());
        PlatformImpl.startup(() -> {
        });
    }

    public static class TestFXToolkit extends Toolkit {
        @Override
        public boolean init() {
            return false;
        }

        @Override
        public boolean canStartNestedEventLoop() {
            return false;
        }

        @Override
        public Object enterNestedEventLoop(Object key) {
            return null;
        }

        @Override
        public void exitNestedEventLoop(Object key, Object rval) {

        }

        @Override
        public boolean isNestedLoopRunning() {
            return false;
        }

        @Override
        public TKStage createTKStage(Window peerWindow, boolean securityDialog, StageStyle stageStyle, boolean primary, Modality modality, TKStage owner, boolean rtl, AccessControlContext acc) {
            return null;
        }

        @Override
        public TKStage createTKPopupStage(Window peerWindow, StageStyle popupStyle, TKStage owner, AccessControlContext acc) {
            return null;
        }

        @Override
        public TKStage createTKEmbeddedStage(HostInterface host, AccessControlContext acc) {
            return null;
        }

        @Override
        public AppletWindow createAppletWindow(long parent, String serverName) {
            return null;
        }

        @Override
        public void closeAppletWindow() {

        }

        @Override
        public void requestNextPulse() {

        }

        @Override
        public Future addRenderJob(RenderJob rj) {
            return null;
        }

        @Override
        public ImageLoader loadImage(String url, int width, int height, boolean preserveRatio, boolean smooth) {
            return null;
        }

        @Override
        public ImageLoader loadImage(InputStream stream, int width, int height, boolean preserveRatio, boolean smooth) {
            return null;
        }

        @Override
        public AsyncOperation loadImageAsync(AsyncOperationListener<? extends ImageLoader> listener, String url, int width, int height, boolean preserveRatio, boolean smooth) {
            return null;
        }

        @Override
        public ImageLoader loadPlatformImage(Object platformImage) {
            return null;
        }

        @Override
        public PlatformImage createPlatformImage(int w, int h) {
            return null;
        }

        @Override
        public void startup(Runnable runnable) {

        }

        @Override
        public void defer(Runnable runnable) {

        }

        @Override
        public Map<Object, Object> getContextMap() {
            return null;
        }

        @Override
        public int getRefreshRate() {
            return 0;
        }

        @Override
        public void setAnimationRunnable(DelayedRunnable animationRunnable) {

        }

        @Override
        public PerformanceTracker getPerformanceTracker() {
            return null;
        }

        @Override
        public PerformanceTracker createPerformanceTracker() {
            return null;
        }

        @Override
        public void waitFor(Task t) {

        }

        @Override
        protected Object createColorPaint(Color paint) {
            return null;
        }

        @Override
        protected Object createLinearGradientPaint(LinearGradient paint) {
            return null;
        }

        @Override
        protected Object createRadialGradientPaint(RadialGradient paint) {
            return null;
        }

        @Override
        protected Object createImagePatternPaint(ImagePattern paint) {
            return null;
        }

        @Override
        public void accumulateStrokeBounds(Shape shape, float[] bbox, StrokeType type, double strokewidth, StrokeLineCap cap, StrokeLineJoin join, float miterLimit, BaseTransform tx) {

        }

        @Override
        public boolean strokeContains(Shape shape, double x, double y, StrokeType type, double strokewidth, StrokeLineCap cap, StrokeLineJoin join, float miterLimit) {
            return false;
        }

        @Override
        public Shape createStrokedShape(Shape shape, StrokeType type, double strokewidth, StrokeLineCap cap, StrokeLineJoin join, float miterLimit, float[] dashArray, float dashOffset) {
            return null;
        }

        @Override
        public int getKeyCodeForChar(String character) {
            return 0;
        }

        @Override
        public Dimension2D getBestCursorSize(int preferredWidth, int preferredHeight) {
            return null;
        }

        @Override
        public int getMaximumCursorColors() {
            return 0;
        }

        @Override
        public PathElement[] convertShapeToFXPath(Object shape) {
            return new PathElement[0];
        }

        @Override
        public HitInfo convertHitInfoToFX(Object hit) {
            return null;
        }

        @Override
        public Filterable toFilterable(Image img) {
            return null;
        }

        @Override
        public FilterContext getFilterContext(Object config) {
            return null;
        }

        @Override
        public boolean isForwardTraversalKey(KeyEvent e) {
            return false;
        }

        @Override
        public boolean isBackwardTraversalKey(KeyEvent e) {
            return false;
        }

        @Override
        public AbstractMasterTimer getMasterTimer() {
            return null;
        }

        @Override
        public FontLoader getFontLoader() {
            return null;
        }

        @Override
        public TextLayoutFactory getTextLayoutFactory() {
            return null;
        }

        @Override
        public Object createSVGPathObject(SVGPath svgpath) {
            return null;
        }

        @Override
        public Path2D createSVGPath2D(SVGPath svgpath) {
            return null;
        }

        @Override
        public boolean imageContains(Object image, float x, float y) {
            return false;
        }

        @Override
        public TKClipboard getSystemClipboard() {
            return null;
        }

        @Override
        public TKSystemMenu getSystemMenu() {
            return null;
        }

        @Override
        public TKClipboard getNamedClipboard(String name) {
            return null;
        }

        @Override
        public ScreenConfigurationAccessor setScreenConfigurationListener(TKScreenConfigurationListener listener) {
            return null;
        }

        @Override
        public Object getPrimaryScreen() {
            return null;
        }

        @Override
        public List<?> getScreens() {
            return null;
        }

        @Override
        public ScreenConfigurationAccessor getScreenConfigurationAccessor() {
            return null;
        }

        @Override
        public void registerDragGestureListener(TKScene s, Set<TransferMode> tm, TKDragGestureListener l) {

        }

        @Override
        public void startDrag(TKScene scene, Set<TransferMode> tm, TKDragSourceListener l, Dragboard dragboard) {

        }

        @Override
        public void enableDrop(TKScene s, TKDropTargetListener l) {

        }

        @Override
        public void installInputMethodRequests(TKScene scene, InputMethodRequests requests) {

        }

        @Override
        public Object renderToImage(ImageRenderingContext context) {
            return null;
        }

        @Override
        public CommonDialogs.FileChooserResult showFileChooser(TKStage ownerWindow, String title, File initialDirectory, String initialFileName, FileChooserType fileChooserType, List<FileChooser.ExtensionFilter> extensionFilters, FileChooser.ExtensionFilter selectedFilter) {
            return null;
        }

        @Override
        public File showDirectoryChooser(TKStage ownerWindow, String title, File initialDirectory) {
            return null;
        }

        @Override
        public long getMultiClickTime() {
            return 0;
        }

        @Override
        public int getMultiClickMaxX() {
            return 0;
        }

        @Override
        public int getMultiClickMaxY() {
            return 0;
        }
    }
}
