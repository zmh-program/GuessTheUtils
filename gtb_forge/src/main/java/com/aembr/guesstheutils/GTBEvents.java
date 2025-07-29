package com.aembr.guesstheutils;

import java.util.*;

public class GTBEvents {
    
    public interface BaseEvent {}
    
    // Game start event (converted from record)
    public static class GameStartEvent implements BaseEvent {
        private final Set<String> players;
        
        public GameStartEvent(Set<String> players) {
            this.players = players;
        }
        
        public Set<String> getPlayers() { return players; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GameStartEvent)) return false;
            GameStartEvent that = (GameStartEvent) o;
            return Objects.equals(players, that.players);
        }
        
        @Override
        public int hashCode() { return Objects.hash(players); }
    }
    
    // Builder change event
    public static class BuilderChangeEvent implements BaseEvent {
        private final String previous;
        private final String current;
        
        public BuilderChangeEvent(String previous, String current) {
            this.previous = previous;
            this.current = current;
        }
        
        public String getPrevious() { return previous; }
        public String getCurrent() { return current; }
    }
    
    // User becomes builder event
    public static class UserBuilderEvent implements BaseEvent {}
    
    // Round start event
    public static class RoundStartEvent implements BaseEvent {
        private final int currentRound;
        private final int totalRounds;
        
        public RoundStartEvent(int currentRound, int totalRounds) {
            this.currentRound = currentRound;
            this.totalRounds = totalRounds;
        }
        
        public int getCurrentRound() { return currentRound; }
        public int getTotalRounds() { return totalRounds; }
    }
    
    // Theme update event
    public static class ThemeUpdateEvent implements BaseEvent {
        private final String theme;
        
        public ThemeUpdateEvent(String theme) {
            this.theme = theme;
        }
        
        public String getTheme() { return theme; }
    }
    
    // State change event
    public static class StateChangeEvent implements BaseEvent {
        private final Object previousState;
        private final Object currentState;
        
        public StateChangeEvent(Object previousState, Object currentState) {
            this.previousState = previousState;
            this.currentState = currentState;
        }
        
        public Object getPreviousState() { return previousState; }
        public Object getCurrentState() { return currentState; }
    }
    
    // Event listener interface
    public interface EventListener<T extends BaseEvent> {
        void onEvent(T event);
    }
    
    // Event manager
    private final Map<Class<? extends BaseEvent>, List<EventListener<?>>> listeners = 
        new HashMap<Class<? extends BaseEvent>, List<EventListener<?>>>();
    
    @SuppressWarnings("unchecked")
    public <T extends BaseEvent> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<EventListener<?>>()).add(listener);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends BaseEvent> void publish(T event) {
        List<EventListener<?>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (EventListener<?> listener : eventListeners) {
                try {
                    ((EventListener<T>) listener).onEvent(event);
                } catch (Exception e) {
                    GuessTheUtils.LOGGER.error("Error in event listener", e);
                }
            }
        }
    }
    
    public void processTickUpdate(Tick tick) {
        // Basic tick processing - will be expanded
        if (tick.chatMessages != null) {
            for (String message : tick.chatMessages) {
                processChatMessage(message);
            }
        }
    }
    
    private void processChatMessage(String message) {
        // Basic chat message processing - will be expanded
    }
} 