package com.ruiners.banchat;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ruiners.banchat.model.Client;
import com.ruiners.banchat.model.Message;
import com.ruiners.banchat.model.ChatViewModel;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposables;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private Client client = new Client(0L);

    private ChatViewModel chatViewModel;
    private final CompositeDisposable viewSubscriptions = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client.setSocket(createSocket());
        client.getSocket().connect();

        Gson gson = new Gson();

        chatViewModel = new ChatViewModel(createMessagesListener(client.getSocket()));

        ListView listView = findViewById(R.id.list_view);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(arrayAdapter);

        client.getSocket().on("last messages", args -> {
            List<Message> list = gson.fromJson((String) args[0], new TypeToken<List<Message>>(){}.getType());
            chatViewModel.setLastMessages(list);

            List<String> messages = new ArrayList<>();
            for (Message message : list)
                messages.add(message.getMessage());

            runOnUiThread(() -> arrayAdapter.addAll(messages));
            client.getSocket().off("last messages");
        });

        viewSubscriptions.add(chatViewModel.getMessageList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    arrayAdapter.clear();
                    arrayAdapter.addAll(list);
                }));

        EditText editText = findViewById(R.id.edit_text);
        findViewById(R.id.send_button).setOnClickListener(event -> {
            Message chatMessage = new Message(editText.getText().toString(), client.getRoom());
            client.getSocket().emit("chat message", gson.toJson(chatMessage));

            editText.setText("");
        });

        chatViewModel.subscribe();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        chatViewModel.unsubscribe();
        viewSubscriptions.clear();

        // Disconnect WebSocket
        client.getSocket().disconnect();
    }

    public static Observable<String> createMessagesListener(Socket socket) {
        return Observable.create(subscriber -> {
            Emitter.Listener listener = args -> subscriber.onNext((String) args[0]);
            socket.on("chat message", listener);

            subscriber.setDisposable(Disposables.fromAction(() -> socket.off("chat message", listener)));
        });
    }

    public static Socket createSocket() {
        Socket socket = null;
        try {
            socket = IO.socket(Config.SERVER_URL);
            socket.on("connect", args -> Log.d(TAG, "Socket connected"));
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error creating socket", e);
        }
        return socket;
    }
}
