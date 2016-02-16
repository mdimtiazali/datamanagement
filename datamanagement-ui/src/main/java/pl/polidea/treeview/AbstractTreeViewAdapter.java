package pl.polidea.treeview;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import com.cnh.pf.android.data.management.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter used to feed the table view.
 *
 * @param <T>
 *            class for ID of the tree
 */
public abstract class AbstractTreeViewAdapter<T> extends BaseAdapter implements
                                                                     ListAdapter {
    protected static final Logger log = LoggerFactory.getLogger(AbstractTreeViewAdapter.class);

    private final TreeStateManager<T> treeStateManager;
    private final int numberOfLevels;
    private final LayoutInflater layoutInflater;

    private int indentWidth = 0;
    private int indicatorGravity = 0;
    private Drawable collapsedDrawable;
    private Drawable expandedDrawable;
    private Drawable indicatorBackgroundDrawable;
    private Drawable rowBackgroundDrawable;

    private final OnClickListener indicatorClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            @SuppressWarnings("unchecked")
            final T id = (T) v.getTag();
            expandCollapse(id);
        }
    };

    private boolean collapsible;
    private final Activity activity;

    public Activity getActivity() {
        return activity;
    }

    protected TreeStateManager<T> getManager() {
        return treeStateManager;
    }

    protected void expandCollapse(final T id) {
        final TreeNodeInfo<T> info = treeStateManager.getNodeInfo(id);
        if (!info.isWithChildren()) {
            // ignore - no default action
            return;
        }
        if (info.isExpanded()) {
            treeStateManager.collapseChildren(id);
        } else {
            treeStateManager.expandDirectChildren(id);
        }
    }

    public AbstractTreeViewAdapter(final Activity activity,
          final TreeStateManager<T> treeStateManager, final int numberOfLevels) {
        this.activity = activity;
        this.treeStateManager = treeStateManager;
        this.layoutInflater = (LayoutInflater) activity
              .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.numberOfLevels = numberOfLevels;
        this.collapsedDrawable = null;
        this.expandedDrawable = null;
        this.rowBackgroundDrawable = null;
        this.indicatorBackgroundDrawable = null;
    }

    @Override
    public void registerDataSetObserver(final DataSetObserver observer) {
        treeStateManager.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(final DataSetObserver observer) {
        treeStateManager.unregisterDataSetObserver(observer);
    }

    @Override
    public int getCount() {
        return treeStateManager.getVisibleCount();
    }

    @Override
    public Object getItem(final int position) {
        return getItemId(position);
    }

    public T getTreeId(final int position) {
        return treeStateManager.getVisibleList().get(position);
    }

    public TreeNodeInfo<T> getTreeNodeInfo(final int position) {
        return treeStateManager.getNodeInfo(getTreeId(position));
    }

    @Override
    public boolean hasStableIds() { // NOPMD
        return true;
    }

    @Override
    public int getItemViewType(final int position) {
        return getTreeNodeInfo(position).getLevel();
    }

    @Override
    public int getViewTypeCount() {
        return numberOfLevels;
    }

    @Override
    public boolean isEmpty() {
        return getCount() == 0;
    }

    @Override
    public boolean areAllItemsEnabled() { // NOPMD
        return true;
    }

    @Override
    public boolean isEnabled(final int position) { // NOPMD
        return true;
    }

    protected int getTreeListItemWrapperId() {
        return R.layout.tree_list_item_wrapper_right;
    }

    @Override
    public final View getView(final int position, final View convertView,
          final ViewGroup parent) {
        //        Log.d(TAG, "Creating a view based on " + convertView
        //                + " with position " + position);
        final TreeNodeInfo<T> nodeInfo = getTreeNodeInfo(position);
        if (convertView == null) {
            //            Log.d(TAG, "Creating the view a new");
            final LinearLayout layout = (LinearLayout) layoutInflater.inflate(
                  getTreeListItemWrapperId(), null);
            return populateTreeItem(layout, getNewChildView(nodeInfo),
                  nodeInfo, true);
        } else {
            //            Log.d(TAG, "Reusing the view");
            final LinearLayout linear = (LinearLayout) convertView;
            final FrameLayout frameLayout = (FrameLayout) linear
                  .findViewById(R.id.treeview_list_item_frame);
            final View childView = frameLayout.getChildAt(0);
            updateView(childView, nodeInfo);
            return populateTreeItem(linear, childView, nodeInfo, false);
        }
    }

    /**
     * Called when new view is to be created.
     *
     * @param treeNodeInfo
     *            node info
     * @return view that should be displayed as tree content
     */
    public abstract View getNewChildView(TreeNodeInfo<T> treeNodeInfo);

    /**
     * Called when new view is going to be reused. You should update the view
     * and fill it in with the data required to display the new information. You
     * can also create a new view, which will mean that the old view will not be
     * reused.
     *
     * @param view
     *            view that should be updated with the new values
     * @param treeNodeInfo
     *            node info used to populate the view
     * @return view to used as row indented content
     */
    public abstract View updateView(View view, TreeNodeInfo<T> treeNodeInfo);

    /**
     * Retrieves background drawable for the node.
     *
     * @param treeNodeInfo
     *            node info
     * @return drawable returned as background for the whole row. Might be null,
     *         then default background is used
     */
    public Drawable getBackgroundDrawable(final TreeNodeInfo<T> treeNodeInfo) { // NOPMD
        return null;
    }

    private Drawable getDrawableOrDefaultBackground(final Drawable r) {
        if (r == null) {
            return activity.getResources()
                  .getDrawable(R.drawable.list_selector_background).mutate();
        } else {
            return r;
        }
    }

    public final LinearLayout populateTreeItem(final LinearLayout layout,
          final View childView, final TreeNodeInfo<T> nodeInfo,
          final boolean newChildView) {
        final Drawable individualRowDrawable = getBackgroundDrawable(nodeInfo);
        final Drawable bgDrawable = individualRowDrawable == null ?
              getDrawableOrDefaultBackground(rowBackgroundDrawable)
              : individualRowDrawable;

        if(newChildView) {
            layout.setTag(R.id.treeview_list_item_indent, layout.findViewById(R.id.treeview_list_item_indent));
            final View view = layout.findViewById(R.id.treeview_list_item_frame_layout);
            layout.setTag(R.id.treeview_list_item_frame_layout, view);
            layout.setTag(R.id.treeview_list_item_toggle, view.findViewById(R.id.treeview_list_item_toggle));
            layout.setTag(R.id.treeview_list_item_frame, view.findViewById(R.id.treeview_list_item_frame));
        }
        final View indentView = (View) layout.getTag(R.id.treeview_list_item_indent);
        final ImageView toggle = (ImageView) layout.getTag(R.id.treeview_list_item_toggle);
        final LinearLayout frameLayout = (LinearLayout) layout.getTag(R.id.treeview_list_item_frame_layout);
        final FrameLayout frame = (FrameLayout) layout.getTag(R.id.treeview_list_item_frame);

        indentView.setLayoutParams(new LinearLayout.LayoutParams(calculateIndentation(nodeInfo), LayoutParams.MATCH_PARENT));

        toggle.setImageDrawable(getDrawable(nodeInfo));
        toggle.setVisibility((nodeInfo.isWithChildren() && collapsible) ? View.VISIBLE : View.GONE);
        toggle.setOnClickListener(nodeInfo.isWithChildren() && collapsible ? indicatorClickListener : null);
        toggle.setTag(nodeInfo.getId());

        layout.setTag(nodeInfo.getId());

        frameLayout.setBackground(bgDrawable);
        if (newChildView) {
            frame.addView(childView, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }
        frameLayout.setTag(nodeInfo.getId());
        return layout;
    }

    protected int calculateIndentation(final TreeNodeInfo<T> nodeInfo) {
        return getIndentWidth() * nodeInfo.getLevel();
    }

    protected Drawable getDrawable(final TreeNodeInfo<T> nodeInfo) {
        if (nodeInfo.isExpanded()) {
            return expandedDrawable;
        } else {
            return collapsedDrawable;
        }
    }

    public void setIndicatorGravity(final int indicatorGravity) {
        this.indicatorGravity = indicatorGravity;
    }

    public void setCollapsedDrawable(final Drawable collapsedDrawable) {
        this.collapsedDrawable = collapsedDrawable;
    }

    public void setExpandedDrawable(final Drawable expandedDrawable) {
        this.expandedDrawable = expandedDrawable;
    }

    public void setIndentWidth(final int indentWidth) {
        this.indentWidth = indentWidth;
    }

    public void setRowBackgroundDrawable(final Drawable rowBackgroundDrawable) {
        this.rowBackgroundDrawable = rowBackgroundDrawable;
    }

    public void setIndicatorBackgroundDrawable(
          final Drawable indicatorBackgroundDrawable) {
        this.indicatorBackgroundDrawable = indicatorBackgroundDrawable;
    }

    public void setCollapsible(final boolean collapsible) {
        this.collapsible = collapsible;
    }

    public void refresh() {
        treeStateManager.refresh();
    }

    private int getIndentWidth() {
        return indentWidth;
    }

    @SuppressWarnings("unchecked")
    public void handleItemClick(final AdapterView< ? > parent, final View view, final int position, final Object id) {

    }

}
