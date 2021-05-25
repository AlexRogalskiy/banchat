package com.ruiners.banchat.model;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import com.google.gson.Gson;

public class ChatViewModel {
    private final CompositeDisposable subscriptions = new CompositeDisposable();
    private final Observable<String> chatMessageObservable;
    private final BehaviorSubject<List<String>> messageList = BehaviorSubject.create();
    private static List<Message> lastMessagesList;

    public ChatViewModel(Observable<String> chatMessageObservable) {
        this.chatMessageObservable = chatMessageObservable;
    }

    public void subscribe() {
        Gson gson = new Gson();
        subscriptions.add(chatMessageObservable
                .map(json -> gson.fromJson(json, Message.class))
                .scan(new ArrayList<>(), ChatViewModel::arrayAccumulatorFunction)
                .flatMap(list -> Observable.fromIterable(list).map(Message::toString).toList().toObservable())
                .subscribe(messageList::onNext));

    }

    public void unsubscribe() {
        subscriptions.clear();
    }

    private static List<Message> arrayAccumulatorFunction(List<Message> previousMessagesList, Message newMessage) {
        ArrayList<Message> newMessagesList = new ArrayList<>(lastMessagesList);
        newMessagesList.addAll(previousMessagesList);
        newMessagesList.add(newMessage);
        return newMessagesList;
    }

    public Observable<List<String>> getMessageList() {
        return messageList.hide();
    }

    public void setLastMessages(List<Message> lastMessages) {
        lastMessagesList = lastMessages;
    }
}
