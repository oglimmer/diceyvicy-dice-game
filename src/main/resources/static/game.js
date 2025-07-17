class DiceGame {
    constructor() {
        this.gameId = null;
        this.playerName = null;
        this.stompClient = null;
        this.gameState = null;
        this.isPlayerTurn = false;
        
        this.init();
    }
    
    init() {
        this.setupEventListeners();
        this.showStartScreen();
    }
    
    setupEventListeners() {
        document.getElementById('startButton').addEventListener('click', () => this.startGame());
        document.getElementById('playerName').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.startGame();
            }
        });
        
        document.getElementById('rollButton').addEventListener('click', () => this.rollDice());
        
        document.querySelectorAll('.book-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const bookingType = e.target.closest('.score-row').dataset.booking;
                this.bookDice(bookingType);
            });
        });
        
        document.querySelectorAll('.dice').forEach(dice => {
            dice.addEventListener('click', (e) => {
                const checkbox = dice.querySelector('.dice-keep');
                if (!checkbox.disabled) {
                    checkbox.checked = !checkbox.checked;
                    this.updateDiceSelection(dice, checkbox.checked);
                }
            });
        });
        
        document.getElementById('newGameButton').addEventListener('click', () => this.showStartScreen());
    }
    
    showStartScreen() {
        document.getElementById('start-screen').style.display = 'flex';
        document.getElementById('game-screen').style.display = 'none';
        document.getElementById('game-over-screen').style.display = 'none';
        document.getElementById('playerName').value = '';
        document.getElementById('playerName').focus();
    }
    
    showGameScreen() {
        document.getElementById('start-screen').style.display = 'none';
        document.getElementById('game-screen').style.display = 'block';
        document.getElementById('game-over-screen').style.display = 'none';
    }
    
    showGameOverScreen() {
        document.getElementById('start-screen').style.display = 'none';
        document.getElementById('game-screen').style.display = 'none';
        document.getElementById('game-over-screen').style.display = 'flex';
    }
    
    async startGame() {
        const playerNameInput = document.getElementById('playerName');
        const playerName = playerNameInput.value.trim();
        
        if (!playerName) {
            alert('Please enter your name');
            playerNameInput.focus();
            return;
        }
        
        this.playerName = playerName;
        document.getElementById('startButton').disabled = true;
        document.getElementById('startButton').textContent = 'Starting...';
        
        try {
            const response = await fetch('/api/game/start', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ playerName: playerName })
            });
            
            if (!response.ok) {
                throw new Error('Failed to start game');
            }
            
            const gameData = await response.json();
            this.gameId = gameData.gameId;
            this.gameState = gameData;
            
            this.showGameScreen();
            this.connectWebSocket();
            this.updateGameUI();
            
        } catch (error) {
            console.error('Error starting game:', error);
            alert('Failed to start game. Please try again.');
            document.getElementById('startButton').disabled = false;
            document.getElementById('startButton').textContent = 'Start Game';
        }
    }
    
    connectWebSocket() {
        const socket = new SockJS('/game-websocket');
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({}, (frame) => {
            console.log('Connected to WebSocket');
            
            this.stompClient.subscribe(`/topic/game/${this.gameId}`, (message) => {
                const gameState = JSON.parse(message.body);
                this.gameState = gameState;
                this.updateGameUI();
            });
        }, (error) => {
            console.error('WebSocket connection error:', error);
            alert('Connection error. Please refresh the page.');
        });
    }
    
    rollDice() {
        if (!this.isPlayerTurn || this.gameState.rollCount >= 3) {
            return;
        }
        
        const diceToKeep = [];
        document.querySelectorAll('.dice-keep').forEach((checkbox, index) => {
            if (checkbox.checked) {
                diceToKeep.push(index + 1);
            }
        });
        
        this.stompClient.send(`/app/game/${this.gameId}/reroll`, {}, JSON.stringify({
            diceToKeep: diceToKeep
        }));
        
        document.querySelectorAll('.dice-keep').forEach((checkbox, index) => {
            checkbox.checked = false;
            const dice = document.querySelector(`[data-index="${index}"]`);
            this.updateDiceSelection(dice, false);
        });
    }
    
    bookDice(bookingType) {
        if (!this.isPlayerTurn) {
            return;
        }
        
        this.stompClient.send(`/app/game/${this.gameId}/book`, {}, JSON.stringify({
            bookingType: bookingType
        }));
    }
    
    updateGameUI() {
        if (!this.gameState) return;
        
        document.getElementById('currentPlayer').textContent = this.gameState.currentPlayer;
        document.getElementById('rollsLeft').textContent = "" + (3 - this.gameState.rollCount);
        
        this.isPlayerTurn = this.gameState.currentPlayer === this.playerName;
        
        this.updateDice();
        this.updateScorecard();
        this.updatePlayerScores();
        this.updateAiActionDisplay();
        
        const rollButton = document.getElementById('rollButton');
        rollButton.disabled = !this.isPlayerTurn || this.gameState.rollCount >= 3;
        
        document.querySelectorAll('.dice-keep').forEach(checkbox => {
            checkbox.disabled = !this.isPlayerTurn || this.gameState.rollCount === 0;
        });
        
        document.querySelectorAll('.dice').forEach(dice => {
            const checkbox = dice.querySelector('.dice-keep');
            dice.style.cursor = (!this.isPlayerTurn || this.gameState.rollCount === 0) ? 'not-allowed' : 'pointer';
        });
        
        this.updateBookingButtons();
        
        if (this.gameState.gameOver) {
            this.showGameOver();
        }
    }
    
    updateDice() {
        const diceRolls = this.gameState.diceRolls || [1, 1, 1, 1, 1];
        
        diceRolls.forEach((value, index) => {
            const diceElement = document.querySelector(`[data-index="${index}"] .dice-face`);
            if (diceElement) {
                diceElement.textContent = value;
            }
        });
    }
    
    updateScorecard() {
        const playerData = this.gameState.players[this.playerName];
        
        if (!playerData) return;
        
        document.getElementById('player-name').textContent = this.playerName;
        
        document.querySelectorAll('.score-row').forEach(row => {
            const bookingType = row.dataset.booking;
            const isUsed = playerData.usedBookingTypes.includes(bookingType);
            
            if (isUsed) {
                row.classList.add('used');
            } else {
                row.classList.remove('used');
            }
        });
    }
    
    updatePlayerScores() {
        const playerData = this.gameState.players[this.playerName];
        const aiData = this.gameState.players['Jürgen-AI'];
        
        if (playerData) {
            document.getElementById('player-total').textContent = `Total: ${playerData.score}`;
            
            const playerScoreElement = document.querySelector('.player-score:first-child');
            if (this.isPlayerTurn) {
                playerScoreElement.classList.add('current-player');
            } else {
                playerScoreElement.classList.remove('current-player');
            }
        }
        
        if (aiData) {
            document.getElementById('ai-total').textContent = `Total: ${aiData.score}`;
            
            const aiScoreElement = document.querySelector('.player-score:last-child');
            if (!this.isPlayerTurn) {
                aiScoreElement.classList.add('current-player');
            } else {
                aiScoreElement.classList.remove('current-player');
            }
        }
    }
    
    updateAiActionDisplay() {
        const aiActionDisplay = document.getElementById('ai-action-display');
        const aiActionText = document.getElementById('ai-action-text');
        
        if (this.gameState.aiAction) {
            aiActionText.textContent = this.gameState.aiAction;
            aiActionDisplay.style.display = 'block';

            // setTimeout(() => {
            //     aiActionDisplay.style.display = 'none';
            // }, 4000);
        } else {
            aiActionDisplay.style.display = 'none';
        }
    }
    
    updateBookingButtons() {
        const playerData = this.gameState.players[this.playerName];
        if (!playerData) return;
        
        document.querySelectorAll('.book-btn').forEach(btn => {
            const bookingType = btn.closest('.score-row').dataset.booking;
            const isUsed = playerData.usedBookingTypes.includes(bookingType);
            const canBook = this.isPlayerTurn && this.gameState.rollCount > 0;
            
            btn.disabled = !canBook || isUsed;
            
            if (isUsed) {
                btn.textContent = 'Used';
            } else {
                btn.textContent = 'Book';
            }
        });
        
        if (this.isPlayerTurn && this.gameState.rollCount > 0) {
            this.calculatePotentialScores();
        }
    }
    
    calculatePotentialScores() {
        const dice = this.gameState.diceRolls || [1, 1, 1, 1, 1];
        const playerData = this.gameState.players[this.playerName];
        
        if (!playerData) return;
        
        const scores = this.calculateScores(dice);
        
        document.querySelectorAll('.score-row').forEach(row => {
            const bookingType = row.dataset.booking;
            const scoreElement = row.querySelector('.score-value');
            const isUsed = playerData.usedBookingTypes.includes(bookingType);
            
            if (isUsed) {
                scoreElement.textContent = 'Used';
            } else {
                const score = scores[bookingType] || 0;
                scoreElement.textContent = score;
            }
        });
    }
    
    calculateScores(dice) {
        const counts = {};
        dice.forEach(die => {
            counts[die] = (counts[die] || 0) + 1;
        });
        
        const scores = {};
        
        for (let i = 1; i <= 6; i++) {
            const bookingType = ['ONES', 'TWOS', 'THREES', 'FOURS', 'FIVES', 'SIXES'][i - 1];
            scores[bookingType] = (counts[i] || 0) * i;
        }
        
        const sortedCounts = Object.values(counts).sort((a, b) => b - a);
        const sum = dice.reduce((a, b) => a + b, 0);
        
        scores.THREE_OF_A_KIND = sortedCounts[0] >= 3 ? sum : 0;
        scores.FOUR_OF_A_KIND = sortedCounts[0] >= 4 ? sum : 0;
        scores.FULL_HOUSE = (sortedCounts[0] === 3 && sortedCounts[1] === 2) ? 25 : 0;
        
        const uniqueDice = [...new Set(dice)].sort((a, b) => a - b);
        const isSmallStraight = this.containsSequence(uniqueDice, 4);
        const isLargeStraight = this.containsSequence(uniqueDice, 5);
        
        scores.SMALL_STRAIGHT = isSmallStraight ? 30 : 0;
        scores.LARGE_STRAIGHT = isLargeStraight ? 40 : 0;
        scores.KNIFFEL = sortedCounts[0] === 5 ? 50 : 0;
        scores.CHANCE = sum;
        
        return scores;
    }
    
    containsSequence(sortedArray, length) {
        let consecutive = 1;
        for (let i = 1; i < sortedArray.length; i++) {
            if (sortedArray[i] === sortedArray[i - 1] + 1) {
                consecutive++;
                if (consecutive >= length) {
                    return true;
                }
            } else {
                consecutive = 1;
            }
        }
        return false;
    }
    
    updateDiceSelection(dice, isSelected) {
        if (isSelected) {
            dice.classList.add('selected');
        } else {
            dice.classList.remove('selected');
        }
    }
    
    showGameOver() {
        const playerData = this.gameState.players[this.playerName];
        const aiData = this.gameState.players['Jürgen-AI'];
        
        let finalScoresHtml = '<h3>Final Scores</h3>';
        
        if (playerData && aiData) {
            const playerWon = playerData.score > aiData.score;
            const tie = playerData.score === aiData.score;
            
            finalScoresHtml += `
                <div class="final-score-row">
                    <span>${this.playerName}:</span>
                    <span>${playerData.score}</span>
                </div>
                <div class="final-score-row">
                    <span>Jürgen-AI:</span>
                    <span>${aiData.score}</span>
                </div>
                <div class="winner">
                    ${tie ? "It's a tie!" : (playerWon ? "You win!" : "Jürgen-AI wins!")}
                </div>
            `;
        }
        
        document.getElementById('final-scores').innerHTML = finalScoresHtml;
        
        setTimeout(() => {
            this.showGameOverScreen();
        }, 2000);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new DiceGame();
});