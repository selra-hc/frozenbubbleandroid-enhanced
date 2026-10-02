/*
 *                 [[ Frozen-Bubble ]]
 *
 * Copyright (c) 2000-2003 Guillaume Cottenceau.
 * Java sourcecode - Copyright (c) 2003 Glenn Sanson.
 * Additional source - Copyright (c) 2013 Eric Fortin.
 *
 * This code is distributed under the GNU General Public License
 */

package org.cb.enhancedfrozenbubble;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class LevelEditorView extends View {

  private static final int CELL_SIZE    = 32;
  private static final int ROW_HEIGHT   = 28;
  /*
   * FrozenGame's own bubble layout formula (left = 190 + i*32 - ...) lays
   * cells out relative to the shared 640-wide two-player field, where a
   * single player's content ends up roughly centered rather than confined
   * to a 0..320 slice. Since this editor's virtual canvas is a single,
   * standalone 320-wide field (GameView.GAMEFIELD_WIDTH), the grid must
   * be explicitly centered within it instead of reusing that raw offset.
   */
  private static final int FIELD_LEFT   =
      (GameView.GAMEFIELD_WIDTH - LevelManager.NUM_COLS * CELL_SIZE) / 2;
  private static final int FIELD_TOP    = 44;

  private byte[][] grid;
  private Bitmap[] bubbles;
  private Bitmap background;
  private int colorLimit = LevelManager.NORMAL;
  private float scale = 1.0f;
  private float offsetX = 0.0f;
  private float offsetY = 0.0f;

  public LevelEditorView(Context context) {
    super(context);
    init(context);
  }

  public LevelEditorView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  private void init(Context context) {
    grid = LevelManager.newEmptyLevel();
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inScaled = false;
    bubbles = new Bitmap[8];
    bubbles[0] = BitmapFactory.decodeResource(getResources(),
        R.drawable.bubble_1, options);
    bubbles[1] = BitmapFactory.decodeResource(getResources(),
        R.drawable.bubble_2, options);
    bubbles[2] = BitmapFactory.decodeResource(getResources(),
        R.drawable.bubble_3, options);
    bubbles[3] = BitmapFactory.decodeResource(getResources(),
        R.drawable.bubble_4, options);
    bubbles[4] = BitmapFactory.decodeResource(getResources(),
        R.drawable.bubble_5, options);
    bubbles[5] = BitmapFactory.decodeResource(getResources(),
        R.drawable.bubble_6, options);
    bubbles[6] = BitmapFactory.decodeResource(getResources(),
        R.drawable.bubble_7, options);
    bubbles[7] = BitmapFactory.decodeResource(getResources(),
        R.drawable.bubble_8, options);
    background = BitmapFactory.decodeResource(getResources(),
        R.drawable.background2, options);
  }

  public void setGrid(byte[][] level) {
    grid = LevelManager.copyLevel(level);
    invalidate();
  }

  public byte[][] getGrid() {
    return LevelManager.copyLevel(grid);
  }

  public void setColorLimit(int limit) {
    if (limit < LevelManager.EASY) {
      limit = LevelManager.EASY;
    }
    else if (limit > LevelManager.INSANE) {
      limit = LevelManager.INSANE;
    }
    colorLimit = limit;
    invalidate();
  }

  public int getColorLimit() {
    return colorLimit;
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    updateLetterbox(w, h);
  }

  private void updateLetterbox(int w, int h) {
    scale = Math.min(w / (float)GameView.GAMEFIELD_WIDTH,
                     h / (float)GameView.GAMEFIELD_HEIGHT);
    offsetX = (w - GameView.GAMEFIELD_WIDTH * scale) / 2.0f;
    offsetY = (h - GameView.GAMEFIELD_HEIGHT * scale) / 2.0f;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.save();
    canvas.translate(offsetX, offsetY);
    canvas.scale(scale, scale);

    if (background != null) {
      /*
       * The background art is a single 640x480 image shared by both
       * fields in a two-player game (player 1's field occupies its left
       * 320x480 half). Drawing it in full here would bleed player 2's
       * half into view as an empty second field, so crop to just the
       * single-player-sized slice this editor represents.
       */
      int srcWidth  = Math.min(background.getWidth(), GameView.GAMEFIELD_WIDTH);
      int srcHeight = Math.min(background.getHeight(), GameView.GAMEFIELD_HEIGHT);
      Rect src = new Rect(0, 0, srcWidth, srcHeight);
      RectF dst = new RectF(0, 0, GameView.GAMEFIELD_WIDTH, GameView.GAMEFIELD_HEIGHT);
      canvas.drawBitmap(background, src, dst, null);
    }

    Paint emptyPaint = new Paint();
    emptyPaint.setARGB(64, 255, 255, 255);

    for (int j = 0; j < (LevelManager.NUM_ROWS - 1); j++) {
      for (int i = j % 2; i < LevelManager.NUM_COLS; i++) {
        int left = FIELD_LEFT + i * CELL_SIZE - (j % 2) * 16;
        int top = FIELD_TOP + j * ROW_HEIGHT;
        RectF cell = new RectF(left, top, left + CELL_SIZE, top + CELL_SIZE);
        byte cellValue = grid[i][j];
        if (cellValue >= 0 && cellValue < bubbles.length) {
          canvas.drawBitmap(bubbles[cellValue], null, cell, null);
        }
        else {
          canvas.drawRect(cell, emptyPaint);
        }
      }
    }
    canvas.restore();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() != MotionEvent.ACTION_UP) {
      return true;
    }
    int[] cell = mapTouchToCell(event.getX(), event.getY());
    if (cell == null) {
      return true;
    }
    cycleCell(cell[0], cell[1]);
    invalidate();
    return true;
  }

  private int[] mapTouchToCell(float touchX, float touchY) {
    float virtualX = (touchX - offsetX) / scale;
    float virtualY = (touchY - offsetY) / scale;

    int j = (int)((virtualY - FIELD_TOP) / ROW_HEIGHT);
    if (j < 0 || j >= (LevelManager.NUM_ROWS - 1)) {
      return null;
    }
    if (virtualY < FIELD_TOP + j * ROW_HEIGHT ||
        virtualY >= FIELD_TOP + j * ROW_HEIGHT + CELL_SIZE) {
      return null;
    }

    int colStart = j % 2;
    for (int i = colStart; i < LevelManager.NUM_COLS; i++) {
      int left = FIELD_LEFT + i * CELL_SIZE - (j % 2) * 16;
      int top = FIELD_TOP + j * ROW_HEIGHT;
      if (virtualX >= left && virtualX < left + CELL_SIZE &&
          virtualY >= top && virtualY < top + CELL_SIZE) {
        return new int[]{i, j};
      }
    }
    return null;
  }

  private void cycleCell(int i, int j) {
    byte current = grid[i][j];
    if (current == -1) {
      grid[i][j] = 0;
    }
    else if (current >= colorLimit - 1) {
      grid[i][j] = -1;
    }
    else {
      grid[i][j] = (byte)(current + 1);
    }
  }
}
