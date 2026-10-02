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

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class LevelEditorActivity extends Activity {

  public static final String EXTRA_LEVEL_DATA = "levelData";
  public static final String EXTRA_LEVEL_INDEX = "levelIndex";
  public static final String EXTRA_FROM_CUSTOM_PACK = "fromCustomPack";

  private LevelEditorView editorView;
  private int packIndex = -1;
  private boolean hasKnownIndex = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                         WindowManager.LayoutParams.FLAG_FULLSCREEN);

    packIndex = getIntent().getIntExtra(EXTRA_LEVEL_INDEX, -1);
    hasKnownIndex = getIntent().getBooleanExtra(EXTRA_FROM_CUSTOM_PACK, false);

    byte[][] grid = LevelManager.newEmptyLevel();
    String levelData = getIntent().getStringExtra(EXTRA_LEVEL_DATA);
    if (levelData != null) {
      LevelManager parsed = new LevelManager(levelData.getBytes(), 0);
      byte[][] parsedGrid = parsed.getCurrentLevel();
      if (parsedGrid != null) {
        grid = parsedGrid;
      }
    }

    editorView = new LevelEditorView(this);
    editorView.setGrid(grid);

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);

    TextView colorLabel = new TextView(this);
    colorLabel.setText(R.string.editor_color_limit);
    root.addView(colorLabel, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT));

    LinearLayout colorRow = buildColorLimitRow();
    root.addView(colorRow, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT));

    root.addView(editorView, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));

    LinearLayout buttonRow = new LinearLayout(this);
    buttonRow.setOrientation(LinearLayout.HORIZONTAL);

    Button saveButton = new Button(this);
    saveButton.setText(R.string.editor_save);
    saveButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        saveLevel(false);
      }
    });
    buttonRow.addView(saveButton);

    Button saveAsNewButton = new Button(this);
    saveAsNewButton.setText(R.string.editor_save_as_new);
    saveAsNewButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        saveLevel(true);
      }
    });
    buttonRow.addView(saveAsNewButton);

    Button testButton = new Button(this);
    testButton.setText(R.string.editor_test);
    testButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        testLevel();
      }
    });
    buttonRow.addView(testButton);

    if (hasKnownIndex) {
      Button resetButton = new Button(this);
      resetButton.setText(R.string.editor_reset_level);
      resetButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          confirmResetToDefault();
        }
      });
      buttonRow.addView(resetButton);
    }

    Button backButton = new Button(this);
    backButton.setText(R.string.editor_back);
    backButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        finish();
      }
    });
    buttonRow.addView(backButton);

    root.addView(buttonRow, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT));

    setContentView(root);
    setTitle(R.string.title_activity_level_editor);
  }

  private LinearLayout buildColorLimitRow() {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);

    final TextView valueView = new TextView(this);
    valueView.setText(String.valueOf(editorView.getColorLimit()));

    Button lessButton = new Button(this);
    lessButton.setText("-");
    lessButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        editorView.setColorLimit(editorView.getColorLimit() - 1);
        valueView.setText(String.valueOf(editorView.getColorLimit()));
      }
    });
    row.addView(lessButton);

    row.addView(valueView);

    Button moreButton = new Button(this);
    moreButton.setText("+");
    moreButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        editorView.setColorLimit(editorView.getColorLimit() + 1);
        valueView.setText(String.valueOf(editorView.getColorLimit()));
      }
    });
    row.addView(moreButton);

    return row;
  }

  private void saveLevel(boolean forceNew) {
    try {
      List<byte[][]> levels = LevelStore.mutableCopy(this);
      byte[][] grid = editorView.getGrid();
      if (!forceNew && hasKnownIndex && packIndex >= 0 &&
          packIndex < levels.size()) {
        levels.set(packIndex, grid);
      }
      else {
        levels.add(grid);
        packIndex = levels.size() - 1;
        hasKnownIndex = true;
      }
      LevelStore.save(this, levels);
      Toast.makeText(this, R.string.editor_saved, Toast.LENGTH_SHORT).show();
      setResult(RESULT_OK);
    } catch (IOException e) {
      Toast.makeText(this, R.string.editor_save_failed, Toast.LENGTH_SHORT).show();
    }
  }

  private void confirmResetToDefault() {
    new AlertDialog.Builder(this)
        .setTitle(R.string.editor_reset_level)
        .setMessage(R.string.editor_reset_level_confirm)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            resetToDefault();
          }
        })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  /**
   * Reverts just this level's grid to its original seeded content, i.e.
   * what it looked like before any in-app edits. Only touches the level
   * currently open in this screen; the change is not persisted until the
   * user hits Save.
   */
  private void resetToDefault() {
    try {
      byte[][] defaultGrid = LevelStore.getDefaultLevel(this, packIndex);
      if (defaultGrid == null) {
        Toast.makeText(this, R.string.editor_reset_no_default, Toast.LENGTH_SHORT).show();
        return;
      }
      editorView.setGrid(defaultGrid);
      Toast.makeText(this, R.string.editor_reset_done, Toast.LENGTH_SHORT).show();
    } catch (IOException e) {
      Toast.makeText(this, R.string.editor_save_failed, Toast.LENGTH_SHORT).show();
    }
  }

  private void testLevel() {
    Intent intent = new Intent(FrozenBubble.EDITORACTION);
    intent.setClass(this, FrozenBubble.class);
    intent.putExtra("levels",
        LevelManager.serializeLevel(editorView.getGrid()).getBytes());
    intent.putExtra("startingLevel", 0);
    startActivity(intent);
  }
}
