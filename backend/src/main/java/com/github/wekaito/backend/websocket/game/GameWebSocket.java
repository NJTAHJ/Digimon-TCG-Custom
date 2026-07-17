package com.github.wekaito.backend.websocket.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wekaito.backend.models.Card;
import com.github.wekaito.backend.DeckService;
import com.github.wekaito.backend.security.MongoUserDetailsService;
import com.github.wekaito.backend.websocket.game.models.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.*;

@Service
@Getter
@RequiredArgsConstructor
public class GameWebSocket extends TextWebSocketHandler {

    private final MongoUserDetailsService mongoUserDetailsService;
    private final DeckService deckService;
    private final CardJsonConverter cardJsonConverter;

    public final ConcurrentHashMap<String, GameRoom> gameRooms = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService SHARED_SCHEDULER = Executors.newScheduledThreadPool(10);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String[] simpleIdCommands = {"/updateAttackPhase", "/activateEffect", "/activateTarget", "/emote"};

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        // do nothing
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        Principal principal = session.getPrincipal();
        if (principal == null) return;
        String username = principal.getName();

        Optional<GameRoom> gameRoomOpt = gameRooms.values().stream().filter(room ->
                room.getSessions().stream().anyMatch(s -> s.getId().equals(session.getId()))
        ).findFirst();

        if (gameRoomOpt.isPresent()) {
            GameRoom gameRoom = gameRoomOpt.get();
            gameRoom.removeSession(session);

            boolean isPlayer = gameRoom.getPlayer1().username().equals(username) || 
                               gameRoom.getPlayer2().username().equals(username);

            if (isPlayer) {
                gameRoom.sendMessageToOtherSessions(session, "[OPPONENT_DISCONNECTED]");
            }

            if (gameRoom.isEmpty()) {
                GameRoom removed = gameRooms.remove(gameRoom.getRoomId());
                if (removed != null) {
                    removed.cancelAllScheduledTasks();
                }
            }
        }
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        String[] parts = payload.split(":", 2);

        if (parts[0].equals("/joinGame")) {
            computeGameRoom(session, parts[1]);
            return;
        }

        String gameId = parts[0];
        String roomMessage = parts[1];

        GameRoom gameRoom = findGameRoomById(gameId);
        if (gameRoom == null) return;

        // --- SPECTATOR SECURITY BLOCK ---
        Principal principal = session.getPrincipal();
        if (principal != null) {
            String username = principal.getName();
            boolean isPlayer = gameRoom.getPlayer1().username().equals(username) || 
                               gameRoom.getPlayer2().username().equals(username);
            
            if (!isPlayer) {
                // Ignore any state-changing requests from spectators. They can only heartbeat or chat.
                if (!roomMessage.startsWith("/heartbeat") && !roomMessage.startsWith("/chatMessage:")) {
                    return;
                }
            }
        }

        if (roomMessage.startsWith("/mulligan:")) {
            boolean currentPlayerDecision = roomMessage.split(":")[1].equals("true");
            gameRoom.setMulliganDecisionForSession(session, currentPlayerDecision);
        }

        if (roomMessage.startsWith("/restartGame:")) {
            boolean isThisPlayerStarting = roomMessage.split(":")[1].equals("first");
            String username = Objects.requireNonNull(session.getPrincipal()).getName();
            String startingPlayerUsername = isThisPlayerStarting ? username :
                    gameRoom.getPlayer1().username().equals(username) ? gameRoom.getPlayer2().username() : gameRoom.getPlayer1().username();
            gameRoom.initiateGame();
            gameRoom.setStartingPlayer(startingPlayerUsername);
            scheduleCardDistribution(gameRoom);
        }

