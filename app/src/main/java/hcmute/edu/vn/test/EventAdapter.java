package hcmute.edu.vn.test;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class EventAdapter extends BaseAdapter {
    private Context context;
    private List<Event> eventList;
    private LayoutInflater inflater;

    public EventAdapter(Context context, List<Event> eventList) {
        this.context = context;
        this.eventList = eventList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return eventList.size();
    }

    @Override
    public Object getItem(int position) {
        return eventList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return eventList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_event, parent, false);
            holder = new ViewHolder();
            holder.txtEventTitle = convertView.findViewById(R.id.txt_Event_Title);
            holder.txtEventDate = convertView.findViewById(R.id.txtEventDate);
            holder.txtEventTime = convertView.findViewById(R.id.txtEventTime);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Event event = eventList.get(position);
        holder.txtEventTitle.setText(event.getTitle());
        holder.txtEventDate.setText("Ngày: " + event.getDate());
        holder.txtEventTime.setText("Giờ: " + (event.getTime() == null ? "Chưa chọn" : event.getTime()));
        Log.d("EventAdapter", "Event: " + event.getTitle() + ", Date: " + event.getDate() + ", Time: " + event.getTime());
        return convertView;


    }


    private static class ViewHolder {
        TextView txtEventTitle, txtEventDate, txtEventTime;
    }
}
