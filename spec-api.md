# Diceyvicy Game API Specification

## Overview
This API provides a real-time Diceyvicy game between a human player and an AI opponent ("Jürgen-AI"). The game uses REST for initial setup and WebSocket for real-time gameplay.

## REST API

### Start Game
**POST** `/api/game/start`

Start a new game with a human player and AI opponent.

**Request:**
```json
{
  "playerName": "string"
}
```

**Response:**
```json
{
  "gameId": "string",
  "diceRolls": [1, 2, 3, 4, 5],
  "currentPlayer": "string",
  "rollCount": 0,
  "gameOver": false,
  "players": {
    "PlayerName": {
      "name": "string",
      "score": 0,
      "usedBookingTypes": []
    },
    "Jürgen-AI": {
      "name": "string", 
      "score": 0,
      "usedBookingTypes": []
    }
  }
}
```

## WebSocket API

### Connection
**Endpoint:** `/game-websocket`
**Protocol:** STOMP over SockJS

### Subscribe to Game Updates
**Topic:** `/topic/game/{gameId}`

Receives real-time game state updates.

### Player Actions

#### Reroll Dice
**Destination:** `/app/game/{gameId}/reroll`

**Message:**
```json
{
  "diceToKeep": [1, 3, 5]
}
```

#### Book Dice Roll
**Destination:** `/app/game/{gameId}/book`

**Message:**
```json
{
  "bookingType": "FULL_HOUSE"
}
```

### Game State Updates

All WebSocket actions trigger a game state broadcast to `/topic/game/{gameId}`:

```json
{
  "gameId": "string",
  "diceRolls": [1, 2, 3, 4, 5],
  "currentPlayer": "string",
  "rollCount": 0,
  "gameOver": false,
  "players": {
    "PlayerName": {
      "name": "string",
      "score": 120,
      "usedBookingTypes": ["ONES", "TWOS", "FULL_HOUSE"]
    },
    "Jürgen-AI": {
      "name": "string",
      "score": 95,
      "usedBookingTypes": ["THREES", "FOURS", "STRAIGHT"]
    }
  }
}
```

## Booking Types
Available booking types (enum values):
- `ONES`, `TWOS`, `THREES`, `FOURS`, `FIVES`, `SIXES`
- `THREE_OF_A_KIND`, `FOUR_OF_A_KIND`, `FULL_HOUSE`
- `SMALL_STRAIGHT`, `LARGE_STRAIGHT`, `KNIFFEL`, `CHANCE`

## Game Flow
1. **Start Game:** POST to `/api/game/start`
2. **Connect:** WebSocket to `/game-websocket`
3. **Subscribe:** To `/topic/game/{gameId}` for updates
4. **Play:** Send reroll/book messages to `/app/game/{gameId}/...`
5. **AI Turn:** Server automatically handles AI actions after player moves
6. **Updates:** Receive real-time game state via WebSocket subscription

## Error Handling
- Invalid moves (wrong turn, already used booking type) are logged server-side
- Game not found returns no response
- WebSocket connection issues should trigger reconnection