        if (roomMessage.startsWith("/heartbeat")) gameRoom.updateLastHearBeat(session);
        if (roomMessage.startsWith("/attack:")) handleAttack(gameRoom, session, roomMessage);
        if (roomMessage.startsWith("/moveCard:")) handleSendMoveCard(gameRoom, session, roomMessage);
        if (roomMessage.startsWith("/moveCardToStack:")) handleSendMoveToStack(gameRoom, session, roomMessage);
        if (roomMessage.startsWith("/setModifiers:")) handleSendSetModifiers(gameRoom, session, roomMessage);
        if (roomMessage.startsWith("/tiltCard:")) handleTiltCard(gameRoom, session, roomMessage);
        if (roomMessage.startsWith("/flipCard:")) handleFlipCard(gameRoom, session, roomMessage);
        if (roomMessage.startsWith("/updateMemory:")) handleMemoryUpdate(gameRoom, session, roomMessage);
        if (roomMessage.startsWith("/chatMessage:")) sendChatMessage(gameRoom, session, roomMessage);
        if (roomMessage.startsWith("/createToken:")) handleCreateToken(gameRoom, session, roomMessage);
        if (roomMessage.startsWith("/unsuspendAll:")) handleUnsuspendAll(gameRoom, session);
        if (Arrays.stream(simpleIdCommands).anyMatch(roomMessage::startsWith)) handleCommandWithId(gameRoom, session, roomMessage);
        else {
            String[] roomMessageParts = roomMessage.split(":", 2);
            String command = roomMessageParts[0];
            if (command.equals("/updatePhase")) gameRoom.progressPhase();
            gameRoom.sendMessageToOtherSessions(session, convertCommand(command));
        }
    }

    private void sendChatMessage(GameRoom gameRoom, WebSocketSession session, String roomMessage) {
        if (!gameRoom.hasFullConnection()) return;
        String userName = Objects.requireNonNull(session.getPrincipal()).getName();

        String[] roomMessageParts = roomMessage.split(":", 2);
        if (roomMessageParts.length < 2) return;
        String chatMessage = roomMessageParts[1];
        
        String formattedMessage = userName + "﹕" + chatMessage;
        storeChatMessage(gameRoom, formattedMessage);
        gameRoom.sendMessageToOtherSessions(session, "[CHAT_MESSAGE]:" + formattedMessage);
    }
    
    private void storeChatMessage(GameRoom gameRoom, String message) {
        String[] currentChat = gameRoom.getChat();
        if (currentChat == null) {
            gameRoom.setChat(new String[]{message});
        } else {
            String[] newChat = new String[currentChat.length + 1];
            System.arraycopy(currentChat, 0, newChat, 0, currentChat.length);
            newChat[currentChat.length] = message;
            gameRoom.setChat(newChat);
        }
    }

    private String convertCommand(String command) {
        return switch (command) {
            case "/surrender" -> "[SURRENDER]";
            case "/restartRequestAsFirst" -> "[RESTART_AS_FIRST]";
            case "/restartRequestAsSecond" -> "[RESTART_AS_SECOND]";
            case "/acceptRestart" -> "[ACCEPT_RESTART]";
            case "/openedSecurity" -> "[SECURITY_VIEWED]";
            case "/playRevealSfx" -> "[REVEAL_SFX]";
            case "/playSecurityRevealSfx" -> "[SECURITY_REVEAL_SFX]";
            case "/playPlaceCardSfx" -> "[PLACE_CARD_SFX]";
            case "/playDrawCardSfx" -> "[DRAW_CARD_SFX]";
            case "/playSuspendCardSfx" -> "[SUSPEND_CARD_SFX]";
            case "/playUnsuspendCardSfx" -> "[UNSUSPEND_CARD_SFX]";
            case "/playButtonClickSfx" -> "[BUTTON_CLICK_SFX]";
            case "/playTrashCardSfx" -> "[TRASH_CARD_SFX]";
            case "/playShuffleDeckSfx" -> "[SHUFFLE_DECK_SFX]";
            case "/playNextPhaseSfx" -> "[NEXT_PHASE_SFX]";
            case "/playPassTurnSfx" -> "[PASS_TURN_SFX]";
            case "/updatePhase" -> "[UPDATE_PHASE]";
            case "/unsuspendAll" -> "[UNSUSPEND_ALL]";
            case "/resolveCounterBlock" -> "[RESOLVE_COUNTER_BLOCK]";
            case "/activateTarget" -> "[ACTIVATE_TARGET]";
            case "/activateEffect" -> "[ACTIVATE_EFFECT]";
            case "/updateAttackPhase" -> "[OPPONENT_ATTACK_PHASE]";
            case "/emote" -> "[EMOTE]";
            default -> "";
        };
    }

    private String getOppositePosition(String fromTo) {
        if (fromTo.startsWith("my")) return fromTo.replaceFirst("my", "opponent");
        else if (fromTo.startsWith("opponent")) return fromTo.replaceFirst("opponent", "my");
        return fromTo;
    }

    private GameRoom findGameRoomById(String gameId) {
        return gameRooms.get(gameId);
    }

    public Optional<GameRoom> findGameRoomBySession(WebSocketSession session) {
        String username = Objects.requireNonNull(session.getPrincipal()).getName();
        return gameRooms.values().stream().filter(room ->
                room.getPlayer1().username().equals(username) || room.getPlayer2().username().equals(username)
        ).findFirst();
    }
    
    private String mapClientToServer(String clientPosition, String username, GameRoom gameRoom) {
        boolean isPlayer1 = gameRoom.getPlayer1().username().equals(username);
        return switch (clientPosition) {
            case "myHand" -> isPlayer1 ? "player1Hand" : "player2Hand";
            case "myDeckField" -> isPlayer1 ? "player1Deck" : "player2Deck";
            case "myEggDeck" -> isPlayer1 ? "player1EggDeck" : "player2EggDeck";
            case "myTrash" -> isPlayer1 ? "player1Trash" : "player2Trash";
            case "mySecurity" -> isPlayer1 ? "player1Security" : "player2Security";
            case "myReveal" -> isPlayer1 ? "player1Reveal" : "player2Reveal";
            case "myBreedingArea" -> isPlayer1 ? "player1BreedingArea" : "player2BreedingArea";
            case "myDigi1" -> isPlayer1 ? "player1Digi1" : "player2Digi1";
            case "myDigi2" -> isPlayer1 ? "player1Digi2" : "player2Digi2";
            case "myDigi3" -> isPlayer1 ? "player1Digi3" : "player2Digi3";
            case "myDigi4" -> isPlayer1 ? "player1Digi4" : "player2Digi4";
            case "myDigi5" -> isPlayer1 ? "player1Digi5" : "player2Digi5";
            case "myDigi6" -> isPlayer1 ? "player1Digi6" : "player2Digi6";
            case "myDigi7" -> isPlayer1 ? "player1Digi7" : "player2Digi7";
            case "myDigi8" -> isPlayer1 ? "player1Digi8" : "player2Digi8";
            case "myDigi9" -> isPlayer1 ? "player1Digi9" : "player2Digi9";
            case "myDigi10" -> isPlayer1 ? "player1Digi10" : "player2Digi10";
            case "myDigi11" -> isPlayer1 ? "player1Digi11" : "player2Digi11";
            case "myDigi12" -> isPlayer1 ? "player1Digi12" : "player2Digi12";
            case "myDigi13" -> isPlayer1 ? "player1Digi13" : "player2Digi13";
            case "myDigi14" -> isPlayer1 ? "player1Digi14" : "player2Digi14";
            case "myDigi15" -> isPlayer1 ? "player1Digi15" : "player2Digi15";
            case "myDigi16" -> isPlayer1 ? "player1Digi16" : "player2Digi16";
            case "myDigi17" -> isPlayer1 ? "player1Digi17" : "player2Digi17";
            case "myDigi18" -> isPlayer1 ? "player1Digi18" : "player2Digi18";
            case "myDigi19" -> isPlayer1 ? "player1Digi19" : "player2Digi19";
            case "myDigi20" -> isPlayer1 ? "player1Digi20" : "player2Digi20";
            case "myDigi21" -> isPlayer1 ? "player1Digi21" : "player2Digi21";
            case "myLink1" -> isPlayer1 ? "player1Link1" : "player2Link1";
            case "myLink2" -> isPlayer1 ? "player1Link2" : "player2Link2";
            case "myLink3" -> isPlayer1 ? "player1Link3" : "player2Link3";
            case "myLink4" -> isPlayer1 ? "player1Link4" : "player2Link4";
            case "myLink5" -> isPlayer1 ? "player1Link5" : "player2Link5";
            case "myLink6" -> isPlayer1 ? "player1Link6" : "player2Link6";
            case "myLink7" -> isPlayer1 ? "player1Link7" : "player2Link7";
            case "myLink8" -> isPlayer1 ? "player1Link8" : "player2Link8";
            case "myLink9" -> isPlayer1 ? "player1Link9" : "player2Link9";
            case "myLink10" -> isPlayer1 ? "player1Link10" : "player2Link10";
            case "myLink11" -> isPlayer1 ? "player1Link11" : "player2Link11";
            case "myLink12" -> isPlayer1 ? "player1Link12" : "player2Link12";
            case "myLink13" -> isPlayer1 ? "player1Link13" : "player2Link13";
            case "myLink14" -> isPlayer1 ? "player1Link14" : "player2Link14";
            case "myLink15" -> isPlayer1 ? "player1Link15" : "player2Link15";
            case "myLink16" -> isPlayer1 ? "player1Link16" : "player2Link16";

            case "opponentHand" -> isPlayer1 ? "player2Hand" : "player1Hand";
            case "opponentDeckField" -> isPlayer1 ? "player2Deck" : "player1Deck";
            case "opponentEggDeck" -> isPlayer1 ? "player2EggDeck" : "player1EggDeck";
            case "opponentTrash" -> isPlayer1 ? "player2Trash" : "player1Trash";
            case "opponentSecurity" -> isPlayer1 ? "player2Security" : "player1Security";
            case "opponentReveal" -> isPlayer1 ? "player2Reveal" : "player1Reveal";
            case "opponentBreedingArea" -> isPlayer1 ? "player2BreedingArea" : "player1BreedingArea";
            case "opponentDigi1" -> isPlayer1 ? "player2Digi1" : "player1Digi1";
            case "opponentDigi2" -> isPlayer1 ? "player2Digi2" : "player1Digi2";
            case "opponentDigi3" -> isPlayer1 ? "player2Digi3" : "player1Digi3";
            case "opponentDigi4" -> isPlayer1 ? "player2Digi4" : "player1Digi4";
            case "opponentDigi5" -> isPlayer1 ? "player2Digi5" : "player1Digi5";
            case "opponentDigi6" -> isPlayer1 ? "player2Digi6" : "player1Digi6";
            case "opponentDigi7" -> isPlayer1 ? "player2Digi7" : "player1Digi7";
            case "opponentDigi8" -> isPlayer1 ? "player2Digi8" : "player1Digi8";
            case "opponentDigi9" -> isPlayer1 ? "player2Digi9" : "player1Digi9";
            case "opponentDigi10" -> isPlayer1 ? "player2Digi10" : "player1Digi10";
            case "opponentDigi11" -> isPlayer1 ? "player2Digi11" : "player1Digi11";
            case "opponentDigi12" -> isPlayer1 ? "player2Digi12" : "player1Digi12";
            case "opponentDigi13" -> isPlayer1 ? "player2Digi13" : "player1Digi13";
            case "opponentDigi14" -> isPlayer1 ? "player2Digi14" : "player1Digi14";
            case "opponentDigi15" -> isPlayer1 ? "player2Digi15" : "player1Digi15";
            case "opponentDigi16" -> isPlayer1 ? "player2Digi16" : "player1Digi16";
            case "opponentDigi17" -> isPlayer1 ? "player2Digi17" : "player1Digi17";
            case "opponentDigi18" -> isPlayer1 ? "player2Digi18" : "player1Digi18";
            case "opponentDigi19" -> isPlayer1 ? "player2Digi19" : "player1Digi19";
            case "opponentDigi20" -> isPlayer1 ? "player2Digi20" : "player1Digi20";
            case "opponentDigi21" -> isPlayer1 ? "player2Digi21" : "player1Digi21";
            case "opponentLink1" -> isPlayer1 ? "player2Link1" : "player1Link1";
            case "opponentLink2" -> isPlayer1 ? "player2Link2" : "player1Link2";
            case "opponentLink3" -> isPlayer1 ? "player2Link3" : "player1Link3";
            case "opponentLink4" -> isPlayer1 ? "player2Link4" : "player1Link4";
            case "opponentLink5" -> isPlayer1 ? "player2Link5" : "player1Link5";
            case "opponentLink6" -> isPlayer1 ? "player2Link6" : "player1Link6";
            case "opponentLink7" -> isPlayer1 ? "player2Link7" : "player1Link7";
            case "opponentLink8" -> isPlayer1 ? "player2Link8" : "player1Link8";
            case "opponentLink9" -> isPlayer1 ? "player2Link9" : "player1Link9";
            case "opponentLink10" -> isPlayer1 ? "player2Link10" : "player1Link10";
            case "opponentLink11" -> isPlayer1 ? "player2Link11" : "player1Link11";
            case "opponentLink12" -> isPlayer1 ? "player2Link12" : "player1Link12";
            case "opponentLink13" -> isPlayer1 ? "player2Link13" : "player1Link13";
            case "opponentLink14" -> isPlayer1 ? "player2Link14" : "player1Link14";
            case "opponentLink15" -> isPlayer1 ? "player2Link15" : "player1Link15";
            case "opponentLink16" -> isPlayer1 ? "player2Link16" : "player1Link16";

            default -> clientPosition;
        };
    }

    private String mapServerToClient(String serverPosition, String destUsername, GameRoom gameRoom) {
        // Force spectators to inherit Player 2's perspective so they match the client's fallback view
        boolean isPlayer1View = gameRoom.getPlayer1().username().equals(destUsername);
        
        return switch (serverPosition) {
            case "player1Hand" -> isPlayer1View ? "myHand" : "opponentHand";
            case "player1Deck" -> isPlayer1View ? "myDeckField" : "opponentDeckField";
            case "player1EggDeck" -> isPlayer1View ? "myEggDeck" : "opponentEggDeck";
            case "player1Trash" -> isPlayer1View ? "myTrash" : "opponentTrash";
            case "player1Security" -> isPlayer1View ? "mySecurity" : "opponentSecurity";
            case "player1Reveal" -> isPlayer1View ? "myReveal" : "opponentReveal";
            case "player1BreedingArea" -> isPlayer1View ? "myBreedingArea" : "opponentBreedingArea";
            case "player1Digi1" -> isPlayer1View ? "myDigi1" : "opponentDigi1";
            case "player1Digi2" -> isPlayer1View ? "myDigi2" : "opponentDigi2";
            case "player1Digi3" -> isPlayer1View ? "myDigi3" : "opponentDigi3";
            case "player1Digi4" -> isPlayer1View ? "myDigi4" : "opponentDigi4";
            case "player1Digi5" -> isPlayer1View ? "myDigi5" : "opponentDigi5";
            case "player1Digi6" -> isPlayer1View ? "myDigi6" : "opponentDigi6";
            case "player1Digi7" -> isPlayer1View ? "myDigi7" : "opponentDigi7";
            case "player1Digi8" -> isPlayer1View ? "myDigi8" : "opponentDigi8";
            case "player1Digi9" -> isPlayer1View ? "myDigi9" : "opponentDigi9";
            case "player1Digi10" -> isPlayer1View ? "myDigi10" : "opponentDigi10";
            case "player1Digi11" -> isPlayer1View ? "myDigi11" : "opponentDigi11";
            case "player1Digi12" -> isPlayer1View ? "myDigi12" : "opponentDigi12";
            case "player1Digi13" -> isPlayer1View ? "myDigi13" : "opponentDigi13";
            case "player1Digi14" -> isPlayer1View ? "myDigi14" : "opponentDigi14";
            case "player1Digi15" -> isPlayer1View ? "myDigi15" : "opponentDigi15";
            case "player1Digi16" -> isPlayer1View ? "myDigi16" : "opponentDigi16";
            case "player1Digi17" -> isPlayer1View ? "myDigi17" : "opponentDigi17";
            case "player1Digi18" -> isPlayer1View ? "myDigi18" : "opponentDigi18";
            case "player1Digi19" -> isPlayer1View ? "myDigi19" : "opponentDigi19";
            case "player1Digi20" -> isPlayer1View ? "myDigi20" : "opponentDigi20";
            case "player1Digi21" -> isPlayer1View ? "myDigi21" : "opponentDigi21";
            case "player1Link1" -> isPlayer1View ? "myLink1" : "opponentLink1";
            case "player1Link2" -> isPlayer1View ? "myLink2" : "opponentLink2";
            case "player1Link3" -> isPlayer1View ? "myLink3" : "opponentLink3";
            case "player1Link4" -> isPlayer1View ? "myLink4" : "opponentLink4";
            case "player1Link5" -> isPlayer1View ? "myLink5" : "opponentLink5";
            case "player1Link6" -> isPlayer1View ? "myLink6" : "opponentLink6";
            case "player1Link7" -> isPlayer1View ? "myLink7" : "opponentLink7";
            case "player1Link8" -> isPlayer1View ? "myLink8" : "opponentLink8";
            case "player1Link9" -> isPlayer1View ? "myLink9" : "opponentLink9";
            case "player1Link10" -> isPlayer1View ? "myLink10" : "opponentLink10";
            case "player1Link11" -> isPlayer1View ? "myLink11" : "opponentLink11";
            case "player1Link12" -> isPlayer1View ? "myLink12" : "opponentLink12";
            case "player1Link13" -> isPlayer1View ? "myLink13" : "opponentLink13";
            case "player1Link14" -> isPlayer1View ? "myLink14" : "opponentLink14";
            case "player1Link15" -> isPlayer1View ? "myLink15" : "opponentLink15";
            case "player1Link16" -> isPlayer1View ? "myLink16" : "opponentLink16";

            case "player2Hand" -> isPlayer1View ? "opponentHand" : "myHand";
            case "player2Deck" -> isPlayer1View ? "opponentDeckField" : "myDeckField";
            case "player2EggDeck" -> isPlayer1View ? "opponentEggDeck" : "myEggDeck";
            case "player2Trash" -> isPlayer1View ? "opponentTrash" : "myTrash";
            case "player2Security" -> isPlayer1View ? "opponentSecurity" : "mySecurity";
            case "player2Reveal" -> isPlayer1View ? "opponentReveal" : "myReveal";
            case "player2BreedingArea" -> isPlayer1View ? "opponentBreedingArea" : "myBreedingArea";
            case "player2Digi1" -> isPlayer1View ? "opponentDigi1" : "myDigi1";
            case "player2Digi2" -> isPlayer1View ? "opponentDigi2" : "myDigi2";
            case "player2Digi3" -> isPlayer1View ? "opponentDigi3" : "myDigi3";
            case "player2Digi4" -> isPlayer1View ? "opponentDigi4" : "myDigi4";
            case "player2Digi5" -> isPlayer1View ? "opponentDigi5" : "myDigi5";
            case "player2Digi6" -> isPlayer1View ? "opponentDigi6" : "myDigi6";
            case "player2Digi7" -> isPlayer1View ? "opponentDigi7" : "myDigi7";
            case "player2Digi8" -> isPlayer1View ? "opponentDigi8" : "myDigi8";
            case "player2Digi9" -> isPlayer1View ? "opponentDigi9" : "myDigi9";
            case "player2Digi10" -> isPlayer1View ? "opponentDigi10" : "myDigi10";
            case "player2Digi11" -> isPlayer1View ? "opponentDigi11" : "myDigi11";
            case "player2Digi12" -> isPlayer1View ? "opponentDigi12" : "myDigi12";
            case "player2Digi13" -> isPlayer1View ? "opponentDigi13" : "myDigi13";
            case "player2Digi14" -> isPlayer1View ? "opponentDigi14" : "myDigi14";
            case "player2Digi15" -> isPlayer1View ? "opponentDigi15" : "myDigi15";
            case "player2Digi16" -> isPlayer1View ? "opponentDigi16" : "myDigi16";
            case "player2Digi17" -> isPlayer1View ? "opponentDigi17" : "myDigi17";
            case "player2Digi18" -> isPlayer1View ? "opponentDigi18" : "myDigi18";
            case "player2Digi19" -> isPlayer1View ? "opponentDigi19" : "myDigi19";
            case "player2Digi20" -> isPlayer1View ? "opponentDigi20" : "myDigi20";
            case "player2Digi21" -> isPlayer1View ? "opponentDigi21" : "myDigi21";
            case "player2Link1" -> isPlayer1View ? "opponentLink1" : "myLink1";
            case "player2Link2" -> isPlayer1View ? "opponentLink2" : "myLink2";
            case "player2Link3" -> isPlayer1View ? "opponentLink3" : "myLink3";
            case "player2Link4" -> isPlayer1View ? "opponentLink4" : "myLink4";
            case "player2Link5" -> isPlayer1View ? "opponentLink5" : "myLink5";
            case "player2Link6" -> isPlayer1View ? "opponentLink6" : "myLink6";
            case "player2Link7" -> isPlayer1View ? "opponentLink7" : "myLink7";
            case "player2Link8" -> isPlayer1View ? "opponentLink8" : "myLink8";
            case "player2Link9" -> isPlayer1View ? "opponentLink9" : "myLink9";
            case "player2Link10" -> isPlayer1View ? "opponentLink10" : "myLink10";
            case "player2Link11" -> isPlayer1View ? "opponentLink11" : "myLink11";
            case "player2Link12" -> isPlayer1View ? "opponentLink12" : "myLink12";
            case "player2Link13" -> isPlayer1View ? "opponentLink13" : "myLink13";
            case "player2Link14" -> isPlayer1View ? "opponentLink14" : "myLink14";
            case "player2Link15" -> isPlayer1View ? "opponentLink15" : "myLink15";
            case "player2Link16" -> isPlayer1View ? "opponentLink16" : "myLink16";

            default -> serverPosition;
        };
    }

    private void updateBoardStateForStackMove(GameRoom gameRoom, String cardId, String fromClient, String toClient, String topOrBottom, String facing, String username) {
        BoardState boardState = gameRoom.getBoardState();
        if (boardState == null) return;

        String fromServer = mapClientToServer(fromClient, username, gameRoom);
        String toServer = mapClientToServer(toClient, username, gameRoom);

        List<GameCard> fromList = boardState.getFieldByName(fromServer);
        GameCard cardToMove = fromList.stream()
                .filter(card -> card.getId().toString().equals(cardId))
                .findFirst()
                .orElse(null);

        if (cardToMove == null) return;

        fromList.remove(cardToMove);
        boardState.setFieldByName(fromServer, fromList);

        if (toServer.contains("Hand") || toServer.contains("Deck") || toServer.contains("EggDeck") || toServer.contains("Trash") || toServer.contains("Security")) {
            if (cardToMove.getUniqueCardNumber().contains("TOKEN")) return;
        }

        if (topOrBottom.equals("Top")) cardToMove.setIsTilted(false);

        if (toServer.equals("player1Hand") || toServer.equals("player2Hand") ||
            toServer.equals("player1Deck") || toServer.equals("player2Deck") ||
            toServer.equals("player1EggDeck") || toServer.equals("player2EggDeck") ||
            toServer.equals("player1Trash") || toServer.equals("player2Trash")) {
            cardToMove.setModifiers(new Modifiers(0, 0, new ArrayList<>(), cardToMove.getColor()));
        }

        if (facing != null) cardToMove.setIsFaceUp(facing.equals("up"));

        List<GameCard> toList = boardState.getFieldByName(toServer);
        if (topOrBottom.equals("Top")) toList.add(0, cardToMove);
        else toList.add(cardToMove);

        boardState.setFieldByName(toServer, toList);
    }

    private void updateBoardStateForCardMove(GameRoom gameRoom, String cardId, String fromClient, String toClient, String username) {
        BoardState boardState = gameRoom.getBoardState();
        if (boardState == null) return;

        String fromServer = mapClientToServer(fromClient, username, gameRoom);
        String toServer = mapClientToServer(toClient, username, gameRoom);

        List<GameCard> fromList = boardState.getFieldByName(fromServer);
        GameCard cardToMove = fromList.stream()
                .filter(card -> card.getId().toString().equals(cardId))
                .findFirst()
                .orElse(null);

        if (cardToMove == null) return;

        fromList.remove(cardToMove);
        boardState.setFieldByName(fromServer, fromList);

        if (toServer.contains("Hand") || toServer.contains("Deck") || toServer.contains("EggDeck") || toServer.contains("Trash") || toServer.contains("Security")) {
            if (cardToMove.getUniqueCardNumber().contains("TOKEN")) return;
        }

        if (toServer.equals("player1Hand") || toServer.equals("player2Hand") ||
            toServer.equals("player1Deck") || toServer.equals("player2Deck") ||
            toServer.equals("player1EggDeck") || toServer.equals("player2EggDeck") ||
            toServer.equals("player1Trash") || toServer.equals("player2Trash")) {
            cardToMove.setModifiers(new Modifiers(0, 0, new ArrayList<>(), cardToMove.getColor()));
        }

        if (toServer.contains("Hand") || toServer.contains("Deck")) cardToMove.setIsFaceUp(false);
        else if (fromServer.contains("EggDeck") || toServer.contains("Reveal") || toServer.contains("Trash") || (fromServer.contains("Hand") && !toServer.contains("Hand") && !toServer.contains("Deck"))) {
            cardToMove.setIsFaceUp(true);
        }

        List<GameCard> toList = boardState.getFieldByName(toServer);
        if (!toList.isEmpty() && !fromServer.equals(toServer)) {
            GameCard prevTopCard = toList.get(toList.size() - 1);
            if (prevTopCard.isTilted) {
                prevTopCard.isTilted = false;
                cardToMove.setIsTilted(!(toServer.contains("Digi17") || toServer.contains("Digi18") || toServer.contains("Digi19") || toServer.contains("Digi20") || toServer.contains("Digi21")));
            } else {
                cardToMove.setIsTilted(false);
            }

            Modifiers cardMods = cardToMove.getModifiers();
            Modifiers prevMods = prevTopCard.getModifiers();

            int newCardPlusDp = (cardMods.plusDp() == 0) ? prevMods.plusDp() : cardMods.plusDp();
            int newCardPlusSecurityAttacks = (cardMods.plusSecurityAttacks() == 0) ? prevMods.plusSecurityAttacks() : cardMods.plusSecurityAttacks();
            List<String> newCardKeywords = cardMods.keywords().isEmpty() ? new ArrayList<>(prevMods.keywords()) : cardMods.keywords();

            Modifiers newCardModifiers = new Modifiers(newCardPlusDp, newCardPlusSecurityAttacks, newCardKeywords, cardMods.colors());
            Modifiers newPrevModifiers = new Modifiers(0, 0, new ArrayList<>(), prevTopCard.getColor());

            cardToMove.setModifiers(newCardModifiers);
            prevTopCard.setModifiers(newPrevModifiers);
        } else if (fromServer.equals(toServer)) {
            cardToMove.setIsTilted(false);
        }

        toList.add(cardToMove);
        boardState.setFieldByName(toServer, toList);
    }

    private void broadcastGameAction(GameRoom gameRoom, WebSocketSession sender, String actionType, String cardId, String fromServer, String toServer, String... extra) {
        for (WebSocketSession s : gameRoom.getSessions()) {
            if (!s.isOpen()) continue;
            if (s.getId().equals(sender.getId())) continue;

            String destUsername = s.getPrincipal() != null ? s.getPrincipal().getName() : "";
            String relativeFrom = mapServerToClient(fromServer, destUsername, gameRoom);
            String relativeTo = mapServerToClient(toServer, destUsername, gameRoom);

            try {
                if (actionType.equals("MOVE_CARD")) {
                    s.sendMessage(new TextMessage("[MOVE_CARD]:" + cardId + ":" + relativeFrom + ":" + relativeTo));
                } else if (actionType.equals("MOVE_CARD_TO_STACK")) {
                    String topOrBottom = extra[0];
                    String facing = extra[1];
                    s.sendMessage(new TextMessage("[MOVE_CARD_TO_STACK]:" + topOrBottom + ":" + cardId + ":" + relativeFrom + ":" + relativeTo + ":" + facing));
                } else if (actionType.equals("TILT_CARD")) {
                    s.sendMessage(new TextMessage("[TILT_CARD]:" + cardId + ":" + relativeTo));
                } else if (actionType.equals("FLIP_CARD")) {
                    s.sendMessage(new TextMessage("[FLIP_CARD]:" + cardId + ":" + relativeTo));
                } else if (actionType.equals("SET_MODIFIERS")) {
                    String modifiersJson = extra[0];
                    s.sendMessage(new TextMessage("[SET_MODIFIERS]:" + cardId + ":" + relativeTo + ":" + modifiersJson));
                } else if (actionType.equals("CREATE_TOKEN")) {
                    String cardName = extra[0];
                    s.sendMessage(new TextMessage("[CREATE_TOKEN]:" + cardId + ":" + cardName + ":" + relativeTo));
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void computeGameRoom(WebSocketSession session, String gameId) throws IOException {
        GameRoom gameRoom = gameRooms.get(gameId);
        boolean isNewRoom = false;

        if (gameRoom == null) {
            String[] usernames = gameId.split("‗");
            try {
                String avatar1 = mongoUserDetailsService.getAvatar(usernames[0]);
                String avatar2 = mongoUserDetailsService.getAvatar(usernames[1]);
                String deckId1 = mongoUserDetailsService.getActiveDeck(usernames[0]);
                String deckId2 = mongoUserDetailsService.getActiveDeck(usernames[1]);
                String mainSleeve1 = deckService.getDeckSleeveById(deckId1);
                String mainSleeve2 = deckService.getDeckSleeveById(deckId2);
                String eggSleeve1 = deckService.getEggDeckSleeveById(deckId1);
                String eggSleeve2 = deckService.getEggDeckSleeveById(deckId2);

                Player player1 = new Player(usernames[0], avatar1, mainSleeve1, eggSleeve1);
                Player player2 = new Player(usernames[1], avatar2, mainSleeve2, eggSleeve2);

                List<Card> player1MainDeck = deckService.getMainDeckCardsById(deckId1);
                List<Card> player1EggDeck = deckService.getEggDeckCardsById(deckId1);
                List<Card> player2MainDeck = deckService.getMainDeckCardsById(deckId2);
                List<Card> player2EggDeck = deckService.getEggDeckCardsById(deckId2);

                GameRoom newGameRoom = new GameRoom(gameId, player1, player1MainDeck, player1EggDeck, player2, player2MainDeck, player2EggDeck);
                newGameRoom.setChat(new String[0]);

                GameRoom existingRoom = gameRooms.putIfAbsent(gameId, newGameRoom);
                if (existingRoom == null) {
                    gameRoom = newGameRoom;
                    isNewRoom = true;
                } else {
                    gameRoom = existingRoom;
                }
            } catch (Exception e) {
                return;
            }
        }

        if (isNewRoom) {
            startGameRoomScheduledTasks(gameRoom);
        }

        gameRoom.addSession(session);
        String username = Objects.requireNonNull(session.getPrincipal()).getName();
        boolean isPlayer = gameRoom.getPlayer1().username().equals(username) || gameRoom.getPlayer2().username().equals(username);

        // Instantly tell the joining session who Player 1 and Player 2 are so the frontend knows if they are a spectator
        List<Player> players = new ArrayList<>(List.of(gameRoom.getPlayer1(), gameRoom.getPlayer2()));
        session.sendMessage(new TextMessage("[PLAYER_INFO]:" + objectMapper.writeValueAsString(players)));

        // If catching up a spectator mid-game
        if (!isNewRoom) {
            if (gameRoom.getBootStage() > 0) {
                session.sendMessage(new TextMessage("[START_GAME]"));
                if (gameRoom.getUsernameTurn() != null) {
                    session.sendMessage(new TextMessage("[STARTING_PLAYER]≔" + gameRoom.getUsernameTurn()));
                }
            }
            if (gameRoom.getBoardState() != null) {
                String boardStateJson = getBoardStateJson(gameRoom);
                session.sendMessage(new TextMessage("[DISTRIBUTE_CARDS]:" + boardStateJson));
                
                String[] chatHistory = gameRoom.getChat();
                if (chatHistory != null && chatHistory.length > 0) {
                    String[] reversedChatHistory = new String[chatHistory.length];
                    for (int i = 0; i < chatHistory.length; i++) {
                        reversedChatHistory[i] = chatHistory[chatHistory.length - 1 - i];
                    }
                    session.sendMessage(new TextMessage("[CHAT_HISTORY]:" + objectMapper.writeValueAsString(reversedChatHistory)));
                } else {
                    session.sendMessage(new TextMessage("[CHAT_HISTORY]:[]"));
                }
                
                session.sendMessage(new TextMessage("[SET_BOOT_STAGE]:" + gameRoom.getBootStage()));
                session.sendMessage(new TextMessage("[SET_PHASE]:" + gameRoom.getPhase()));
                session.sendMessage(new TextMessage("[SET_TURN]:" + gameRoom.getUsernameTurn()));
            }
            
            // Only announce a reconnect if an actual player came back
            if (isPlayer && gameRoom.getBoardState() != null) {
                gameRoom.sendMessageToOtherSessions(session, "[OPPONENT_RECONNECTED]");
            }
        }

        // Only start the game when BOTH Player 1 and Player 2 are fully connected
        if (gameRoom.getBootStage() == 0 && gameRoom.areBothPlayersConnected()) {
            try {
                gameRoom.initiateGame();
                gameRoom.setStartingPlayer(gameRoom.getRandomPlayer().username());
                scheduleCardDistribution(gameRoom);
            } catch (Exception e) {
                System.err.println("Error in initial game setup for room " + gameRoom.getRoomId() + ": " + e.getMessage());
            }
        }
    }

    private String getBoardStateJson(GameRoom gameRoom) throws JsonProcessingException {
        BoardState boardState = gameRoom.getBoardState();
        if (boardState == null) return "{}";

        Map<String, Object> completeBoardState = new HashMap<>();
        completeBoardState.put("player1Hand", boardState.getPlayer1Hand());
        completeBoardState.put("player1Deck", boardState.getPlayer1Deck());
        completeBoardState.put("player1EggDeck", boardState.getPlayer1EggDeck());
        completeBoardState.put("player1Security", boardState.getPlayer1Security());
        completeBoardState.put("player1Trash", boardState.getPlayer1Trash());
        completeBoardState.put("player1Reveal", boardState.getPlayer1Reveal());
        completeBoardState.put("player1BreedingArea", boardState.getPlayer1BreedingArea());
        
        for (int i = 1; i <= 21; i++) {
            completeBoardState.put("player1Digi" + i, boardState.getFieldByName("player1Digi" + i));
        }
        for (int i = 1; i <= 16; i++) {
            completeBoardState.put("player1Link" + i, boardState.getFieldByName("player1Link" + i));
        }
        
        completeBoardState.put("player2Hand", boardState.getPlayer2Hand());
        completeBoardState.put("player2Deck", boardState.getPlayer2Deck());
        completeBoardState.put("player2EggDeck", boardState.getPlayer2EggDeck());
        completeBoardState.put("player2Security", boardState.getPlayer2Security());
        completeBoardState.put("player2Trash", boardState.getPlayer2Trash());
        completeBoardState.put("player2Reveal", boardState.getPlayer2Reveal());
        completeBoardState.put("player2BreedingArea", boardState.getPlayer2BreedingArea());
        
        for (int i = 1; i <= 21; i++) {
            completeBoardState.put("player2Digi" + i, boardState.getFieldByName("player2Digi" + i));
        }
        for (int i = 1; i <= 16; i++) {
            completeBoardState.put("player2Link" + i, boardState.getFieldByName("player2Link" + i));
        }

        completeBoardState.put("player1Memory", boardState.getPlayer1Memory());
        completeBoardState.put("player2Memory", boardState.getPlayer2Memory());

        return objectMapper.writeValueAsString(completeBoardState);
    }

    private void handleAttack(GameRoom gameRoom, WebSocketSession session, String message) {
        if (!gameRoom.hasFullConnection() || message.split(":").length < 4) return;
        String[] parts = message.split(":", 4);
        String from = parts[1];
        String to = parts[2];
        String isEffect = parts[3];

        String username = Objects.requireNonNull(session.getPrincipal()).getName();
        String fromServer = mapClientToServer(from, username, gameRoom);
        String toServer = mapClientToServer(to, username, gameRoom);

        for (WebSocketSession s : gameRoom.getSessions()) {
            if (!s.isOpen()) continue;
            if (s.getId().equals(session.getId())) continue;

            String destUsername = s.getPrincipal() != null ? s.getPrincipal().getName() : "";
            String relativeFrom = mapServerToClient(fromServer, destUsername, gameRoom);
            String relativeTo = mapServerToClient(toServer, destUsername, gameRoom);

            try {
                s.sendMessage(new TextMessage("[ATTACK]:" + relativeFrom + ":" + relativeTo + ":" + isEffect));
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void handleSendMoveCard(GameRoom gameRoom, WebSocketSession session, String roomMessage) {
        if (!gameRoom.hasFullConnection() || roomMessage.split(":").length < 4) return;
        String[] parts = roomMessage.split(":", 4);
        String cardId = parts[1];
        String from = parts[2];
        String to = parts[3];
        String currentPlayer = session.getPrincipal() != null ? session.getPrincipal().getName() : null;
        if (currentPlayer != null) {
            updateBoardStateForCardMove(gameRoom, cardId, from, to, currentPlayer);
            String fromServer = mapClientToServer(from, currentPlayer, gameRoom);
            String toServer = mapClientToServer(to, currentPlayer, gameRoom);
            broadcastGameAction(gameRoom, session, "MOVE_CARD", cardId, fromServer, toServer);
        }
    }

    private void handleSendSetModifiers(GameRoom gameRoom, WebSocketSession session, String roomMessage) {
        if (!gameRoom.hasFullConnection() || roomMessage.split(":").length < 5) return;
        String[] parts = roomMessage.split(":");
        String cardId = parts[2];
        String location = parts[3];
        String modifiersJson = String.join(":", Arrays.copyOfRange(parts, 4, parts.length));

        String currentPlayer = session.getPrincipal() != null ? session.getPrincipal().getName() : null;
        if (currentPlayer != null) {
            updateCardModifiers(session, gameRoom, cardId, location, modifiersJson);
            String locationServer = mapClientToServer(location, currentPlayer, gameRoom);
            broadcastGameAction(gameRoom, session, "SET_MODIFIERS", cardId, null, locationServer, modifiersJson);
        }
    }

    private void handleSendMoveToStack(GameRoom gameRoom, WebSocketSession session, String roomMessage) {
        if (!gameRoom.hasFullConnection() || roomMessage.split(":").length < 6) return;
        String[] parts = roomMessage.split(":", 6);
        String topOrBottom = parts[1];
        String cardId = parts[2];
        String from = parts[3];
        String to = parts[4];
        String facing = parts[5];
        String currentPlayer = session.getPrincipal() != null ? session.getPrincipal().getName() : null;
        if (currentPlayer != null) {
            updateBoardStateForStackMove(gameRoom, cardId, from, to, topOrBottom, facing, currentPlayer);
            String fromServer = mapClientToServer(from, currentPlayer, gameRoom);
            String toServer = mapClientToServer(to, currentPlayer, gameRoom);
            broadcastGameAction(gameRoom, session, "MOVE_CARD_TO_STACK", cardId, fromServer, toServer, topOrBottom, facing);
        }
    }

    private void handleTiltCard(GameRoom gameRoom, WebSocketSession session, String roomMessage) {
        if (!gameRoom.hasFullConnection() || roomMessage.split(":").length < 4) return;
        String[] parts = roomMessage.split(":", 4);
        String cardId = parts[2];
        String location = parts[3];

        String currentPlayer = session.getPrincipal() != null ? session.getPrincipal().getName() : null;
        if (currentPlayer != null) {
            updateCardTiltStatus(session, gameRoom, cardId, location);
            String locationServer = mapClientToServer(location, currentPlayer, gameRoom);
            broadcastGameAction(gameRoom, session, "TILT_CARD", cardId, null, locationServer);
        }
    }

    private void handleFlipCard(GameRoom gameRoom, WebSocketSession session, String roomMessage) {
        if (!gameRoom.hasFullConnection() || roomMessage.split(":").length < 3) return;
        String[] parts = roomMessage.split(":", 3);
        String cardId = parts[1];
        String location = parts[2];

        String currentPlayer = session.getPrincipal() != null ? session.getPrincipal().getName() : null;
        if (currentPlayer != null) {
            updateCardFaceStatus(session, gameRoom, cardId, location);
            String locationServer = mapClientToServer(location, currentPlayer, gameRoom);
            broadcastGameAction(gameRoom, session, "FLIP_CARD", cardId, null, locationServer);
        }
    }

    private void updateCardTiltStatus(WebSocketSession session, GameRoom gameRoom, String cardId, String location) {
        BoardState boardState = gameRoom.getBoardState();
        if (boardState == null) return;
        String username = session.getPrincipal() != null ? session.getPrincipal().getName() : null;
        if (username == null) return;

        String serverLocation = mapClientToServer(location, username, gameRoom);
        List<GameCard> cards = boardState.getFieldByName(serverLocation);
        cards.stream().filter(c -> c.getId().toString().equals(cardId)).findFirst().ifPresent(GameCard::tilt);
        boardState.setFieldByName(serverLocation, cards);
    }

    private void updateCardFaceStatus(WebSocketSession session, GameRoom gameRoom, String cardId, String location) {
        BoardState boardState = gameRoom.getBoardState();
        if (boardState == null) return;
        String username = session.getPrincipal() != null ? session.getPrincipal().getName() : null;
        if (username == null) return;

        String serverLocation = mapClientToServer(location, username, gameRoom);
        List<GameCard> cards = boardState.getFieldByName(serverLocation);
        cards.stream().filter(c -> c.getId().toString().equals(cardId)).findFirst().ifPresent(GameCard::flip);
        boardState.setFieldByName(serverLocation, cards);
    }

    private void updateCardModifiers(WebSocketSession session, GameRoom gameRoom, String cardId, String location, String modifiersJson) {
        BoardState boardState = gameRoom.getBoardState();
        if (boardState == null) return;
        String username = session.getPrincipal() != null ? session.getPrincipal().getName() : null;
        if (username == null) return;

        String serverLocation = mapClientToServer(location, username, gameRoom);
        try {
            Modifiers newModifiers = objectMapper.readValue(modifiersJson, Modifiers.class);
            List<GameCard> cards = boardState.getFieldByName(serverLocation);
            cards.stream().filter(c -> c.getId().toString().equals(cardId)).findFirst().ifPresent(c -> c.setModifiers(newModifiers));
            boardState.setFieldByName(serverLocation, cards);
        } catch (Exception e) {
            // ignore
        }
    }

    private void handleUnsuspendAll(GameRoom gameRoom, WebSocketSession session) {
        if (!gameRoom.hasFullConnection()) return;
        unsuspendAllCardsInBoardState(gameRoom, session);
        gameRoom.sendMessageToOtherSessions(session, "[UNSUSPEND_ALL]");
    }
    
    private void unsuspendAllCardsInBoardState(GameRoom gameRoom, WebSocketSession session) {
        BoardState boardState = gameRoom.getBoardState();
        if (boardState == null) return;
        String username = session.getPrincipal() != null ? session.getPrincipal().getName() : null;
        if (username == null) return;

        boolean isPlayer1 = gameRoom.getPlayer1().username().equals(username);
        for (int i = 1; i <= 21; i++) {
            String digiPosition = isPlayer1 ? "player1Digi" + i : "player2Digi" + i;
            List<GameCard> cards = boardState.getFieldByName(digiPosition);
            cards.stream().filter(c -> c.isTilted).forEach(GameCard::tilt);
            boardState.setFieldByName(digiPosition, cards);
        }
    }

    private void handleCreateToken(GameRoom gameRoom, WebSocketSession session, String roomMessage) {
        if (!gameRoom.hasFullConnection()) return;
        String[] parts = roomMessage.split(":", 3);
        String targetPosition = parts[1];
        String cardJson = parts[2];
        String currentPlayer = session.getPrincipal() != null ? session.getPrincipal().getName() : null;
        if (currentPlayer != null) {
            try {
                GameCard card = cardJsonConverter.convertToGameCard(cardJson);
                BoardState boardState = gameRoom.getBoardState();
                if (boardState != null) {
                    String serverPosition = mapClientToServer(targetPosition, currentPlayer, gameRoom);
                    List<GameCard> currentList = boardState.getFieldByName(serverPosition);
                    currentList.add(card);
                    boardState.setFieldByName(serverPosition, currentList);
                    
                    broadcastGameAction(gameRoom, session, "CREATE_TOKEN", card.getId().toString(), null, serverPosition, card.getName());
                }
            } catch (Exception e) {
                System.err.println("ERROR in handleCreateToken: " + e.getMessage());
            }
        }
    }

    private void handleMemoryUpdate(GameRoom gameRoom, WebSocketSession session, String roomMessage) {
        if (!gameRoom.hasFullConnection() || roomMessage.split(":").length < 2) return;
        String[] parts = roomMessage.split(":", 2);
        int memory = Integer.parseInt(parts[1]) * -1;
        BoardState boardState = gameRoom.getBoardState();
        if (boardState != null) {
            String currentPlayer = session.getPrincipal() != null ? session.getPrincipal().getName() : null;
            if (currentPlayer != null) {
                boolean isPlayer1 = gameRoom.getPlayer1().username().equals(currentPlayer);
                int newMemory = Integer.parseInt(parts[1]);
                if (isPlayer1) {
                    boardState.setPlayer1Memory(newMemory);
                    boardState.setPlayer2Memory(-1 * newMemory);
                } else {
                    boardState.setPlayer1Memory(-1 * newMemory);
                    boardState.setPlayer2Memory(newMemory);
                }
            }
        }
        gameRoom.sendMessageToOtherSessions(session, "[UPDATE_MEMORY]:" + memory);
    }

    private void handleCommandWithId(GameRoom gameRoom, WebSocketSession session, String roomMessage) {
        if (!gameRoom.hasFullConnection() || roomMessage.split(":").length < 2) return;
        String[] parts = roomMessage.split(":", 2);
        String command = parts[0];
        String id = parts.length > 1 ? parts[1] : "";
        gameRoom.sendMessageToOtherSessions(session, convertCommand(command) + ":" + id);
    }
    
    public void broadcastServerMessageToAllGameRooms(String message) {
        String formattedMessage = "[CHAT_MESSAGE]:【SERVER】﹕" + message;
        for (GameRoom gameRoom : gameRooms.values()) {
            gameRoom.sendMessagesToAll(formattedMessage);
        }
    }
    
    private void startGameRoomScheduledTasks(GameRoom gameRoom) {
        ScheduledFuture<?> heartbeatTask = SHARED_SCHEDULER.scheduleWithFixedDelay(() -> {
            if (!gameRooms.containsKey(gameRoom.getRoomId()) || gameRoom.isEmpty()) return;
            try {
                gameRoom.sendMessagesToAll("[HEARTBEAT]");
            } catch (Exception e) {
                System.err.println("Error in heartbeat for room " + gameRoom.getRoomId() + ": " + e.getMessage());
            }
        }, 10, 10, TimeUnit.SECONDS);
        gameRoom.getScheduledTasks().add(heartbeatTask);

        ScheduledFuture<?> cleanupTask = SHARED_SCHEDULER.scheduleWithFixedDelay(() -> {
            if (!gameRooms.containsKey(gameRoom.getRoomId())) return;
            try {
                gameRoom.getSessions().removeIf(s -> !s.isOpen()); // Prune dead connections
                if (gameRoom.isEmpty()) {
                    GameRoom removed = gameRooms.remove(gameRoom.getRoomId());
                    if (removed != null) removed.cancelAllScheduledTasks();
                }
            } catch (Exception e) {
                System.err.println("Error in empty check for room " + gameRoom.getRoomId() + ": " + e.getMessage());
            }
        }, 15, 15, TimeUnit.SECONDS);
        gameRoom.getScheduledTasks().add(cleanupTask);
    }
    
    private void scheduleCardDistribution(GameRoom gameRoom) {
        SHARED_SCHEDULER.schedule(() -> {
            try {
                GameStart newGame = gameRoom.initiallyDistributeCards();
                gameRoom.distributeCardsAndSetStage(newGame);
            } catch (Exception e) {
                System.err.println("Error in scheduleCardDistribution for room " + gameRoom.getRoomId() + ": " + e.getMessage());
            }
        }, 4500, TimeUnit.MILLISECONDS);
    }
}