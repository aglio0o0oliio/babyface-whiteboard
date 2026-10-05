package com.babyface.whiteboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

public class WhiteboardView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<Stroke> strokes = new ArrayList<>();
    private final ArrayList<Stroke> redo = new ArrayList<>();
    private final SharedPreferences prefs;
    private Stroke current;
    private boolean eraser = false;
    private float penWidth = 6f;

    static class PointData {
        float x, y;
        PointData(float x, float y) { this.x = x; this.y = y; }
    }

    static class Stroke {
        final ArrayList<PointData> points = new ArrayList<>();
        final float width;
        final boolean eraser;
        Stroke(float width, boolean eraser) {
            this.width = width;
            this.eraser = eraser;
        }
    }

    public WhiteboardView(Context context) {
        super(context);
        prefs = context.getSharedPreferences("whiteboard", Context.MODE_PRIVATE);
        setBackgroundColor(Color.WHITE);
        setKeepScreenOn(true);
        load();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        for (Stroke s : strokes) drawStroke(canvas, s);
        if (current != null) drawStroke(canvas, current);
    }

    private void drawStroke(Canvas canvas, Stroke s) {
        if (s.points.isEmpty()) return;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(s.width);
        paint.setColor(Color.rgb(20, 20, 20));
        paint.setXfermode(s.eraser
                ? new PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                : null);

        if (s.points.size() == 1) {
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(s.points.get(0).x, s.points.get(0).y,
                    s.width / 2f, paint);
        } else {
            Path path = new Path();
            path.moveTo(s.points.get(0).x, s.points.get(0).y);
            for (int i = 1; i < s.points.size(); i++) {
                path.lineTo(s.points.get(i).x, s.points.get(i).y);
            }
            canvas.drawPath(path, paint);
        }
        paint.setXfermode(null);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                current = new Stroke(penWidth, eraser);
                current.points.add(new PointData(e.getX(), e.getY()));
                redo.clear();
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (current != null) {
                    current.points.add(new PointData(e.getX(), e.getY()));
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (current != null) {
                    strokes.add(current);
                    current = null;
                    save();
                    invalidate();
                }
                return true;
        }
        return true;
    }

    public void setEraser(boolean value) {
        eraser = value;
    }

    public boolean isEraser() {
        return eraser;
    }

    public void setPenWidth(float value) {
        penWidth = Math.max(1f, Math.min(30f, value));
    }

    public void undo() {
        if (!strokes.isEmpty()) {
            redo.add(strokes.remove(strokes.size() - 1));
            save();
            invalidate();
        }
    }

    public void redo() {
        if (!redo.isEmpty()) {
            strokes.add(redo.remove(redo.size() - 1));
            save();
            invalidate();
        }
    }

    public void clearBoard() {
        strokes.clear();
        redo.clear();
        save();
        invalidate();
    }

    private void save() {
        StringBuilder b = new StringBuilder();
        for (Stroke s : strokes) {
            b.append(s.eraser ? "E" : "P")
             .append("|").append(s.width).append("|");
            for (PointData p : s.points) {
                b.append(p.x).append(",").append(p.y).append(";");
            }
            b.append("\n");
        }
        prefs.edit().putString("strokes", b.toString()).apply();
    }

    private void load() {
        try {
            String data = prefs.getString("strokes", "");
            if (data.isEmpty()) return;

            for (String line : data.split("\\n")) {
                if (line.trim().isEmpty()) continue;

                String[] a = line.split("\\|", 3);
                if (a.length < 3) continue;

                Stroke s = new Stroke(
                    Float.parseFloat(a[1]),
                    "E".equals(a[0])
                );

                for (String pair : a[2].split(";")) {
                    if (pair.isEmpty()) continue;
                    String[] xy = pair.split(",");
                    if (xy.length == 2) {
                        s.points.add(new PointData(
                            Float.parseFloat(xy[0]),
                            Float.parseFloat(xy[1])
                        ));
                    }
                }
                strokes.add(s);
            }
            invalidate();
        } catch (Exception ignored) {}
    }
}
