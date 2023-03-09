package andrew.a5.logic;

import andrew.a5.util.PlayerRole;
import andrew.a5.util.GameType;
import andrew.a5.util.GameResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * A Pente game, where players take turns to place stones on board.
 * When consecutive two stones are surrounded by the opponent's stones on two ends,
 * these two stones are removed (captured).
 * A player wins by placing 5 consecutive stones or capturing stones 5 times.
 */
public class Pente extends MNKGame {

    /**
     * Record how many times each player captured.
     */
    final Map<PlayerRole, Integer> capturedPairsNos;

    /**
     * Create an 8-by-8 Pente game.
     */
    public Pente() {
        super(8, 8, 5);

        capturedPairsNos = new HashMap<>();
        capturedPairsNos.put(PlayerRole.FIRST_PLAYER, 0);
        capturedPairsNos.put(PlayerRole.SECOND_PLAYER, 0);
    }

    /**
     * Creates: a copy of the game state.
     */
    public Pente(Pente game) {
        super(game);
        capturedPairsNos = new HashMap<>(game.capturedPairsNos);
    }

    @Override
    public boolean makeMove(Position p) {
        if (!board().validPos(p)) {
            return false;
        }
        board().place(p, currentPlayer());

        // see if this move captures pieces

        int[][] steps = {
                {+1, +1}, {+1, 0}, {+1, -1}, {0, +1},
                {-1, -1}, {-1, 0}, {-1, +1}, {0, -1}
        };
        for (int[] step : steps) {
            boolean success = true;
            Position[] nextPs = new Position[4];
            nextPs[0] = p;
            for (int i = 1; i < 4; ++i) {
                nextPs[i] = new Position(nextPs[i - 1].row() + step[0],
                        nextPs[i - 1].col() + step[1]);
                if (!board().onBoard(nextPs[i])) {
                    success = false;
                    break;
                }
            }
            if (success) {
                if (!(board().get(nextPs[0]) == board().get(nextPs[3]) &&
                        board().get(nextPs[1]) == board().get(nextPs[2]) &&
                        board().get(nextPs[1]) == currentPlayer().nextPlayer().boardValue())) {
                    success = false;
                }
            }
            if (success) {
                board().erase(nextPs[1]);
                board().erase(nextPs[2]);
                capturedPairsNos.put(currentPlayer(), capturedPairsNo(currentPlayer()) + 1);
            }
        }

        changePlayer();
        advanceTurn();
        return true;
    }

    /**
     * Returns: a new game state representing the state of the game after the current player takes a
     * move {@code p}.
     */
    public Pente applyMove(Position p) {
        Pente newGame = new Pente(this);
        newGame.makeMove(p);
        return newGame;
    }

    /**
     * Returns: the number of captured pairs by {@code playerRole}.
     */
    public int capturedPairsNo(PlayerRole playerRole) {
        return capturedPairsNos.get(playerRole);
    }

    @Override
    public boolean hasEnded() {
        if (capturedPairsNo(PlayerRole.FIRST_PLAYER) >= 5) {
            setResult(GameResult.FIRST_PLAYER_WON);
            return true;
        }

        if (capturedPairsNo(PlayerRole.SECOND_PLAYER) >= 5) {
            setResult(GameResult.SECOND_PLAYER_WON);
            return true;
        }
        return super.hasEnded();
    }

    @Override
    public GameType gameType() {
        return GameType.PENTE;
    }


    @Override
    public String toString() {
        String board = super.toString();
        return board + System.lineSeparator() + "Captured pairs: " +
                "first: " + capturedPairsNo(PlayerRole.FIRST_PLAYER) + ", " +
                "second: " + capturedPairsNo(PlayerRole.SECOND_PLAYER);
    }

    @Override
    public boolean equals(Object o) {
        if (getClass() != o.getClass()) {
            return false;
        }
        Pente p = (Pente) o;
        return stateEqual(p);
    }

    /**
     * Returns: true if the two games have the same state.
     */
    protected boolean stateEqual(Pente p) {
        if (!super.stateEqual(p)) {
            return false;
        }
        if (!capturedPairsNos.equals(p.capturedPairsNos)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new int[] {
                super.hashCode(),
                capturedPairsNos.hashCode()
        });
    }
}
