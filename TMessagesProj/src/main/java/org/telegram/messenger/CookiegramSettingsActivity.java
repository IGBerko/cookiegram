package org.telegram.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.CookiegramConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.Locale;

public class CookiegramSettingsActivity extends BaseFragment {

    private static final int VIEW_HEADER = 0, VIEW_CHECK = 1, VIEW_INFO = 2;

    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;

    private int rowCount;
    private int designHeaderRow;
    private int toggleDesignRow;
    private int designInfoRow;

    private final String highlightKey;

    public CookiegramSettingsActivity() {
        this(null);
    }

    public CookiegramSettingsActivity(String highlightKey) {
        this.highlightKey = highlightKey;
    }

    @Override
    public boolean onFragmentCreate() {
        rowCount = 0;
        designHeaderRow = rowCount++;
        toggleDesignRow = rowCount++;
        designInfoRow = rowCount++;
        return super.onFragmentCreate();
    }

    // Новая настройка: добавь строку выше и case здесь.
    private int rowForKey(String key) {
        if (key == null) {
            return -1;
        }
        switch (key.toLowerCase(Locale.ROOT)) {
            case "design":
                return designHeaderRow;
            case "design/toggledesign":
                return toggleDesignRow;
        }
        return -1;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Cookiegram");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(new ListAdapter(context));
        actionBar.setAdaptiveBackground(listView);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position == toggleDesignRow) {
                boolean value = !CookiegramConfig.isOldDesign();
                CookiegramConfig.setOldDesign(value);
                ((TextCheckCell) view).setChecked(value);
            }
        });

        final int target = rowForKey(highlightKey);
        if (target >= 0) {
            AndroidUtilities.runOnUIThread(() -> {
                if (listView == null) {
                    return;
                }
                layoutManager.scrollToPositionWithOffset(target, AndroidUtilities.dp(120));
                AndroidUtilities.runOnUIThread(() -> flashRow(target), 150);
            }, 300);
        }
        return fragmentView;
    }

    private void flashRow(int position) {
        if (listView == null) {
            return;
        }
        RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(position);
        if (holder == null) {
            return;
        }
        final View view = holder.itemView;
        final int base = Theme.getColor(Theme.key_windowBackgroundWhite);
        final int accent = ColorUtils.blendARGB(base, Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader), 0.25f);
        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), base, accent, base, accent, base);
        animator.setDuration(1600);
        animator.addUpdateListener(a -> view.setBackgroundColor((int) a.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setBackgroundColor(base);
            }
        });
        animator.start();
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context context;

        ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getAdapterPosition() == toggleDesignRow;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case VIEW_HEADER:
                    view = new HeaderCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_CHECK:
                    view = new TextCheckCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                default:
                    view = new TextInfoPrivacyCell(context);
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case VIEW_HEADER:
                    ((HeaderCell) holder.itemView).setText("Дизайн");
                    break;
                case VIEW_CHECK:
                    ((TextCheckCell) holder.itemView).setTextAndCheck("Старый дизайн", CookiegramConfig.isOldDesign(), false);
                    break;
                default:
                    ((TextInfoPrivacyCell) holder.itemView).setText("Без нижней панели, с боковым меню.");
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == designHeaderRow) {
                return VIEW_HEADER;
            } else if (position == toggleDesignRow) {
                return VIEW_CHECK;
            }
            return VIEW_INFO;
        }
    }
}