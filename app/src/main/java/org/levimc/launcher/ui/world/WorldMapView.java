package org.levimc.launcher.ui.world;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.levimc.launcher.R;
import org.levimc.launcher.core.content.world.MapSettings;
import org.levimc.launcher.core.content.world.WorldMapRenderer;
import org.levimc.launcher.core.content.world.WorldRecords;
import org.levimc.launcher.core.content.world.WorldSession;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class WorldMapView extends View {
    public interface Listener {
        void inspect(int x, int z, boolean longPress);

        void moved(double x, double z, float scale);

        void error(String message);
    }

    private static final class Tile {
        Bitmap image;
        String error;
        List<WorldRecords.Marker> markers = new ArrayList<>();
    }

    private final WorldSession session;
    private final WorldMapRenderer renderer;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path crosshair = new Path();
    private final float density;
    private final GestureDetector gestures;
    private final ScaleGestureDetector scaleDetector;
    private final Map<String, Tile> tiles =
            new LinkedHashMap<>(256, .75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Tile> eldest) {
                    return size() > 2048;
                }
            };
    private final Set<String> pending = new HashSet<>();
    private List<WorldRecords.Marker> players = new ArrayList<>(), bookmarks = new ArrayList<>();
    private ThreadPoolExecutor worker;
    private Listener listener;
    private MapSettings settings = new MapSettings();
    private double centerX, centerZ;
    private float scale = 5;
    private int generation;
    private long contentRevision = -1;
    private boolean active;
    private String lastError = "";
    private Integer selectionX, selectionZ, selectionEndX, selectionEndZ;
    private int background, surface, foreground, accent;

    public WorldMapView(Context context, WorldSession session, WorldMapRenderer renderer) {
        super(context);
        this.session = session;
        this.renderer = renderer;
        density = context.getResources().getDisplayMetrics().density;
        float arm = 11 * density, gap = 3 * density;
        crosshair.moveTo(-arm, 0);
        crosshair.lineTo(-gap, 0);
        crosshair.moveTo(gap, 0);
        crosshair.lineTo(arm, 0);
        crosshair.moveTo(0, -arm);
        crosshair.lineTo(0, -gap);
        crosshair.moveTo(0, gap);
        crosshair.lineTo(0, arm);
        background = WorldUi.color(context, R.color.background);
        surface = WorldUi.color(context, R.color.surface_variant);
        foreground = WorldUi.color(context, R.color.on_surface);
        accent = WorldUi.accent(context);
        setFocusable(true);
        setContentDescription(
                "Saved world map. Drag to pan, pinch to zoom, tap to inspect, hold to select.");
        gestures =
                new GestureDetector(
                        context,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onDown(MotionEvent event) {
                                return true;
                            }

                            @Override
                            public boolean onScroll(
                                    MotionEvent start, MotionEvent current, float dx, float dy) {
                                if (!scaleDetector.isInProgress()) {
                                    centerX += dx / scale;
                                    centerZ += dy / scale;
                                    changed();
                                }
                                return true;
                            }

                            @Override
                            public boolean onSingleTapConfirmed(MotionEvent e) {
                                if (listener != null)
                                    listener.inspect(worldX(e.getX()), worldZ(e.getY()), false);
                                performClick();
                                return true;
                            }

                            @Override
                            public boolean onDoubleTap(MotionEvent e) {
                                zoom(1.7f, e.getX(), e.getY());
                                return true;
                            }

                            @Override
                            public void onLongPress(MotionEvent e) {
                                if (!scaleDetector.isInProgress() && listener != null)
                                    listener.inspect(worldX(e.getX()), worldZ(e.getY()), true);
                            }
                        });
        scaleDetector =
                new ScaleGestureDetector(
                        context,
                        new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            @Override
                            public boolean onScale(ScaleGestureDetector detector) {
                                zoom(
                                        detector.getScaleFactor(),
                                        detector.getFocusX(),
                                        detector.getFocusY());
                                return true;
                            }
                        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void configure(MapSettings settings) {
        long revision = session.revision();
        boolean changed =
                contentRevision != revision
                        || !this.settings.renderKey().equals(settings.renderKey())
                        || this.settings.entities != settings.entities
                        || this.settings.blockEntities != settings.blockEntities
                        || !this.settings.entityFilter.equals(settings.entityFilter)
                        || !this.settings.blockEntityFilter.equals(settings.blockEntityFilter);
        this.settings = settings.copy();
        contentRevision = revision;
        if (changed) refresh();
        else invalidate();
    }

    public void setPlayers(List<WorldRecords.Marker> value) {
        players = new ArrayList<>(value);
        invalidate();
    }

    public void setBookmarks(List<WorldRecords.Marker> value) {
        bookmarks = new ArrayList<>(value);
        invalidate();
    }

    public void center(double x, double z) {
        centerX = Math.max(-30000000, Math.min(30000000, x));
        centerZ = Math.max(-30000000, Math.min(30000000, z));
        changed();
    }

    public void viewport(double x, double z, float scale) {
        this.scale = Math.max(1, Math.min(48, scale));
        center(x, z);
    }

    public double centerX() {
        return centerX;
    }

    public double centerZ() {
        return centerZ;
    }

    public float zoom() {
        return scale;
    }

    public void zoom(float multiplier) {
        zoom(multiplier, getWidth() / 2f, getHeight() / 2f);
    }

    private void zoom(float multiplier, float focusX, float focusY) {
        double x = centerX + (focusX - getWidth() / 2f) / scale,
                z = centerZ + (focusY - getHeight() / 2f) / scale;
        scale = Math.max(1, Math.min(48, scale * multiplier));
        centerX = x - (focusX - getWidth() / 2f) / scale;
        centerZ = z - (focusY - getHeight() / 2f) / scale;
        changed();
    }

    private void changed() {
        centerX = Math.max(-30000000, Math.min(30000000, centerX));
        centerZ = Math.max(-30000000, Math.min(30000000, centerZ));
        if (listener != null) listener.moved(centerX, centerZ, scale);
        invalidate();
    }

    private int worldX(float x) {
        return (int) Math.floor(centerX + (x - getWidth() / 2f) / scale);
    }

    private int worldZ(float y) {
        return (int) Math.floor(centerZ + (y - getHeight() / 2f) / scale);
    }

    public void selection(Integer x, Integer z, Integer endX, Integer endZ) {
        selectionX = x;
        selectionZ = z;
        selectionEndX = endX;
        selectionEndZ = endZ;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        gestures.onTouchEvent(e);
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    public void start() {
        if (active) return;
        active = true;
        worker =
                new ThreadPoolExecutor(
                        1,
                        1,
                        10,
                        TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(48),
                        r -> {
                            Thread t = new Thread(r, "LeviWorldMap");
                            t.setPriority(Thread.MIN_PRIORITY);
                            return t;
                        },
                        new ThreadPoolExecutor.AbortPolicy());
        invalidate();
    }

    public void stop() {
        active = false;
        generation++;
        if (worker != null) {
            worker.shutdownNow();
            worker = null;
        }
        pending.clear();
        tiles.clear();
    }

    public void refresh() {
        generation++;
        if (worker != null) worker.getQueue().clear();
        pending.clear();
        tiles.clear();
        lastError = "";
        invalidate();
    }

    private String key(int x, int z) {
        return generation
                + ":"
                + session.revision()
                + ":"
                + x
                + ":"
                + z
                + ":"
                + settings.renderKey()
                + ":"
                + settings.entities
                + ":"
                + settings.blockEntities
                + ":"
                + settings.entityFilter
                + ":"
                + settings.blockEntityFilter;
    }

    private void request(int x, int z) {
        if (!active || worker == null || pending.size() >= 48) return;
        String id = key(x, z);
        if (tiles.containsKey(id) || !pending.add(id)) return;
        int expected = generation;
        MapSettings options = settings.copy();
        try {
            worker.execute(
                    () -> {
                        Tile tile = new Tile();
                        try {
                            tile.image = renderer.render(x, z, options);
                            if (options.entities || options.blockEntities)
                                tile.markers = WorldRecords.chunkMarkers(session, x, z, options);
                        } catch (Exception ex) {
                            if (Thread.currentThread().isInterrupted()) return;
                            tile.error = ex.getMessage();
                        }
                        if (Thread.currentThread().isInterrupted()) return;
                        post(
                                () -> {
                                    pending.remove(id);
                                    if (!active || generation != expected) return;
                                    tiles.put(id, tile);
                                    if (tile.error != null && !tile.error.equals(lastError)) {
                                        lastError = tile.error;
                                        if (listener != null) listener.error(tile.error);
                                    }
                                    invalidate();
                                });
                    });
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            pending.remove(id);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(background);
        double left = centerX - getWidth() / (2.0 * scale),
                top = centerZ - getHeight() / (2.0 * scale);
        int minX = Math.floorDiv((int) Math.floor(left), 16),
                minZ = Math.floorDiv((int) Math.floor(top), 16),
                maxX = Math.floorDiv((int) Math.ceil(left + getWidth() / scale), 16),
                maxZ = Math.floorDiv((int) Math.ceil(top + getHeight() / scale), 16);
        List<int[]> missing = new ArrayList<>();
        paint.setFilterBitmap(false);
        paint.setStyle(Paint.Style.FILL);
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++) {
                float sx = (float) ((x * 16 - left) * scale),
                        sz = (float) ((z * 16 - top) * scale),
                        size = 16 * scale;
                Tile tile = tiles.get(key(x, z));
                RectF rect = new RectF(sx, sz, sx + size, sz + size);
                paint.setColor(((x ^ z) & 1) == 0 ? surface : WorldUi.translucent(surface, 120));
                canvas.drawRect(rect, paint);
                if (tile != null && tile.image != null)
                    canvas.drawBitmap(tile.image, null, rect, paint);
                if (tile == null && missing.size() < 4096) missing.add(new int[] {x, z});
                if (tile != null && tile.error != null) {
                    paint.setColor(WorldUi.translucent(accent, 130));
                    paint.setStrokeWidth(1);
                    canvas.drawLine(sx, sz, sx + size, sz + size, paint);
                }
                if (settings.slimeOverlay
                        && settings.dimension == 0
                        && WorldMapRenderer.slime(x, z)) {
                    paint.setColor(0x5579cd62);
                    canvas.drawRect(rect, paint);
                }
                if (settings.grid && scale >= 2) {
                    paint.setColor(WorldUi.translucent(foreground, 55));
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(1);
                    canvas.drawRect(rect, paint);
                    paint.setStyle(Paint.Style.FILL);
                }
            }
        missing.sort(
                java.util.Comparator.comparingDouble(
                        a ->
                                Math.abs(a[0] * 16 + 8 - centerX)
                                        + Math.abs(a[1] * 16 + 8 - centerZ)));
        for (int i = 0; i < Math.min(48, missing.size()); i++)
            request(missing.get(i)[0], missing.get(i)[1]);
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++) {
                Tile tile = tiles.get(key(x, z));
                if (tile != null)
                    for (WorldRecords.Marker marker : tile.markers)
                        drawMarker(canvas, marker, left, top);
            }
        if (settings.players)
            for (WorldRecords.Marker marker : players) drawMarker(canvas, marker, left, top);
        for (WorldRecords.Marker marker : bookmarks) drawMarker(canvas, marker, left, top);
        if (selectionX != null && selectionZ != null) {
            int x2 = selectionEndX == null ? selectionX : selectionEndX,
                    z2 = selectionEndZ == null ? selectionZ : selectionEndZ;
            RectF selected =
                    new RectF(
                            (float) ((Math.min(selectionX, x2) - left) * scale),
                            (float) ((Math.min(selectionZ, z2) - top) * scale),
                            (float) ((Math.max(selectionX, x2) + 1 - left) * scale),
                            (float) ((Math.max(selectionZ, z2) + 1 - top) * scale));
            paint.setColor(WorldUi.translucent(accent, 48));
            canvas.drawRect(selected, paint);
            paint.setColor(accent);
            paint.setStrokeWidth(WorldUi.dp(getContext(), 2));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(selected, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        drawCrosshair(canvas);
        if (settings.coordinates) {
            paint.setColor(foreground);
            paint.setTextSize(WorldUi.dp(getContext(), 11));
            canvas.drawText(
                    "N ↑", WorldUi.dp(getContext(), 12), WorldUi.dp(getContext(), 20), paint);
            int length =
                    Math.max(1, (int) Math.pow(2, Math.floor(Math.log(90 / scale) / Math.log(2))));
            float line = length * scale;
            canvas.drawLine(12, getHeight() - 18, 12 + line, getHeight() - 18, paint);
            canvas.drawText(length + " blocks", 12, getHeight() - 25, paint);
        }
    }

    private void drawMarker(Canvas canvas, WorldRecords.Marker marker, double left, double top) {
        if (marker.dimension != settings.dimension) return;
        float x = (float) ((marker.x - left) * scale),
                z = (float) ((marker.z - top) * scale),
                radius =
                        WorldUi.dp(getContext(), marker.kind == WorldRecords.Marker.PLAYER ? 5 : 3);
        if (x < 0 || z < 0 || x > getWidth() || z > getHeight()) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(background);
        canvas.drawCircle(x, z, radius + 2, paint);
        paint.setColor(
                marker.kind == WorldRecords.Marker.PLAYER
                        ? accent
                        : marker.kind == WorldRecords.Marker.ENTITY
                                ? 0xffe7a24d
                                : marker.kind == WorldRecords.Marker.BOOKMARK
                                        ? 0xffc27eb8
                                        : 0xff69adc2);
        canvas.drawCircle(x, z, radius, paint);
    }

    private void drawCrosshair(Canvas canvas) {
        int save = canvas.save();
        canvas.translate(getWidth() / 2f, getHeight() / 2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(0xe614161c);
        paint.setStrokeWidth(5 * density);
        canvas.drawPath(crosshair, paint);
        paint.setColor(0xffffffff);
        paint.setStrokeWidth(2 * density);
        canvas.drawPath(crosshair, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xe614161c);
        canvas.drawCircle(0, 0, 2.5f * density, paint);
        paint.setColor(0xffffffff);
        canvas.drawCircle(0, 0, density, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeWidth(density);
        canvas.restoreToCount(save);
    }

    public Bitmap screenshot() {
        Bitmap image =
                Bitmap.createBitmap(
                        Math.max(1, getWidth()), Math.max(1, getHeight()), Bitmap.Config.ARGB_8888);
        draw(new Canvas(image));
        return image;
    }
}
