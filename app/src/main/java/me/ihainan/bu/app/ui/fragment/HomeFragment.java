package me.ihainan.bu.app.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.android.volley.Response;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import me.ihainan.bu.app.R;
import me.ihainan.bu.app.adapters.LatestThreadListAdapter;
import me.ihainan.bu.app.models.LatestThread;
import me.ihainan.bu.app.ui.assist.SimpleDividerItemDecoration;
import me.ihainan.bu.app.utils.BUApplication;
import me.ihainan.bu.app.utils.CommonUtils;
import me.ihainan.bu.app.utils.network.BUApi;
import me.ihainan.bu.app.utils.ui.CustomOnClickListener;

/**
 * Home Page Fragment
 */
public class HomeFragment extends BasicRecyclerViewFragment<LatestThread> {
    private final static String HOME_FRAGMENT_HISTORY_LIST = "HOME_FRAGMENT_HISTORY_LIST";
    private List<LatestThread> mHistoryList;

    @Override
    protected String getNoNewDataMessage() {
        return getString(R.string.error_unknown_msg);
    }

    @Override
    protected String getFragmentTag() {
        return HomeFragment.class.getSimpleName();
    }

    @Override
    protected List<LatestThread> processList(List<LatestThread> list) {
        return list;
    }

    @Override
    protected List<LatestThread> parseResponse(JSONObject response) throws Exception {
        return BUApi.MAPPER.readValue(response.getJSONArray("newlist").toString(),
                new TypeReference<List<LatestThread>>() {
                });
    }

    @Override
    protected boolean checkStatus(JSONObject response) {
        return BUApi.checkStatus(response);
    }

    @Override
    protected void getExtra() {

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().findViewById(R.id.toolbar).setOnClickListener(CustomOnClickListener.doubleClickToListTop(mContext, mRecyclerView));
    }

    @Override
    protected RecyclerView.Adapter<RecyclerView.ViewHolder> getAdapter() {
        return new LatestThreadListAdapter(mContext, mList);
    }

    @Override
    protected int getLoadingCount() {
        return BUApplication.LOADING_HOME_PAGE_COUNT;
    }

    @Override
    protected void setupRecyclerView() {
        mLayoutManager = new LinearLayoutManager(mContext);

        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(mContext));
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Adapter
        mAdapter = getAdapter();
        mRecyclerView.setAdapter(mAdapter);

        // 加载历史数据
        String jsonStr = BUApplication.getCache(mContext).getAsString(HOME_FRAGMENT_HISTORY_LIST);
        try {
            if (jsonStr != null && !"".equals(jsonStr)) {
                mHistoryList = BUApi.MAPPER.readValue(jsonStr,
                        new TypeReference<List<LatestThread>>() {
                        });
                if (mHistoryList != null) {
                    mList.addAll(mHistoryList);
                    mAdapter.notifyDataSetChanged();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "解析历史数据失败 " + jsonStr, e);
        }
    }

    @Override
    protected void refreshData() {
        BUApi.getHomePage(mContext, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if (!isAdded() || ((Activity) mContext).isFinishing()) return;
                try {
                    mSwipeRefreshLayout.setRefreshing(false);

                    if (BUApi.checkStatus(response)) {
                        List<LatestThread> newItems = parseResponse(response);

                        // 重新加载
                        if (from == 0) {
                            mList.clear();
                            mAdapter.notifyDataSetChanged();
                        }

                        if (newItems == null || newItems.size() == 0) {
                            String message = getString(R.string.error_negative_credit);
                            String debugMessage = message + " - " + response;
                            Log.w(TAG, debugMessage);
                            CommonUtils.debugToast(mContext, debugMessage);

                            // Error
                            showErrorLayout(message);
                        } else {
                            mList.clear();
                            mList.addAll(newItems);
                            if (!checkIsSameList())
                                mAdapter.notifyDataSetChanged();
                            Gson gson = new GsonBuilder().create();
                            JsonArray jsArray = gson.toJsonTree(mList).getAsJsonArray();
                            BUApplication.getCache(mContext).put(HOME_FRAGMENT_HISTORY_LIST, jsArray.toString());
                        }
                    } else {
                        String message = getString(R.string.error_unknown_msg) + ": " + response.getString("msg");
                        String debugMessage = message + " - " + response;
                        Log.w(TAG, debugMessage);
                        CommonUtils.debugToast(mContext, debugMessage);
                        showErrorLayout(message);
                    }
                } catch (Exception e) {
                    mSwipeRefreshLayout.setRefreshing(false);
                    Log.e(TAG, getString(R.string.error_parse_json) + "\n" + response, e);
                    showErrorLayout(getString(R.string.error_parse_json));
                }
            }
        }, errorListener);
    }

    private boolean checkIsSameList() {
        if (mList == null && mHistoryList == null) return true;
        if (mList == null && mHistoryList != null
                || mList != null && mHistoryList == null
                || mList.size() != mHistoryList.size())
            return false;
        for (int i = 0; i < mList.size(); ++i) {
            if (!mList.get(i).toString().equals(mHistoryList.get(i).toString())) return false;
        }

        return true;
    }
}
