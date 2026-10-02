# Plan: Rename App & Add 400 New Levels

## Part 1: Rename App to "Enhanced Frozen Bubbles" (Localized)

### 1.1 Localized Names

Each locale gets a translated version of "Enhanced Frozen Bubbles":

| Locale | File | Current `app_name` | New `app_name` |
|--------|------|--------------------|----------------|
| Default (en) | `res/values/strings.xml` | `Frozen Bubble` | `Enhanced Frozen Bubbles` |
| French | `res/values-fr/strings.xml` | `Frozen Bubble` | `Frozen Bubbles Amélioré` |
| German | `res/values-de/strings.xml` | `Frozen Bubble` | `Erweiterte Frozen Bubbles` |
| Spanish | `res/values-es/strings.xml` | `Frozen Bubble` | `Frozen Bubbles Mejorado` |
| Brazilian Portuguese | `res/values-pt-rBR/strings.xml` | `Frozen Bubble` | `Frozen Bubbles Melhorado` |
| Icelandic | `res/values-is/strings.xml` | `Frostkúlur` | `Endurbættur Frostkúlur` |
| Chinese (Simplified) | `res/values-zh-rCN/strings.xml` | `冰冻泡泡` | `增强版冰冻泡泡` |

### 1.2 Strings to Update Per File

Each of the 7 `strings.xml` files has **3 strings** to update:

| String Name | Current Value | New Value (varies by locale) |
|-------------|--------------|------------------------------|
| `app_name` | locale-specific | localized "Enhanced Frozen Bubbles" |
| `title_activity_frozen_bubble` | same as app_name | localized "Enhanced Frozen Bubbles" |
| `title_activity_home_screen` | same as app_name | localized "Enhanced Frozen Bubbles" |

**Total edits: 7 files × 3 strings = 21 string edits**

### 1.3 Detailed File Changes

#### `res/values/strings.xml` (English — default)
- Line 3: `app_name` → `Enhanced Frozen Bubbles`
- Line 27: `title_activity_frozen_bubble` → `Enhanced Frozen Bubbles`
- Line 28: `title_activity_home_screen` → `Enhanced Frozen Bubbles`

#### `res/values-fr/strings.xml` (French)
- Line 3: `app_name` → `Frozen Bubbles Amélioré`
- Line 25: `title_activity_frozen_bubble` → `Frozen Bubbles Amélioré`
- Line 26: `title_activity_home_screen` → `Frozen Bubbles Amélioré`

#### `res/values-de/strings.xml` (German)
- Line 3: `app_name` → `Erweiterte Frozen Bubbles`
- Line 25: `title_activity_frozen_bubble` → `Erweiterte Frozen Bubbles`
- Line 26: `title_activity_home_screen` → `Erweiterte Frozen Bubbles`

#### `res/values-es/strings.xml` (Spanish)
- Line 3: `app_name` → `Frozen Bubbles Mejorado`
- Line 25: `title_activity_frozen_bubble` → `Frozen Bubbles Mejorado`
- Line 26: `title_activity_home_screen` → `Frozen Bubbles Mejorado`

#### `res/values-pt-rBR/strings.xml` (Brazilian Portuguese)
- Line 3: `app_name` → `Frozen Bubbles Melhorado`
- Line 25: `title_activity_frozen_bubble` → `Frozen Bubbles Melhorado`
- Line 26: `title_activity_home_screen` → `Frozen Bubbles Melhorado`

#### `res/values-is/strings.xml` (Icelandic)
- Line 3: `app_name` → `Endurbættur Frostkúlur`
- Line 25: `title_activity_frozen_bubble` → `Endurbættur Frostkúlur`
- Line 26: `title_activity_home_screen` → `Endurbættur Frostkúlur`

#### `res/values-zh-rCN/strings.xml` (Chinese Simplified)
- Line 3: `app_name` → `增强版冰冻泡泡`
- Line 25: `title_activity_frozen_bubble` → `增强版冰冻泡泡`
- Line 26: `title_activity_home_screen` → `增强版冰冻泡泡`

### 1.4 Files NOT Changed

- `AndroidManifest.xml` — references `@string/app_name`, picks up changes automatically
- `build.gradle` — `applicationId 'org.jfedor.frozenbubble'` stays unchanged (internal package ID, not display name)

---

## Part 2: Generate & Add 400 New Levels

### 2.1 Level Format Specification

Each level in `assets/levels.txt` is a **10-row hex grid**:

