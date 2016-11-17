package ee.ria.EstEIDUtility.adapter;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import ee.ria.EstEIDUtility.R;

public class AlertItemAdapter extends ArrayAdapter<String> {

    private List<Integer> images;
    private List<String> items;
    private final Activity context;

    public AlertItemAdapter(Activity context, String[] items, Integer[] images) {
        super(context, R.layout.send_action_row, items);
        this.images = Arrays.asList(images);
        this.items = Arrays.asList(items);
        this.context = context;
    }

    private class ViewHolder {
        TextView name;
        ImageView image;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;

        if (convertView == null) {
            LayoutInflater inflator = context.getLayoutInflater();
            view = inflator.inflate(R.layout.send_action_row, null);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.name = (TextView) view.findViewById(R.id.sendText);
            viewHolder.image = (ImageView) view.findViewById(R.id.sendImg);
            view.setTag(viewHolder);
        } else {
            view = convertView;
        }

        ViewHolder holder = (ViewHolder) view.getTag();
        holder.name.setText(items.get(position));
        holder.image.setImageResource(images.get(position));

        return view;
    }
}
