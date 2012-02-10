/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.inputmethod.keyboard;

import android.graphics.Paint;

import com.android.inputmethod.keyboard.internal.KeySpecParser;
import com.android.inputmethod.latin.R;

public class MoreKeysKeyboard extends Keyboard {
    private final int mDefaultKeyCoordX;

    MoreKeysKeyboard(Builder.MoreKeysKeyboardParams params) {
        super(params);
        mDefaultKeyCoordX = params.getDefaultKeyCoordX() + params.mDefaultKeyWidth / 2;
    }

    public int getDefaultCoordX() {
        return mDefaultKeyCoordX;
    }

    public static class Builder extends Keyboard.Builder<Builder.MoreKeysKeyboardParams> {
        private final String[] mMoreKeys;

        public static class MoreKeysKeyboardParams extends Keyboard.Params {
            /* package */int mTopRowAdjustment;
            public int mNumRows;
            public int mNumColumns;
            public int mLeftKeys;
            public int mRightKeys; // includes default key.

            public MoreKeysKeyboardParams() {
                super();
            }

            /* package for test */MoreKeysKeyboardParams(int numKeys, int maxColumns, int keyWidth,
                    int rowHeight, int coordXInParent, int parentKeyboardWidth) {
                super();
                setParameters(numKeys, maxColumns, keyWidth, rowHeight, coordXInParent,
                        parentKeyboardWidth);
            }

            /**
             * Set keyboard parameters of more keys keyboard.
             *
             * @param numKeys number of keys in this more keys keyboard.
             * @param maxColumns number of maximum columns of this more keys keyboard.
             * @param keyWidth more keys keyboard key width in pixel, including horizontal gap.
             * @param rowHeight more keys keyboard row height in pixel, including vertical gap.
             * @param coordXInParent coordinate x of the key preview in parent keyboard.
             * @param parentKeyboardWidth parent keyboard width in pixel.
             */
            public void setParameters(int numKeys, int maxColumns, int keyWidth, int rowHeight,
                    int coordXInParent, int parentKeyboardWidth) {
                if (parentKeyboardWidth / keyWidth < maxColumns) {
                    throw new IllegalArgumentException(
                            "Keyboard is too small to hold more keys keyboard: "
                                    + parentKeyboardWidth + " " + keyWidth + " " + maxColumns);
                }
                mDefaultKeyWidth = keyWidth;
                mDefaultRowHeight = rowHeight;

                final int numRows = (numKeys + maxColumns - 1) / maxColumns;
                mNumRows = numRows;
                final int numColumns = getOptimizedColumns(numKeys, maxColumns);
                mNumColumns = numColumns;

                final int numLeftKeys = (numColumns - 1) / 2;
                final int numRightKeys = numColumns - numLeftKeys; // including default key.
                // Maximum number of keys we can layout both side of the parent key
                final int maxLeftKeys = coordXInParent / keyWidth;
                final int maxRightKeys = (parentKeyboardWidth - coordXInParent) / keyWidth;
                int leftKeys, rightKeys;
                if (numLeftKeys > maxLeftKeys) {
                    leftKeys = maxLeftKeys;
                    rightKeys = numColumns - leftKeys;
                } else if (numRightKeys > maxRightKeys + 1) {
                    rightKeys = maxRightKeys + 1; // include default key
                    leftKeys = numColumns - rightKeys;
                } else {
                    leftKeys = numLeftKeys;
                    rightKeys = numRightKeys;
                }
                // If the left keys fill the left side of the parent key, entire more keys keyboard
                // should be shifted to the right unless the parent key is on the left edge.
                if (maxLeftKeys == leftKeys && leftKeys > 0) {
                    leftKeys--;
                    rightKeys++;
                }
                // If the right keys fill the right side of the parent key, entire more keys
                // should be shifted to the left unless the parent key is on the right edge.
                if (maxRightKeys == rightKeys - 1 && rightKeys > 1) {
                    leftKeys++;
                    rightKeys--;
                }
                mLeftKeys = leftKeys;
                mRightKeys = rightKeys;

                // Centering of the top row.
                if (numRows < 2 || getTopRowEmptySlots(numKeys, numColumns) % 2 == 0) {
                    mTopRowAdjustment = 0;
                } else if (mLeftKeys < mRightKeys - 1) {
                    mTopRowAdjustment = 1;
                } else {
                    mTopRowAdjustment = -1;
                }

                mBaseWidth = mOccupiedWidth = mNumColumns * mDefaultKeyWidth;
                // Need to subtract the bottom row's gutter only.
                mBaseHeight = mOccupiedHeight = mNumRows * mDefaultRowHeight - mVerticalGap
                        + mTopPadding + mBottomPadding;
            }

