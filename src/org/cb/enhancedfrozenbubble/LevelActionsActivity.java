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

/**
 * The actions (edit, test, delete, reorder) available for a single level
 * in the {@link LevelStore} catalog, reached by tapping a level number in
 * {@link LevelPackManager}.
 */
public class LevelActionsActivity extends Activity {

  public static final String EXTRA_LEVEL_INDEX = "levelIndex";

  private int index;
  private TextView titleView;
  private Button editButton;
  private Button testButton;
  private Button upButton;
  private Button downButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                         WindowManager.LayoutParams.FLAG_FULLSCREEN);
    index = getIntent().getIntExtra(EXTRA_LEVEL_INDEX, -1);
    buildUi();
  }

  @Override
  protected void onResume() {
    super.onResume();
    refresh();
  }

  private void buildUi() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);

    titleView = new TextView(this);
    root.addView(titleView, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT));

    editButton = makeActionButton(R.string.level_pack_edit,
        new View.OnClickListener() {
          public void onClick(View v) {
            openEditor();
          }
        });
    root.addView(editButton);

    testButton = makeActionButton(R.string.editor_test,
        new View.OnClickListener() {
          public void onClick(View v) {
            testLevel();
          }
        });
    root.addView(testButton);

    upButton = makeActionButton(R.string.level_pack_move_up,
        new View.OnClickListener() {
          public void onClick(View v) {
            moveLevel(-1);
          }
        });
    root.addView(upButton);

    downButton = makeActionButton(R.string.level_pack_move_down,
        new View.OnClickListener() {
          public void onClick(View v) {
            moveLevel(1);
          }
        });
    root.addView(downButton);

    root.addView(makeActionButton(R.string.level_pack_delete,
        new View.OnClickListener() {
          public void onClick(View v) {
            confirmDelete();
          }
        }));

    root.addView(makeActionButton(R.string.editor_back,
        new View.OnClickListener() {
          public void onClick(View v) {
            finish();
          }
        }));

    setContentView(root);
  }

  private Button makeActionButton(int labelRes, View.OnClickListener listener) {
    Button button = new Button(this);
    button.setText(labelRes);
    button.setOnClickListener(listener);
    return button;
  }

  /**
   * Reloads the catalog and updates this screen's title/button state for
   * the current index. Finishes if the level no longer exists (e.g. it
   * was deleted from another path while this screen was backgrounded).
   */
  private void refresh() {
    List<byte[][]> levels;
    try {
      levels = LevelStore.loadLevelList(this);
    } catch (IOException e) {
      Toast.makeText(this, R.string.level_pack_load_failed, Toast.LENGTH_SHORT).show();
      finish();
      return;
    }
    if (index < 0 || index >= levels.size()) {
      finish();
      return;
    }
    titleView.setText(getString(R.string.level_pack_level_label, index + 1));
    setTitle(getString(R.string.level_pack_level_label, index + 1));
    upButton.setEnabled(index > 0);
    downButton.setEnabled(index < levels.size() - 1);
  }

  private void openEditor() {
    try {
      List<byte[][]> levels = LevelStore.loadLevelList(this);
      if (index < 0 || index >= levels.size()) {
        return;
      }
      Intent intent = new Intent(this, LevelEditorActivity.class);
      intent.putExtra(LevelEditorActivity.EXTRA_LEVEL_DATA,
          LevelManager.serializeLevel(levels.get(index)));
      intent.putExtra(LevelEditorActivity.EXTRA_LEVEL_INDEX, index);
      intent.putExtra(LevelEditorActivity.EXTRA_FROM_CUSTOM_PACK, true);
      startActivity(intent);
    } catch (IOException e) {
      Toast.makeText(this, R.string.level_pack_load_failed, Toast.LENGTH_SHORT).show();
    }
  }

  private void testLevel() {
    try {
      List<byte[][]> levels = LevelStore.loadLevelList(this);
      Intent intent = new Intent(FrozenBubble.EDITORACTION);
      intent.setClass(this, FrozenBubble.class);
      intent.putExtra("levels", LevelManager.serializeLevels(levels));
      intent.putExtra("startingLevel", index);
      intent.putExtra("catalogIndexReliable", true);
      startActivity(intent);
    } catch (IOException e) {
      Toast.makeText(this, R.string.level_pack_load_failed, Toast.LENGTH_SHORT).show();
    }
  }

  private void moveLevel(int delta) {
    try {
      List<byte[][]> levels = LevelStore.mutableCopy(this);
      int to = index + delta;
      if (to < 0 || to >= levels.size()) {
        return;
      }
      byte[][] level = levels.remove(index);
      levels.add(to, level);
      LevelStore.save(this, levels);
      index = to;
      refresh();
    } catch (IOException e) {
      Toast.makeText(this, R.string.editor_save_failed, Toast.LENGTH_SHORT).show();
    }
  }

  private void confirmDelete() {
    new AlertDialog.Builder(this)
        .setTitle(R.string.level_pack_delete)
        .setMessage(R.string.level_pack_delete_confirm)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            deleteLevel();
          }
        })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  private void deleteLevel() {
    try {
      List<byte[][]> levels = LevelStore.mutableCopy(this);
      if (index < 0 || index >= levels.size()) {
        finish();
        return;
      }
      levels.remove(index);
      LevelStore.save(this, levels);
      finish();
    } catch (IOException e) {
      Toast.makeText(this, R.string.editor_save_failed, Toast.LENGTH_SHORT).show();
    }
  }
}
