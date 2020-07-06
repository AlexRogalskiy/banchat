package com.ruiners.banchat;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.ruiners.banchat.model.ChatMessage;
import com.ruiners.banchat.model.ChatViewModel;

import java.net.URISyntaxException;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private Socket socket;
    private ChatViewModel chatViewModel;
    private final CompositeDisposable viewSubscriptions = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        socket = createSocket();
        socket.on("connect", args -> {
            //socket.send("{\"username\":\"admin\",\"password\":\"admin\"}");
            Log.d(TAG, "Socket connected");
        });
        socket.connect();

        Gson gson = new Gson();

        chatViewModel = new ChatViewModel(createListener(socket));

        ListView listView = findViewById(R.id.list_view);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(arrayAdapter);

        viewSubscriptions.add(chatViewModel.getMessageList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    arrayAdapter.clear();
                    arrayAdapter.addAll(list);
                }));

        EditText editText = findViewById(R.id.edit_text);
        findViewById(R.id.send_button).setOnClickListener(event -> {
            ChatMessage chatMessage = new ChatMessage(editText.getText().toString());
            socket.emit("message", gson.toJson(chatMessage));
        });

        chatViewModel.subscribe();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        chatViewModel.unsubscribe();
        viewSubscriptions.clear();

        // Disconnect WebSocket
        socket.disconnect();
    }

    public static Observable<String> createListener(Socket socket) {
        return Observable.create(subscriber -> {
            Emitter.Listener listener = args -> subscriber.onNext((String) args[0]);
            socket.on("message", listener);
            subscriber.setDisposable(Disposables.fromAction(
                    () -> socket.off("message", listener)
            ));
        });
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
}
