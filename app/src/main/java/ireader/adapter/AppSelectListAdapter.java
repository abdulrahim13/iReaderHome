package ireader.adapter;

import floating.lib.Dragger;
import ireader.home.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SectionIndexer;
import android.widget.TextView;
import com.android.settings.net.UidDetail;
import com.android.settings.net.UidDetailProvider;
import com.android.settings.net.UidDetailTask;
import com.baoyz.swipemenulistview.BaseSwipListAdapter;

import de.greenrobot.event.EventBus;

public class AppSelectListAdapter extends BaseSwipListAdapter implements SectionIndexer, Filterable {
    protected List<UidDetail> mAllApps;
    public List<UidDetail> mResultApps;
    protected boolean mIsSearching = true;
    private Context mContext;
    private LayoutInflater mInflater;
    private final UidDetailProvider mProvider;
    protected List<String> mSections;
    protected List<Integer> mPositions;

    public AppSelectListAdapter(Context context,
            UidDetailProvider provider,
            List<UidDetail> apps,
            List<String> sections,
            List<Integer> positions) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mAllApps = apps;
        mProvider = provider;
        mSections = sections;
        mPositions = positions;
        mResultApps = new ArrayList<UidDetail>();
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint,
                    FilterResults results) {
                mResultApps = (ArrayList<UidDetail>) results.values;
                if (results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }

            protected FilterResults performFiltering(CharSequence s) {
                String str = s.toString().toUpperCase();
                StringBuilder builder = new StringBuilder("");
                for (int i = 0; i < str.length(); i++) {
                    builder.append(str.charAt(i));
                    builder.append(".+");
                }
                String format = builder.toString();
                FilterResults results = new FilterResults();
                ArrayList<UidDetail> appList = new ArrayList<UidDetail>();
                if (mAllApps != null && mAllApps.size() > 0) {
                    for (UidDetail info : mAllApps) {
                        // match label or pinyin
                        String label = info.label;
                        String pinyin = info.pinyin;
                        if (label.matches(format) || label.indexOf(str) > -1 || pinyin.matches(format) || pinyin.indexOf(str) > -1) {
                            appList.add(info);
                        }
                    }
                }
                results.values = appList;
                results.count = appList.size();
                return results;
            }
        };
        return filter;
    }

    @Override
    public int getCount() {
        return mResultApps == null ? 0 : mResultApps.size();
    }

    @Override
    public Object getItem(int position) {
        return mResultApps == null ? 0 : mResultApps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.app_item, null, false);
            convertView.findViewById(R.id.app_item).setOnClickListener(launchClickListener);
            convertView.findViewById(R.id.version_name).setOnClickListener(detailClickListener);
        }
        UidDetail detail;
        if (mIsSearching) {
            if (mResultApps == null || mResultApps.isEmpty() || position >= mResultApps.size()) {
                return convertView;
            }
            detail = mResultApps.get(position);
        } else {
            detail = mAllApps.get(position);
        }
        final TextView title = (TextView) convertView.findViewById(R.id.app_name);
        final TextView packageName = (TextView) convertView.findViewById(R.id.package_name);
        title.setText(detail.label);
        // show apk name maybe more useful
        packageName.setText(detail.sourceDir);

        TextView group = (TextView) convertView.findViewById(R.id.group_title);
        if (group == null) {
            View parentView = (View) convertView.getParent();
            group = (TextView) parentView.findViewById(R.id.group_title);
            if (group == null) {
                parentView = (View) parentView.getParent();
                group = (TextView) parentView.findViewById(R.id.group_title);
            }
        }
        if (!mIsSearching) {
            int section = getSectionForPosition(position);
            if (getPositionForSection(section) == position) {
                group.setVisibility(View.VISIBLE);
                group.setText(mSections.get(section));
            } else {
                group.setVisibility(View.GONE);
            }
        } else {
            group.setVisibility(View.GONE);
        }
        UidDetailTask.bindView(mProvider, detail, convertView);

        return convertView;
    }

    private OnClickListener launchClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            UidDetail detail = (UidDetail)view.findViewById(R.id.version_name).getTag();
            if (detail == null || ((Activity) mContext).getComponentName().getPackageName().equals(detail.packageName)) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName(detail.packageName, detail.className));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            EventBus.getDefault().post(intent);
            try {
                mContext.startActivity(intent);
                mContext.startService(new Intent(mContext, Dragger.class));
            } catch(ActivityNotFoundException e) {}
        }
    };

    private OnClickListener detailClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            UidDetail detail = (UidDetail)view.getTag();
            if (detail == null) {
                return;
            }
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", detail.packageName, null));
            try {
                mContext.startActivity(intent);
            } catch(ActivityNotFoundException e) {}
        }
    };

    @Override
    public int getPositionForSection(int section) {
        if (section < 0 || section >= mPositions.size()) {
            return -1;
        }
        return mPositions.get(section);
    }

    @Override
    public int getSectionForPosition(int position) {
        if (position < 0 || position >= getCount()) {
            return -1;
        }
        int index = Arrays.binarySearch(mPositions.toArray(), position);
        return index >= 0 ? index : -index - 2;
    }

    @Override
    public Object[] getSections() {
        return mSections.toArray();
    }

}
