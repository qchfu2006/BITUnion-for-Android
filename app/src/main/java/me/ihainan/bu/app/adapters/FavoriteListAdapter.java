package me.ihainan.bu.app.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import me.ihainan.bu.app.R;
import me.ihainan.bu.app.models.Favorite;
import me.ihainan.bu.app.models.Member;
import me.ihainan.bu.app.ui.PostListActivity;
import me.ihainan.bu.app.ui.ProfileActivity;
import me.ihainan.bu.app.ui.viewholders.LoadingViewHolder;
import me.ihainan.bu.app.ui.viewholders.TimelineViewHolder;
import me.ihainan.bu.app.utils.CommonUtils;
import me.ihainan.bu.app.utils.BUApplication;
import me.ihainan.bu.app.utils.ui.HtmlUtil;

/**
 * 收藏列表适配器
 */
public class FavoriteListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final static String TAG = PostListAdapter.class.getSimpleName();
    private final List<Favorite> mList;
    private final LayoutInflater mLayoutInflater;
    private final Context mContext;


    public FavoriteListAdapter(Context context, List<Favorite> list) {
        mList = list;
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
    }

    private final int VIEW_TYPE_ITEM = 0;

    @Override
    public int getItemViewType(int position) {
        int VIEW_TYPE_LOADING = 1;
        return mList.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof TimelineViewHolder.TimelinePostViewHolder) {
            // Do nothing here
            final Favorite favorite = mList.get(position);
            final TimelineViewHolder.TimelinePostViewHolder viewHolder = (TimelineViewHolder.TimelinePostViewHolder) holder;

            // 收藏
            String username = favorite.author;
            viewHolder.username.setText(username);
            viewHolder.action.setText("发表的主题");
            viewHolder.title.setText(Html.fromHtml(HtmlUtil.formatHtml(favorite.subject)));
            viewHolder.title.setTextAppearance(mContext, R.style.boldText);
            viewHolder.content.setVisibility(View.GONE);
            viewHolder.date.setText(CommonUtils.getRelativeTimeSpanString(CommonUtils.parseDateString(favorite.dt_created)));

            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, PostListActivity.class);
                    intent.putExtra(PostListActivity.THREAD_ID_TAG, favorite.tid);
                    intent.putExtra(PostListActivity.THREAD_AUTHOR_NAME_TAG, favorite.author);
                    intent.putExtra(PostListActivity.THREAD_NAME_TAG, favorite.subject);
                    intent.putExtra(PostListActivity.THREAD_JUMP_FLOOR, 0);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (((Activity) mContext).isInMultiWindowMode()) {
                            intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT |
                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        }
                    }

                    mContext.startActivity(intent);
                }
            };

            viewHolder.rootLayout.setOnClickListener(onClickListener);

            // 从缓存中获取用户头像
            // viewHolder.avatar.setVisibility(View.GONE);
            username = username == null ? (BUApplication.userSession == null ? "UNKNOWN" : BUApplication.userSession.username) : username;
            CommonUtils.getAndCacheUserInfo(mContext,
                    username,
                    new CommonUtils.UserInfoAndFillAvatarCallback() {
                        @Override
                        public void doSomethingIfHasCached(final Member member) {
                            String avatarURL = CommonUtils.getRealImageURL(member.avatar);
                            CommonUtils.setAvatarImageView(mContext, viewHolder.avatar,
                                    avatarURL, R.drawable.default_avatar);
                            viewHolder.username.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(mContext, ProfileActivity.class);
                                    intent.putExtra(ProfileActivity.USER_ID_TAG, member.uid);
                                    intent.putExtra(ProfileActivity.USER_NAME_TAG, member.username);
                                    mContext.startActivity(intent);
                                }
                            });
                        }
                    });

            CommonUtils.setUserAvatarClickListener(mContext,
                    viewHolder.avatar, -1, username);
        } else {
            LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
            loadingViewHolder.progressBar.setIndeterminate(true);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_ITEM) {
            view = mLayoutInflater.inflate(R.layout.item_event_post, parent, false);
            return new TimelineViewHolder.TimelinePostViewHolder(view);
        } else {
            view = mLayoutInflater.inflate(R.layout.listview_progress_bar, parent, false);
            return new LoadingViewHolder(view);
        }
    }
}
