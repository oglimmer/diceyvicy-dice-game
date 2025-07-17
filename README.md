# Diceyvicy - A Yahtzee Game with AI Bot

Diceyvicy is a web-based implementation of the classic Yahtzee dice game, featuring an AI opponent powered by OpenAI's GPT-3.5 model with custom fine-tuning for strategic gameplay.

## Architecture

### Backend (Spring Boot)
- **Technology**: Java 21 with Spring Boot 3.5.3
- **Main Components**:
  - `GameApplication.java` - Spring Boot application entry point
  - `GameService.java` - Core game logic and state management
  - `GameController.java` - REST API endpoints
  - `GameWebSocketController.java` - WebSocket communication for real-time updates
  - `AiBot.java` - AI opponent using OpenAI GPT-3.5 with fine-tuning
  - `WebSocketConfig.java` - WebSocket configuration

### Frontend (Embedded Static Web App)
- **Technology**: Vanilla JavaScript with HTML5 and CSS3
- **Files**:
  - `index.html` - Main game interface with start screen, game board, and scorecard
  - `game.js` - Client-side game logic and WebSocket communication
  - `style.css` - Game styling and responsive design
- **Features**:
  - Interactive dice rolling and selection
  - Real-time scorecard updates
  - AI opponent actions display
  - Game state synchronization via WebSockets

### Dependencies
- **Game Logic**: Uses `kniffel-rules-lib` (v0.0.4) for Yahtzee/Kniffel rule implementation
- **AI Integration**: OpenAI Java SDK (v2.13.1) for GPT-3.5 integration
- **WebSocket**: Spring Boot WebSocket support for real-time communication
- **Frontend Libraries**: SockJS and STOMP.js for WebSocket client communication

## AI Fine-Tuning

The AI opponent ("Jürgen-AI") uses a custom fine-tuned GPT-3.5 model specifically trained for Yahtzee strategy.

### Fine-Tuning Process
Located in `fine-tune-openai/` directory:

1. **Training Data**: `yahtzee_training_data.jsonl` contains Q&A pairs about Yahtzee rules and strategies
2. **Training Script**: `main.py` handles the OpenAI fine-tuning process:
   - Uploads training data to OpenAI
   - Creates fine-tuning job with GPT-3.5-turbo base model
   - Monitors job status until completion
   - Returns fine-tuned model ID

### AI Implementation Details
The AI makes two types of decisions:

1. **Dice Selection** (`askAiWhichDiceToKeep`):
   - Analyzes current dice roll and available booking types
   - Decides which dice to keep for optimal scoring
   - Returns structured JSON response with dice selection and reasoning

2. **Booking Selection** (`askAiBookingType`):
   - Evaluates final dice roll against available scorecard categories
   - Selects optimal booking type to maximize score
   - Returns structured JSON response with booking choice and reasoning

### Fine-Tuned Model
- **Model**: `ft:gpt-3.5-turbo-0125:personal::BuP2JgWv`
- **Temperature**: 0.1 (low for consistent strategic decisions)
- **Max Tokens**: 200 (sufficient for structured responses)

## Getting Started

### Prerequisites
- Java 21
- Maven 3.x
- OpenAI API key (for AI opponent)

### Running the Application
1. Set OpenAI API key:
   ```bash
   export OPENAI_API_KEY="your-api-key-here"
   ```

2. Build and run:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

3. Open browser to `http://localhost:8080`

### Fine-Tuning the AI (Optional)
To create your own fine-tuned model:

1. Navigate to fine-tuning directory:
   ```bash
   cd fine-tune-openai/
   ```

2. Install dependencies:
   ```bash
   pip install openai
   ```

3. Run fine-tuning script:
   ```bash
   python main.py
   ```

4. Update the model ID in `AiBot.java` (lines 64 and 138)

## Deployment

The application includes Docker and Kubernetes (Helm) deployment configurations:
- `Dockerfile` - Container configuration
- `helm/` - Kubernetes deployment charts
- `sealedsecret.yaml` - Secret management for production

## Game Features

- **Classic Yahtzee Rules**: 13 rounds with standard scoring categories
- **AI Opponent**: Strategic AI player with reasoning displayed
- **Real-time Updates**: WebSocket-based communication
- **Responsive Design**: Works on desktop and mobile devices
- **Score Tracking**: Full scorecard with upper/lower section bonuses

## Technical Highlights

- **Embedded Frontend**: Static files served directly from Spring Boot
- **WebSocket Communication**: Real-time game state synchronization
- **AI Integration**: Custom fine-tuned model for strategic gameplay
- **Structured AI Responses**: JSON-based AI communication with error handling
- **Rule Engine**: External library for game logic validation