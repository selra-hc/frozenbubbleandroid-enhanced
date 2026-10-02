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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Lists every level in the catalog by number. Tapping a level opens
 * {@link LevelActionsActivity}, which offers the actions (edit, test,
 * delete, reorder) for that one level.
 */
public class LevelPackManager extends Activity {

  private LinearLayout levelListLayout;
  private List<byte[][]> levels;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                         WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setTitle(R.string.title_activity_level_pack_manager);
    buildUi();
  }

  @Override
  protected void onResume() {
    super.onResume();
    /*
     * Reload every time this screen becomes visible, so returning from
     * LevelActionsActivity (after an edit, delete, or reorder) or from
     * "New Level" always reflects the catalog's current state.
     */
    reloadLevels();
  }

  private void buildUi() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);

    ScrollView scrollView = new ScrollView(this);
    levelListLayout = new LinearLayout(this);
    levelListLayout.setOrientation(LinearLayout.VERTICAL);
    scrollView.addView(levelListLayout);
    root.addView(scrollView, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));

    LinearLayout actions = new LinearLayout(this);
    actions.setOrientation(LinearLayout.VERTICAL);

    actions.addView(makeActionButton(R.string.level_pack_new_level,
        new View.OnClickListener() {
          public void onClick(View v) {
            openNewLevelEditor();
          }
        }));

    actions.addView(makeActionButton(R.string.level_pack_reset,
        new View.OnClickListener() {
          public void onClick(View v) {
            confirmReset();
          }
        }));

    actions.addView(makeActionButton(R.string.editor_back,
        new View.OnClickListener() {
          public void onClick(View v) {
            finish();
          }
        }));

    root.addView(actions, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT));

    setContentView(root);
  }

  private Button makeActionButton(int labelRes, View.OnClickListener listener) {
    Button button = new Button(this);
    button.setText(labelRes);
    button.setOnClickListener(listener);
    return button;
  }

  private void reloadLevels() {
    try {
      levels = LevelStore.mutableCopy(this);
    } catch (IOException e) {
      levels = new java.util.ArrayList<byte[][]>();
      Toast.makeText(this, R.string.level_pack_load_failed, Toast.LENGTH_SHORT).show();
    }
    refreshLevelList();
  }

  private void refreshLevelList() {
    levelListLayout.removeAllViews();
    for (int i = 0; i < levels.size(); i++) {
      levelListLayout.addView(buildLevelRow(i));
    }
    if (levels.isEmpty()) {
      TextView empty = new TextView(this);
      empty.setText(R.string.level_pack_empty);
      levelListLayout.addView(empty);
    }
  }

  private View buildLevelRow(final int index) {
    Button row = new Button(this);
    row.setText(getString(R.string.level_pack_level_label, index + 1));
    row.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        openLevelActions(index);
      }
    });
    return row;
  }

  private void openLevelActions(int index) {
    Intent intent = new Intent(this, LevelActionsActivity.class);
    intent.putExtra(LevelActionsActivity.EXTRA_LEVEL_INDEX, index);
    startActivity(intent);
  }

  private void openNewLevelEditor() {
    Intent intent = new Intent(this, LevelEditorActivity.class);
    intent.putExtra(LevelEditorActivity.EXTRA_LEVEL_INDEX, -1);
    intent.putExtra(LevelEditorActivity.EXTRA_FROM_CUSTOM_PACK, false);
    startActivity(intent);
  }

  private void confirmReset() {
    new AlertDialog.Builder(this)
        .setTitle(R.string.level_pack_reset)
        .setMessage(R.string.level_pack_reset_confirm)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            try {
              LevelStore.resetToDefaults(LevelPackManager.this);
              reloadLevels();
            } catch (IOException e) {
              Toast.makeText(LevelPackManager.this,
                  R.string.editor_save_failed, Toast.LENGTH_SHORT).show();
            }
          }
        })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }
}
