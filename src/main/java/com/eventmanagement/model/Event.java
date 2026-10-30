package com.eventmanagement.model;

import java.sql.Timestamp;

public class Event {

    private int id;
    private String title;
    private String description;
    private Timestamp eventDate;
    private String venue;
    private int capacity;
    private Timestamp createdAt;

    // Computed helper fields for display
    private int registeredCount;

    public Event() {
    }

    public Event(String title, String description, Timestamp eventDate, String venue, int capacity) {
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.venue = venue;
        this.capacity = capacity;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getEventDate() {
        return eventDate;
    }

    public void setEventDate(Timestamp eventDate) {
        this.eventDate = eventDate;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getRegisteredCount() {
        return registeredCount;
    }

    public void setRegisteredCount(int registeredCount) {
        this.registeredCount = registeredCount;
    }

    // Helper: calculate remaining seats
    public int getAvailableSeats() {
        return Math.max(0, capacity - registeredCount);
    }

    // Helper: is the event full?
    public boolean isFull() {
        return registeredCount >= capacity;
    }

    // Helper: is the event in the past?
    public boolean isPast() {
        return eventDate != null && eventDate.before(new Timestamp(System.currentTimeMillis()));
    }


}