            // Return key position according to column count (0 is default).
            /* package */int getColumnPos(int n) {
                final int col = n % mNumColumns;
                if (col == 0) {
                    // default position.
                    return 0;
                }
                int pos = 0;
                int right = 1; // include default position key.
                int left = 0;
                int i = 0;
                while (true) {
                    // Assign right key if available.
                    if (right < mRightKeys) {
                        pos = right;
                        right++;
                        i++;
                    }
                    if (i >= col)
                        break;
                    // Assign left key if available.
                    if (left < mLeftKeys) {
                        left++;
                        pos = -left;
                        i++;
                    }
                    if (i >= col)
                        break;
                }
                return pos;
            }

            private static int getTopRowEmptySlots(int numKeys, int numColumns) {
                final int remainingKeys = numKeys % numColumns;
                if (remainingKeys == 0) {
                    return 0;
                } else {
                    return numColumns - remainingKeys;
                }
            }

            private int getOptimizedColumns(int numKeys, int maxColumns) {
                int numColumns = Math.min(numKeys, maxColumns);
                while (getTopRowEmptySlots(numKeys, numColumns) >= mNumRows) {
                    numColumns--;
                }
                return numColumns;
            }

            public int getDefaultKeyCoordX() {
                return mLeftKeys * mDefaultKeyWidth;
            }

            public int getX(int n, int row) {
                final int x = getColumnPos(n) * mDefaultKeyWidth + getDefaultKeyCoordX();
                if (isTopRow(row)) {
                    return x + mTopRowAdjustment * (mDefaultKeyWidth / 2);
                }
                return x;
            }

            public int getY(int row) {
                return (mNumRows - 1 - row) * mDefaultRowHeight + mTopPadding;
            }

            public void markAsEdgeKey(Key key, int row) {
                if (row == 0)
                    key.markAsTopEdge(this);
                if (isTopRow(row))
                    key.markAsBottomEdge(this);
            }

            private boolean isTopRow(int rowCount) {
                return rowCount == mNumRows - 1;
            }
        }

        public Builder(KeyboardView view, int xmlId, Key parentKey, Keyboard parentKeyboard) {
            super(view.getContext(), new MoreKeysKeyboardParams());
            load(xmlId, parentKeyboard.mId);

            // TODO: More keys keyboard's vertical gap is currently calculated heuristically.
            // Should revise the algorithm.
            mParams.mVerticalGap = parentKeyboard.mVerticalGap / 2;
            mMoreKeys = parentKey.mMoreKeys;

            final int previewWidth = view.mKeyPreviewDrawParams.mPreviewBackgroundWidth;
            final int previewHeight = view.mKeyPreviewDrawParams.mPreviewBackgroundHeight;
            final int width, height;
            // Use pre-computed width and height if these values are available and more keys
            // keyboard has only one key to mitigate visual flicker between key preview and more
            // keys keyboard.
            if (view.isKeyPreviewPopupEnabled() && mMoreKeys.length == 1 && previewWidth > 0
                    && previewHeight > 0) {
                width = previewWidth;
                height = previewHeight + mParams.mVerticalGap;
            } else {
                width = getMaxKeyWidth(view, parentKey.mMoreKeys, mParams.mDefaultKeyWidth);
                height = parentKeyboard.mMostCommonKeyHeight;
            }
            mParams.setParameters(mMoreKeys.length, parentKey.mMaxMoreKeysColumn, width, height,
                    parentKey.mX + parentKey.mWidth / 2, view.getMeasuredWidth());
        }

        private static int getMaxKeyWidth(KeyboardView view, String[] moreKeys, int minKeyWidth) {
            final int padding = (int) view.getContext().getResources()
                    .getDimension(R.dimen.more_keys_keyboard_key_horizontal_padding);
            Paint paint = null;
            int maxWidth = minKeyWidth;
            for (String moreKeySpec : moreKeys) {
                final String label = KeySpecParser.getLabel(moreKeySpec);
                // If the label is single letter, minKeyWidth is enough to hold the label.
                if (label != null && label.length() > 1) {
                    if (paint == null) {
                        paint = new Paint();
                        paint.setAntiAlias(true);
                    }
                    final int width = (int)view.getDefaultLabelWidth(label, paint) + padding;
                    if (maxWidth < width) {
                        maxWidth = width;
                    }
                }
            }
            return maxWidth;
        }

        @Override
        public MoreKeysKeyboard build() {
            final MoreKeysKeyboardParams params = mParams;
            for (int n = 0; n < mMoreKeys.length; n++) {
                final String moreKeySpec = mMoreKeys[n];
                final int row = n / params.mNumColumns;
                final Key key = new Key(mResources, params, moreKeySpec, params.getX(n, row),
                        params.getY(row), params.mDefaultKeyWidth, params.mDefaultRowHeight);
                params.markAsEdgeKey(key, row);
                params.onAddKey(key);
            }
            return new MoreKeysKeyboard(params);
        }
    }
}
