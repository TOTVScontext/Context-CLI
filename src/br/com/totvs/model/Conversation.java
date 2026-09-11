package br.com.totvs.model;

import java.util.List;

public class Conversation {
    private String id;
    private String text;
    private List<String> participants;

    public Conversation(String id, String text, List<String> participants) {
        this.id = id;
        this.text = text;
        this.participants = participants;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }
}
