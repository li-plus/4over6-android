package top.liplus.v4over6.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

abstract public class BaseRecyclerViewAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    protected List<T> data;

    public BaseRecyclerViewAdapter(@NonNull List<T> data) {
        this.data = data;
    }

    public List<T> getData() {
        return data;
    }

    public void insertItemImmediate(int position, T item) {
        data.add(position, item);
        notifyItemInserted(position);
    }

    public void insertItemImmediate(T item) {
        data.add(item);
        notifyItemInserted(data.size() - 1);
    }

    public T removeItemImmediate(int position) {
        T bak = data.remove(position);
        notifyItemRemoved(position);
        return bak;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class BaseViewHolder extends RecyclerView.ViewHolder {
        public BaseViewHolder(@NonNull ViewGroup parent, int layoutResId) {
            super(LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false));
        }

        public View findViewById(int resId) {
            return itemView.findViewById(resId);
        }

        public View getView() {
            return itemView;
        }
    }
}
