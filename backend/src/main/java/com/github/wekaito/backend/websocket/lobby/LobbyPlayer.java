package com.github.wekaito.backend.websocket.lobby;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

@AllArgsConstructor
@Getter
@Setter
public class LobbyPlayer {
    private WebSocketSession session;
    private String name;
    boolean ready;
    private String role; // "PLAYER" or "SPECTATOR"
}