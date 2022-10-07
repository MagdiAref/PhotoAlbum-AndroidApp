package cs213.PhotosApp19.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import cs213.PhotosApp19.R;
import cs213.PhotosApp19.model.Photo;

/**
 * PhotoAdapter holds the photoview
 * @author Magdi Aref
 * @author Muskan Burman
 */
public class PhotoAdapter extends ArrayAdapter<Photo> {

    private Context context;

    /**
     * initialize
     * @param context
     * @param resourceId
     * @param items
     */
    public PhotoAdapter(Context context, int resourceId, List<Photo> items) {
        super(context, resourceId, items);
        this.context = context;
    }

    /**
     * getView returns the requested view.
     * @param position
     * @param convertView
     * @param parent
     * @return view
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        Photo photo = getItem(position);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.photo_view, null);
            holder = new ViewHolder();
            holder.caption = (TextView) convertView.findViewById(R.id.caption);
            holder.photo = (ImageView) convertView.findViewById(R.id.photo);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.caption.setText(photo.getCaption());
        holder.photo.setImageBitmap(photo.getBitmap());

        return convertView;
    }

    /**
     * ViewHolder holds the photo + captions and is private.
     */
    private class ViewHolder {
        ImageView photo;
        TextView caption;
    }
}
