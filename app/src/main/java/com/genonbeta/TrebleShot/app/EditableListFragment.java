package com.genonbeta.TrebleShot.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.collection.ArrayMap;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.ui.callback.DetachListener;
import com.genonbeta.TrebleShot.ui.callback.PowerfulActionModeSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.TrebleShot.widget.EditableListAdapterImpl;
import com.genonbeta.TrebleShot.widget.recyclerview.PaddingItemDecoration;
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeTouchSelectionListener;
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment;
import com.genonbeta.android.framework.widget.PowerfulActionMode;
import com.genonbeta.android.framework.widget.recyclerview.FastScroller;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * created by: Veli
 * date: 21.11.2017 10:12
 */

abstract public class EditableListFragment<T extends Editable, V extends EditableListAdapter.EditableViewHolder, E extends EditableListAdapter<T, V>>
        extends DynamicRecyclerViewFragment<T, V, E>
        implements EditableListFragmentImpl<T>, EditableListFragmentModelImpl<V>, DetachListener
{
    private SelectionCallback<T> mSelectionCallback;
    private SelectionCallback<T> mDefaultSelectionCallback;
    private PowerfulActionMode.SelectorConnection<T> mSelectionConnection;
    private PowerfulActionMode.SelectorConnection<T> mDefaultSelectionConnection;
    private FilteringDelegate<T> mFilteringDelegate;
    private Snackbar mRefreshDelayedSnackbar;
    private boolean mRefreshRequested = false;
    private boolean mSortingSupported = true;
    private boolean mFilteringSupported = false;
    private boolean mUseDefaultPaddingDecoration = false;
    private boolean mUseDefaultPaddingDecorationSpaceForEdges = true;
    private float mDefaultPaddingDecorationSize = -1;
    private int mDefaultOrderingCriteria = EditableListAdapter.MODE_SORT_ORDER_ASCENDING;
    private int mDefaultSortingCriteria = EditableListAdapter.MODE_SORT_BY_NAME;
    private int mDefaultViewingGridSize = 1;
    private int mDefaultViewingGridSizeLandscape = 1;
    private int mDividerResId = R.id.abstract_layout_fast_scroll_recyclerview_bottom_divider;
    private FastScroller mFastScroller;
    private Map<String, Integer> mSortingOptions = new ArrayMap<>();
    private Map<String, Integer> mOrderingOptions = new ArrayMap<>();
    private ContentObserver mObserver;
    private LayoutClickListener<V> mLayoutClickListener;
    private String mSearchText;
    private FilteringDelegate<T> mDefaultFilteringDelegate = new FilteringDelegate<T>()
    {
        @Override
        public boolean changeFilteringKeyword(@Nullable String keyword)
        {
            mSearchText = keyword;
            return true;
        }

        @Nullable
        @Override
        public String[] getFilteringKeyword(EditableListFragmentImpl<T> listFragment)
        {
            if (mSearchText != null && mSearchText.length() > 0)
                return mSearchText.split(" ");

            return null;
        }
    };

    abstract public boolean onDefaultClickAction(V holder);

    public boolean onDefaultLongClickAction(V holder)
    {
        return false;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getAdapter().setFragment(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        if (getPowerfulActionMode() != null && getSelectionCallback() != null)
            setDefaultSelectionConnection(new PowerfulActionMode.SelectorConnection<>(getPowerfulActionMode(), getSelectionCallback()));

        setHasOptionsMenu(true);

        if (mUseDefaultPaddingDecoration) {
            float padding = mDefaultPaddingDecorationSize > -1
                    ? mDefaultPaddingDecorationSize
                    : getResources().getDimension(R.dimen.padding_list_content_parent_layout);

            getListView().addItemDecoration(new PaddingItemDecoration((int) padding,
                    mUseDefaultPaddingDecorationSpaceForEdges, isHorizontalOrientation()));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        getAdapter().notifyGridSizeUpdate(getViewingGridSize(), isScreenLarge());
        getAdapter().setSortingCriteria(getSortingCriteria(), getOrderingCriteria());

        // We have to recreate the provider class because old one loses its ground
        getFastScroller().setViewProvider(new LongTextBubbleFastScrollViewProvider());
        setDividerVisible(true);
        getListView().addOnItemTouchListener(new SwipeTouchSelectionListener<>(this));
    }

    @Override
    protected RecyclerView onListView(View mainContainer, ViewGroup listViewContainer)
    {
        super.onListView(mainContainer, listViewContainer);
        View view = getLayoutInflater().inflate(R.layout.abstract_layout_fast_scroll_recyclerview_container, null, false);

        ViewGroup recyclerViewContainer = view.findViewById(R.id.abstract_layout_fast_scroll_recyclerview_container);
        RecyclerView recyclerView = onListView(recyclerViewContainer);
        mFastScroller = view.findViewById(R.id.abstract_layout_fast_scroll_recyclerview_fastscroll_view);

        // TODO: 1/18/19 Something like onSetListView method would be more safe to set the layout manager etc.
        recyclerView.setLayoutManager(onLayoutManager());

        listViewContainer.addView(view);

        return recyclerView;
    }

    protected RecyclerView onListView(ViewGroup container)
    {
        RecyclerView view = (RecyclerView) getLayoutInflater()
                .inflate(R.layout.abstract_recyclerview, null, false);

        container.addView(view);

        return view;
    }

    @Override
    public boolean onSetListAdapter(E adapter)
    {
        if (super.onSetListAdapter(adapter)) {
            mFastScroller.setRecyclerView(getListView());
            return true;
        }

        return false;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        refreshList();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.actions_abs_editable_list, menu);

        MenuItem filterItem = menu.findItem(R.id.actions_abs_editable_filter);

        if (filterItem != null) {
            filterItem.setVisible(mFilteringSupported);

            if (mFilteringSupported) {
                View view = filterItem.getActionView();

                if (view instanceof SearchView) {
                    ((SearchView) view).setOnQueryTextListener(new SearchView.OnQueryTextListener()
                    {
                        @Override
                        public boolean onQueryTextSubmit(String query)
                        {
                            refreshList();
                            return true;
                        }

                        @Override
                        public boolean onQueryTextChange(String newText)
                        {
                            mSearchText = newText;
                            refreshList();
                            return true;
                        }
                    });
                }
            }
        }

        MenuItem gridSizeItem = menu.findItem(R.id.actions_abs_editable_grid_size);

        if (gridSizeItem != null) {
            Menu gridSizeMenu = gridSizeItem.getSubMenu();

            for (int i = 1; i < (isScreenLandscape() ? 7 : 5); i++)
                gridSizeMenu.add(R.id.actions_abs_editable_group_grid_size, 0, i,
                        getContext().getResources().getQuantityString(R.plurals.text_gridRow, i, i));

            gridSizeMenu.setGroupCheckable(R.id.actions_abs_editable_group_grid_size, true, true);
        }

        Map<String, Integer> sortingOptions = new ArrayMap<>();
        onSortingOptions(sortingOptions);

        if (sortingOptions.size() > 0) {
            mSortingOptions.clear();
            mSortingOptions.putAll(sortingOptions);

            applyDynamicMenuItems(menu.findItem(R.id.actions_abs_editable_sort_by),
                    R.id.actions_abs_editable_group_sorting, mSortingOptions);

            Map<String, Integer> orderingOptions = new ArrayMap<>();
            onOrderingOptions(orderingOptions);

            if (orderingOptions.size() > 0) {
                mOrderingOptions.clear();
                mOrderingOptions.putAll(orderingOptions);

                applyDynamicMenuItems(menu.findItem(R.id.actions_abs_editable_order_by),
                        R.id.actions_abs_editable_group_sort_order, mOrderingOptions);
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu)
    {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.actions_abs_editable_sort_by)
                .setEnabled(isSortingSupported());

        MenuItem multiSelect = menu.findItem(R.id.actions_abs_editable_multi_select);

        if (multiSelect != null
                && (getSelectionConnection() == null
                || !getSelectionConnection().getMode().getEngineToolbar().isFinishAllowed()))
            multiSelect.setVisible(false);

        if (!getAdapter().isGridSupported())
            menu.findItem(R.id.actions_abs_editable_grid_size)
                    .setVisible(false);

        MenuItem sortingItem = menu.findItem(R.id.actions_abs_editable_sort_by);

        if (sortingItem != null) {
            sortingItem.setVisible(mSortingSupported);

            if (sortingItem.isVisible()) {
                checkPreferredDynamicItem(sortingItem, getSortingCriteria(), mSortingOptions);

                MenuItem orderingItem = menu.findItem(R.id.actions_abs_editable_order_by);

                if (orderingItem != null)
                    checkPreferredDynamicItem(orderingItem, getOrderingCriteria(),
                            mOrderingOptions);
            }
        }

        MenuItem gridSizeItem = menu.findItem(R.id.actions_abs_editable_grid_size);

        if (gridSizeItem != null) {
            Menu gridRowMenu = gridSizeItem.getSubMenu();
            int currentRow = getViewingGridSize() - 1;

            if (currentRow < gridRowMenu.size())
                gridRowMenu.getItem(currentRow).setChecked(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        int groupId = item.getGroupId();

        if (id == R.id.actions_abs_editable_multi_select && getSelectionCallback() != null)
            getSelectionConnection().getMode().start(getSelectionCallback());
        else if (groupId == R.id.actions_abs_editable_group_sorting)
            changeSortingCriteria(item.getOrder());
        else if (groupId == R.id.actions_abs_editable_group_sort_order)
            changeOrderingCriteria(item.getOrder());
        else if (groupId == R.id.actions_abs_editable_group_grid_size)
            changeGridViewSize(item.getOrder());

        return super.onOptionsItemSelected(item);
    }

    public void onSortingOptions(Map<String, Integer> options)
    {
        options.put(getString(R.string.text_sortByName), EditableListAdapter.MODE_SORT_BY_NAME);
        options.put(getString(R.string.text_sortByDate), EditableListAdapter.MODE_SORT_BY_DATE);
        options.put(getString(R.string.text_sortBySize), EditableListAdapter.MODE_SORT_BY_SIZE);
    }

    public void onOrderingOptions(Map<String, Integer> options)
    {
        options.put(getString(R.string.text_sortOrderAscending),
                EditableListAdapter.MODE_SORT_ORDER_ASCENDING);
        options.put(getString(R.string.text_sortOrderDescending),
                EditableListAdapter.MODE_SORT_ORDER_DESCENDING);
    }

    @Override
    public void onPrepareDetach()
    {
        if (getPowerfulActionMode() != null && getSelectionCallback() != null)
            getPowerfulActionMode().finish(getSelectionCallback());
    }

    public int onGridSpanSize(int viewType, int currentSpanSize)
    {
        return 1;
    }

    @Override
    public RecyclerView.LayoutManager onLayoutManager()
    {
        final RecyclerView.LayoutManager defaultLayoutManager = super.onLayoutManager();
        final int preferredGridSize = getViewingGridSize();
        final int optimalGridSize = preferredGridSize > 1 ? preferredGridSize
                : !getAdapter().isGridSupported() && isScreenLarge()
                && !isHorizontalOrientation() ? 2 : 1;

        final GridLayoutManager layoutManager;

        if (defaultLayoutManager instanceof GridLayoutManager) {
            layoutManager = (GridLayoutManager) defaultLayoutManager;
            layoutManager.setSpanCount(optimalGridSize);
        } else
            layoutManager = new GridLayoutManager(getContext(), optimalGridSize);

        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup()
        {
            @Override
            public int getSpanSize(int position)
            {
                // should be reserved so it can occupy all the available space of a line
                int viewType = getAdapter().getItemViewType(position);

                return viewType == EditableListAdapter.VIEW_TYPE_DEFAULT
                        ? 1
                        : onGridSpanSize(viewType, optimalGridSize);
            }
        });

        return layoutManager;
    }

    protected void applyDynamicMenuItems(MenuItem mainItem, int groupId,
                                         Map<String, Integer> options)
    {
        if (mainItem != null) {
            mainItem.setVisible(true);

            Menu dynamicMenu = mainItem.getSubMenu();

            for (String currentKey : options.keySet()) {
                int modeId = options.get(currentKey);
                dynamicMenu.add(groupId, 0, modeId, currentKey);
            }

            dynamicMenu.setGroupCheckable(groupId, true, true);
        }
    }

    public boolean applyViewingChanges(int gridSize)
    {
        if (!getAdapter().isGridSupported())
            return false;

        getAdapter().notifyGridSizeUpdate(gridSize, isScreenLarge());

        getListView().setLayoutManager(onLayoutManager());
        getListView().setAdapter(getAdapter());

        refreshList();

        return true;
    }

    public void changeGridViewSize(int gridSize)
    {
        getViewPreferences().edit()
                .putInt(getUniqueSettingKey("GridSize" + (isScreenLandscape() ? "Landscape" : "")),
                        gridSize)
                .apply();

        applyViewingChanges(gridSize);
    }

    public void changeOrderingCriteria(int id)
    {
        getViewPreferences().edit()
                .putInt(getUniqueSettingKey("SortOrder"), id)
                .apply();

        getAdapter().setSortingCriteria(getSortingCriteria(), id);

        refreshList();
    }

    public void changeSortingCriteria(int id)
    {
        getViewPreferences().edit()
                .putInt(getUniqueSettingKey("SortBy"), id)
                .apply();

        getAdapter().setSortingCriteria(id, getOrderingCriteria());

        refreshList();
    }

    public void checkPreferredDynamicItem(MenuItem dynamicItem, int preferredItemId, Map<String,
            Integer> options)
    {
        if (dynamicItem != null) {
            Menu gridSizeMenu = dynamicItem.getSubMenu();

            for (String title : options.keySet()) {
                if (options.get(title) == preferredItemId) {
                    MenuItem menuItem;
                    int iterator = 0;

                    while ((menuItem = gridSizeMenu.getItem(iterator)) != null) {
                        if (title.equals(String.valueOf(menuItem.getTitle()))) {
                            menuItem.setChecked(true);
                            return;
                        }

                        iterator++;
                    }

                    // normally we should not be here
                    return;
                }
            }
        }
    }

    public EditableListAdapterImpl<T> getAdapterImpl()
    {
        return getAdapter();
    }

    public ContentObserver getDefaultContentObserver()
    {
        if (mObserver == null)
            mObserver = new ContentObserver(new Handler(Looper.myLooper()))
            {
                @Override
                public boolean deliverSelfNotifications()
                {
                    return true;
                }

                @Override
                public void onChange(boolean selfChange)
                {
                    refreshList();
                }
            };

        return mObserver;
    }

    @Override
    public FilteringDelegate<T> getFilteringDelegate()
    {
        return mFilteringDelegate == null
                ? mDefaultFilteringDelegate
                : mFilteringDelegate;
    }

    @Override
    public void setFilteringDelegate(FilteringDelegate<T> delegate)
    {
        mFilteringDelegate = delegate;
    }

    public FastScroller getFastScroller()
    {
        return mFastScroller;
    }

    public int getOrderingCriteria()
    {
        return getViewPreferences().getInt(getUniqueSettingKey("SortOrder"),
                mDefaultOrderingCriteria);
    }

    public String getUniqueSettingKey(String setting)
    {
        return getClass().getSimpleName() + "_" + setting;
    }

    public PowerfulActionMode.SelectorConnection<T> getSelectionConnection()
    {
        return mSelectionConnection == null
                ? mDefaultSelectionConnection
                : mSelectionConnection;
    }

    public SelectionCallback<T> getSelectionCallback()
    {
        return mSelectionCallback == null
                ? mDefaultSelectionCallback
                : mSelectionCallback;
    }

    public void setSelectionCallback(SelectionCallback<T> selectionCallback)
    {
        mSelectionCallback = selectionCallback;
    }

    public int getSortingCriteria()
    {
        return getViewPreferences().getInt(getUniqueSettingKey("SortBy"), mDefaultSortingCriteria);
    }

    public PowerfulActionMode getPowerfulActionMode()
    {
        return getActivity() != null && getActivity() instanceof PowerfulActionModeSupport
                ? ((PowerfulActionModeSupport) getActivity()).getPowerfulActionMode()
                : null;
    }

    public SharedPreferences getViewPreferences()
    {
        return AppUtils.getViewingPreferences(getContext());
    }

    public int getViewingGridSize()
    {
        if (getViewPreferences() == null)
            return 1;

        return isScreenLandscape()
                ? getViewPreferences().getInt(getUniqueSettingKey("GridSizeLandscape"),
                mDefaultViewingGridSizeLandscape)
                : getViewPreferences().getInt(getUniqueSettingKey("GridSize"),
                mDefaultViewingGridSize);
    }

    public int getActiveViewingGridSize()
    {
        return getListView().getLayoutManager() instanceof GridLayoutManager
                ? ((GridLayoutManager) getListView().getLayoutManager()).getSpanCount()
                : 1;
    }

    public boolean isRefreshLocked()
    {
        return false;
    }

    public boolean isRefreshRequested()
    {
        return mRefreshRequested;
    }

    public void setRefreshRequested(boolean requested)
    {
        mRefreshRequested = requested;
    }

    public boolean isSortingSupported()
    {
        return mSortingSupported;
    }

    public void setSortingSupported(boolean sortingSupported)
    {
        mSortingSupported = sortingSupported;
    }

    public boolean loadIfRequested()
    {
        boolean refreshed = isRefreshRequested();

        setRefreshRequested(false);

        if (refreshed)
            refreshList();

        return refreshed;
    }

    public boolean openUri(Uri uri)
    {
        return FileUtils.openUri(getContext(), uri);
    }

    public boolean performLayoutClick(V holder)
    {
        return setItemSelected(holder)
                || (mLayoutClickListener != null && mLayoutClickListener.onLayoutClick(
                this, holder, false))
                || onDefaultClickAction(holder);
    }

    public boolean performLayoutLongClick(V holder)
    {
        return (mLayoutClickListener != null && mLayoutClickListener.onLayoutClick(
                this, holder, true))
                || onDefaultLongClickAction(holder)
                || getSelectionConnection() != null && getSelectionConnection().setSelected(holder);
    }

    public boolean performLayoutClickOpen(V holder)
    {
        try {
            T object = getAdapter().getItem(holder);

            if (object instanceof Shareable)
                return openUri(((Shareable) object).uri);
        } catch (NotReadyException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void registerLayoutViewClicks(final V holder)
    {
        holder.getClickableView().setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                performLayoutClick(holder);
            }
        });

        holder.getClickableView().setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                return performLayoutLongClick(holder);
            }
        });
    }

    @Override
    public void refreshList()
    {
        if (isRefreshLocked()) {
            setRefreshRequested(true);

            if (mRefreshDelayedSnackbar == null) {
                mRefreshDelayedSnackbar = createSnackbar(R.string.mesg_listRefreshSnoozed);
                mRefreshDelayedSnackbar.setDuration(BaseTransientBottomBar.LENGTH_LONG);
            }

            mRefreshDelayedSnackbar.show();
        } else {
            super.refreshList();

            if (mRefreshDelayedSnackbar != null) {
                mRefreshDelayedSnackbar.dismiss();
                mRefreshDelayedSnackbar = null;
            }
        }
    }

    public void setDefaultPaddingDecorationSize(float defaultPadding)
    {
        mDefaultPaddingDecorationSize = defaultPadding;
    }

    public void setDefaultOrderingCriteria(int criteria)
    {
        mDefaultOrderingCriteria = criteria;
    }

    public void setDefaultSelectionCallback(SelectionCallback<T> selectionCallback)
    {
        mDefaultSelectionCallback = selectionCallback;
    }

    public void setDefaultSelectionConnection(PowerfulActionMode.SelectorConnection<T> selectionConnection)
    {
        mDefaultSelectionConnection = selectionConnection;
    }

    public void setDefaultSortingCriteria(int criteria)
    {
        mDefaultSortingCriteria = criteria;
    }

    public void setDefaultViewingGridSize(int gridSize, int gridSizeLandscape)
    {
        mDefaultViewingGridSize = gridSize;
        mDefaultViewingGridSizeLandscape = gridSizeLandscape;
    }

    public void setDividerVisible(boolean visible)
    {
        if (getView() != null) {
            View divider = getView().findViewById(mDividerResId);
            divider.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void setDividerView(int resId)
    {
        mDividerResId = resId;
    }

    public boolean setItemSelected(V holder)
    {
        return getSelectionCallback() != null && getSelectionCallback().setItemSelected(holder.getAdapterPosition());
    }

    @Override
    public void setLayoutClickListener(LayoutClickListener<V> clickListener)
    {
        mLayoutClickListener = clickListener;
    }

    public void setFilteringSupported(boolean supported)
    {
        mFilteringSupported = supported;
    }

    @Override
    public void setSelectorConnection(PowerfulActionMode.SelectorConnection<T> selectionConnection)
    {
        mSelectionConnection = selectionConnection;
    }

    public void setUseDefaultPaddingDecoration(boolean use)
    {
        mUseDefaultPaddingDecoration = use;
    }

    public void setUseDefaultPaddingDecorationSpaceForEdges(boolean use)
    {
        mUseDefaultPaddingDecorationSpaceForEdges = use;
    }

    public interface LayoutClickListener<V extends EditableListAdapter.EditableViewHolder>
    {
        boolean onLayoutClick(EditableListFragment listFragment, V holder, boolean longClick);
    }

    public interface FilteringDelegate<T extends Editable>
    {
        boolean changeFilteringKeyword(@Nullable String keyword);

        @Nullable
        String[] getFilteringKeyword(EditableListFragmentImpl<T> listFragment);
    }

    public static class SelectionCallback<T extends Editable> implements PowerfulActionMode.Callback<T>
    {
        private EditableListFragmentImpl<T> mFragment;

        public SelectionCallback(EditableListFragmentImpl<T> fragment)
        {
            updateProvider(fragment);
        }

        public EditableListAdapterImpl<T> getAdapter()
        {
            return mFragment.getAdapterImpl();
        }

        public EditableListFragmentImpl<T> getFragment()
        {
            return mFragment;
        }

        public boolean isSelectionActivated()
        {
            return mFragment.getSelectionConnection() != null
                    && mFragment.getSelectionConnection().selectionActive();
        }

        @Override
        public List<T> getSelectableList()
        {
            return getAdapter().getList();
        }

        public boolean setItemSelected(int position)
        {
            return isSelectionActivated() && mFragment.getSelectionConnection().setSelected(position);
        }

        public void setSelection(boolean selection, List<T> selectableList)
        {
            for (T selectable : selectableList)
                mFragment.getSelectionConnection().setSelected(selectable, selection);
        }

        public boolean setSelection()
        {
            boolean allSelected = mFragment.getSelectionConnection().getSelectedItemList().size() != getSelectableList().size();

            setSelection(allSelected);

            return allSelected;
        }

        public void setSelection(boolean selection)
        {
            setSelection(selection, getSelectableList());

            // One-by-one calling caused an ANR
            getAdapter().syncSelectionList();
            getAdapter().notifyItemRangeChanged(0, getSelectableList().size());
        }

        public void updateProvider(EditableListFragmentImpl<T> fragment)
        {
            mFragment = fragment;
        }

        private void updateSelectionTitle(PowerfulActionMode actionMode)
        {
            int selectedSize = mFragment.getSelectionConnection()
                    .getSelectedItemList()
                    .size();

            actionMode.setTitle(String.valueOf(selectedSize));
        }

        @Override
        public boolean onPrepareActionMenu(Context context, PowerfulActionMode actionMode)
        {
            updateSelectionTitle(actionMode);
            return true;
        }

        @Override
        public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
        {
            actionMode.getMenuInflater().inflate(R.menu.action_mode_abs_editable, menu);
            return false;
        }

        @Override
        public void onItemChecked(Context context, PowerfulActionMode actionMode, T selectable, int position)
        {
            updateSelectionTitle(actionMode);

            if (position != -1) {
                getAdapter().syncSelectionList();
                getAdapter().notifyItemChanged(position);
            }
        }

        @Override
        public boolean onActionMenuItemSelected(final Context context, PowerfulActionMode actionMode, MenuItem item)
        {
            int id = item.getItemId();

            if (id == R.id.action_mode_abs_editable_select_all)
                setSelection(true);
            else if (id == R.id.action_mode_abs_editable_select_none)
                setSelection(false);
            else if (id == R.id.action_mode_abs_editable_preview_selections)
                new SelectionEditorDialog<>(mFragment.getActivity(), mFragment.getSelectionConnection().getSelectedItemList())
                        .setOnDismissListener(new DialogInterface.OnDismissListener()
                        {
                            @Override
                            public void onDismiss(DialogInterface dialog)
                            {
                                List<T> selectedItems = new ArrayList<>(mFragment.getSelectionConnection().getSelectedItemList());

                                for (T selectable : selectedItems)
                                    if (!selectable.isSelectableSelected())
                                        mFragment.getSelectionConnection().setSelected(selectable, false);

                                // Position cannot be assumed that is why we need to request a refresh
                                getAdapter().notifyAllSelectionChanges();
                            }

                        })
                        .show();

            return false;
        }

        @Override
        public void onFinish(Context context, PowerfulActionMode actionMode)
        {
            setSelection(false);

            mFragment.getSelectionConnection().getSelectedItemList().clear();
            mFragment.loadIfRequested();
        }
    }
}
