package io.github.wax911.retgraph.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.wax911.retgraph.R;
import io.github.wax911.retgraph.model.Repository;
import io.github.wax911.retgraph.model.User;
import io.github.wax911.retgraph.model.parent.Entry;

public class AdapterExample extends RecyclerView.Adapter<AdapterExample.EntryAdapter> {

    private List<Entry> model = new ArrayList<>();

    public List<Entry> getModel() {
        return model;
    }

    public void addItems(@NonNull List<Entry> model) {
        this.model.addAll(model);
        notifyDataSetChanged();
    }

    @Override @NonNull
    public EntryAdapter onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_example, parent, false);
        return new EntryAdapter(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryAdapter holder, int position) {
        holder.bindModel(model.get(position));
    }

    @Override
    public int getItemCount() {
        return model.size();
    }

    public boolean isDataSetEmpty() {
        return model.isEmpty();
    }

    /** View Holder for our list items */
    class EntryAdapter extends RecyclerView.ViewHolder {

        private TextView repoName, postedBy, repoStars;
        private ImageView ownerAvatar;
        private Context context;

        EntryAdapter(View itemView) {
            super(itemView);
            context = itemView.getContext();
            repoName = itemView.findViewById(R.id.repoName);
            postedBy = itemView.findViewById(R.id.postedBy);
            repoStars = itemView.findViewById(R.id.repoStars);
            ownerAvatar = itemView.findViewById(R.id.ownerAvatar);
        }

        void bindModel(Entry data) {
            Repository repository = data.getRepository();
            if(repository != null) {
                User user = repository.getOwner();
                Glide.with(context)
                        .load(user.getAvatar_url())
                        .into(ownerAvatar);
                repoName.setText(repository.getFull_name());
                repoStars.setText(String.format(Locale.getDefault()," %d", repository.getStargazers_count()));
            } else {
                repoName.setText("Unknown");
                repoStars.setText("--");
            }
            postedBy.setText(data.getPostedBy().getLogin());
        }
    }
}
