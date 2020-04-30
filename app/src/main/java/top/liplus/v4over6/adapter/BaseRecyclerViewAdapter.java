package top.liplus.v4over6.adapter;

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

    public void insertItemImmediately(int position, T item) {
        data.add(position, item);
        notifyItemInserted(position);
    }

    public void insertItemImmediately(T item) {
        data.add(item);
        notifyItemInserted(data.size() - 1);
    }

    public T removeItemImmediately(int position) {
        T bak = data.remove(position);
        notifyItemRemoved(position);
        return bak;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