```
6   6   4   4   2   2   3   3        ← even row (0,2,4...): 8 columns
  6   6   4   4   2   2   3          ← odd row (1,3,5...): 7 columns, 2-space indent
-   -   -   -   -   -   -   -        ← dash = empty cell
```

- **Values:** `0`–`7` = bubble color index, `-` = empty
- **Column separator:** 3 spaces
- **Level separator:** blank line (`\n\n`)
- **Internal representation:** `byte[NUM_COLS][NUM_ROWS-1]` = `byte[8][12]`
- **Constants from `LevelManager.java`:** `NUM_COLS=8`, `NUM_ROWS=13`, `VS_ROWS=5`

### 2.2 Why No Code Changes Are Needed

| Concern | How It's Handled |
|---------|------------------|
| Level count | `LevelManager` parses `levels.txt` by splitting on `\n\n` — **dynamically counted**, no hardcoded 100 |
| Level wrap-around | `goToNextLevel()` wraps at `levelList.size()` — auto-wraps at 500 |
| Music rotation | `currentLevelIndex % MODlist.length` (17 songs) — **works automatically** for any level count |
| Level advancement | `goToNextLevel()` increments and wraps — no changes needed |

### 2.3 Level Generation Algorithm

Write a Python script (`generate_levels.py`) that creates 400 levels with progressive difficulty:

#### Difficulty Progression (by level range)

| Levels | Filled Rows | Colors | Fill Rate | Style |
|--------|-------------|--------|-----------|-------|
| 101–160 | 4–5 | 4–5 | 60–75% | Simple geometric (rectangles, triangles) |
| 161–240 | 5–7 | 5–6 | 65–80% | Mixed shapes with some gaps |
| 241–350 | 6–8 | 6–7 | 70–85% | Complex clusters, asymmetric shapes |
| 351–500 | 7–10 | 7–8 | 75–90% | Dense, many colors, challenging layouts |

#### Shape Templates (randomly selected per level)

1. **Full block** — rectangular fill of top N rows
2. **Triangle** — centered triangle pointing down
3. **Diamond** — centered diamond shape
4. **V-shape** — inverted V from top corners
5. **Random clusters** — scattered groups of 2–4 connected bubbles
6. **Mirror** — left-right symmetric layout (pick one half, mirror it)
7. **Frame** — outer ring with hollow center
8. **Staircase** — descending steps from left or right

Each level randomly picks 1–2 templates and combines them.

#### Playability Constraints

- Every bubble must be **connected to the top row** (directly or via adjacent bubbles)
- At least **2 distinct colors** present per level
- No completely empty levels
- At least **1 row with ≥4 filled cells** (ensures there's something to play)

#### Random Seed

- Use a fixed seed (e.g., `seed=42`) for reproducibility
- Each level advances the RNG state so levels are deterministic

### 2.4 Implementation Steps

1. **Write `generate_levels.py`** at project root:
   - Reads existing `assets/levels.txt` (to validate format compatibility)
   - Generates 400 level strings in exact text format
   - Validates each level meets playability constraints
   - Outputs just the new levels to stdout or a temp file

2. **Append to `assets/levels.txt`:**
   - Add `\n` separator + 400 new level blocks after the existing 100 levels
   - Result: 500 total levels, no other file changes

3. **Delete `generate_levels.py`** (temporary utility, not part of the app)

### 2.5 Music Rotation for New Levels

Already handled automatically by the `currentLevelIndex % 17` logic in `FrozenBubble.java`:

- Levels 101–117 → songs 0–16
- Levels 118–134 → songs 0–16 (wraps)
- ...continues cycling through all 17 MOD/XM tracks

No changes needed.

---

## Summary

| Task | Files Modified | Type |
|------|----------------|------|
| Rename app (localized) | `res/values/strings.xml` | Edit 3 strings |
| Rename app (localized) | `res/values-fr/strings.xml` | Edit 3 strings |
| Rename app (localized) | `res/values-de/strings.xml` | Edit 3 strings |
| Rename app (localized) | `res/values-es/strings.xml` | Edit 3 strings |
| Rename app (localized) | `res/values-pt-rBR/strings.xml` | Edit 3 strings |
| Rename app (localized) | `res/values-is/strings.xml` | Edit 3 strings |
| Rename app (localized) | `res/values-zh-rCN/strings.xml` | Edit 3 strings |
| Add 400 levels | `assets/levels.txt` | Append 400 levels |
| Level generator | `generate_levels.py` | New (temporary) |

**Total: 7 strings.xml edits (21 string changes) + append to levels.txt + temporary Python script**

**Zero Java/Kotlin code changes required.**
