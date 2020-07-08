package com.ruiners.banchat.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ruiners.banchat.Config;
import com.ruiners.banchat.R;
import com.ruiners.banchat.model.Client;
import com.ruiners.banchat.model.Room;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

public class ChannelsActivity extends AppCompatActivity {
    private static final String TAG = ChannelsActivity.class.getSimpleName();

    public static Client client = new Client(0L);
    public static Gson gson = new Gson();

    RoomsAdapter arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channels);

        ListView listView = findViewById(R.id.rooms_view);

        client.setSocket(createSocket());
        client.getSocket().on("connect", args -> {
            Log.d(TAG, "Socket connected");
        });

        client.getSocket().on("get rooms", args -> {
            List<Room> list = gson.fromJson((String) args[0], new TypeToken<List<Room>>(){}.getType());

            runOnUiThread(() ->  {
                arrayAdapter = new RoomsAdapter(this, list);
                listView.setAdapter(arrayAdapter);
            });
        });

        client.getSocket().connect();

        TextInputEditText roomName = findViewById(R.id.add_room_name);
        findViewById(R.id.button).setOnClickListener(event -> {
            String newRoomName = Objects.requireNonNull(roomName.getText()).toString();
            if (!"".equals(newRoomName))
                client.getSocket().emit("new room", gson.toJson(new Room(0, newRoomName)));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        client.getSocket().disconnect();
    }

    public static Socket createSocket() {
        Socket socket = null;
        try {
            socket = IO.socket(Config.SERVER_URL);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error creating socket", e);
        }
        return socket;
    }

    public class RoomsAdapter extends ArrayAdapter<Room> {
        public RoomsAdapter(Context context, List<Room> users) {
            super(context, 0, users);
        }

        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            Room room = getItem(position);
            if (convertView == null)
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.room_list_item, parent, false);

            assert room != null;

            TextView name = (TextView) convertView.findViewById(R.id.roomName);
            name.setOnClickListener(event -> {
                client.setRoom(room.getId());

                client.getSocket().emit("enter room", gson.toJson(client.getRoom()));

                Intent intent = new Intent(ChannelsActivity.this, ChatActivity.class);
                startActivity(intent);
            });
            name.setText(room.getName());

            return convertView;

        }
    }
}