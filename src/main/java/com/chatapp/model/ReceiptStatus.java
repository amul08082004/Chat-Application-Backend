package com.chatapp.model;

public enum ReceiptStatus {
    SENT,       // message persisted on server
    DELIVERED,  // recipient's client received it (was online / came online)
    SEEN        // recipient opened the chat and viewed it
}
