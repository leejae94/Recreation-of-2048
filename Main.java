package game2048;

import ucb.util.CommandArgs;
import java.util.Arrays;
import game2048.gui.Game;
import static game2048.Main.Side.*;

/** The main class for the 2048 game.
 *  @author Jae Lee
 */
public class Main {

    /** Size of the board: number of rows and of columns. */
    static final int SIZE = 4;
    /** Final score to win the game.*/
    static final int FINALSCORE = 2048;
    /** Number of squares on the board. */
    static final int SQUARES = SIZE * SIZE;

    /** Symbolic names for the four sides of a board. */
    static enum Side { NORTH, EAST, SOUTH, WEST };

    /** The main program.  ARGS may contain the options --seed=NUM,
     *  (random seed); --log (record moves and random tiles
     *  selected.); --testing (take random tiles and moves from
     *  standard input); and --no-display. */
    public static void main(String... args) {
        CommandArgs options =
            new CommandArgs("--seed=(\\d+) --log --testing --no-display",
                            args);
        if (!options.ok()) {
            System.err.println("Usage: java game2048.Main [ --seed=NUM ] "
                               + "[ --log ] [ --testing ] [ --no-display ]");
            System.exit(1);
        }

        Main game = new Main(options);

        while (game.play()) {
            continue;
        }
        System.exit(0);
    }

    /** A new Main object using OPTIONS as options (as for main). */
    Main(CommandArgs options) {
        boolean log = options.contains("--log"),
            display = !options.contains("--no-display");
        long seed = !options.contains("--seed") ? 0 : options.getLong("--seed");
        _testing = options.contains("--testing");
        _game = new Game("2048", SIZE, seed, log, display, _testing);
    }

    /** Reset the score for the current game to 0 and clear the board. */
    void clear() {
        _score = 0;
        _count = 0;
        _game.clear();
        _game.setScore(_score, _maxScore);
        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                _board[r][c] = 0;
            }
        }
    }

    /** Play one game of 2048, updating the maximum score. Return true
     *  iff play should continue with another game, or false to exit. */
    boolean play() {
        clear();
        setRandomPiece();
        while (true) {
            setRandomPiece();
            if (gameOver()) {
                _maxScore = _score > _maxScore ? _score : _maxScore;
                _game.setScore(_score, _maxScore);
                _game.endGame();
            }

        GetMove:
            while (true) {
                String key = _game.readKey();
                if (key.equals("\u2191")) {
                    key = "Up";
                }
                if (key.equals("\u2193")) {
                    key = "Down";
                }
                if (key.equals("\u2190")) {
                    key = "Left";
                }
                if (key.equals("\u2192")) {
                    key = "Right";
                }
                switch (key) {
                case "Up": case "Down": case "Left": case "Right":
                    if (!gameOver() && !tiltBoard(key)) {
                        break GetMove;
                    }
                    break;
                case "New Game":
                    clear();
                    return true;
                case "Quit":
                    return false;
                default:
                    break;
                }
            }
        }
    }

    /** Helper method which takes KEY and
     * moves the tiles across the board according to
     * the corresponding direction.*/
    void innerPlay(String key) {
        Side side = keyToSide(key);
        int[][] cBoard = new int[SIZE][SIZE];
        for (int col = 0; col < SIZE; col++) {
            for (int row = SIZE - 1; row >= 0; row--) {
                cBoard[row][col] =
                        _board[tiltRow(side, row, col)]
                            [tiltCol(side, row, col)];
            }
        }
        moveAllTiles(side, cBoard);
    }

    /** Helper method which takes SIDE and CBOARD
     * and moves all tiles accordingly.*/
    void moveAllTiles(Side side, int[][] cBoard) {
        int mergePlace;
        for (int col = 0; col < SIZE; col++) {
            boolean merged = false;
            for (int row = 0; row < SIZE; row++) {
                if (cBoard[row][col] != 0) {
                    int shift = findSpot(cBoard, row, col);
                    try {
                        mergePlace = cBoard[row - shift - 1][col];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        mergePlace = cBoard[row - shift][col];
                    }
                    try {
                        if (cBoard[row - 1][col] != 0
                                && cBoard[row][col] != cBoard[row - 1][col]) {
                            continue;
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        continue;
                    }
                    if (!merged
                        && cBoard[row][col] == mergePlace) {
                        _game.mergeTile(cBoard[row][col],
                                2 * cBoard[row][col],
                                tiltRow(side, row, col),
                                tiltCol(side, row, col),
                                tiltRow(side, row - shift - 1, col),
                                tiltCol(side, row - shift - 1, col));
                        _board[tiltRow(side, row - shift - 1, col)]
                                [tiltCol(side, row - shift - 1, col)] =
                                2 * cBoard[row][col];
                        _board[tiltRow(side, row, col)]
                                [tiltCol(side, row, col)] = 0;
                        cBoard[row - shift - 1][col] = 2 * cBoard[row][col];
                        cBoard[row][col] = 0;
                        _count--;
                        _score += cBoard[row - shift - 1][col];
                        merged = true;
                    } else {
                        _game.moveTile(cBoard[row][col],
                                tiltRow(side, row, col),
                                tiltCol(side, row, col),
                                tiltRow(side, row - shift, col),
                                tiltCol(side, row - shift, col));
                        _board[tiltRow(side, row - shift, col)]
                                [tiltCol(side, row - shift, col)] =
                                    cBoard[row][col];
                        _board[tiltRow(side, row, col)]
                                [tiltCol(side, row, col)] = 0;
                        cBoard[row - shift][col] = cBoard[row][col];
                        cBoard[row][col] = 0;
                        merged = false;
                    }
                }
            }
        }
        _game.setScore(_score, _maxScore);
        _game.displayMoves();

    }

    /** Helper method that takes in CBOARD, R, and C,
     *and returns the farthest available spot.**/
    int findSpot(int[][] cBoard, int r, int c) {
        try {
            if (cBoard[r - 1][c] != 0) {
                return 0;
            } else {
                return 1 + findSpot(cBoard, r - 1, c);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }


    /** Return true iff the current game is over (no more moves
     *  possible). Discussed with Randy Shi about the
     *  algorithm.*/
    boolean gameOver() {
        if (_maxScore == FINALSCORE) {
            return true;
        } else if (_count == SQUARES) {
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    try {
                        if (_board[row][col] == _board[row][col + 1]
                                || _board[col][row] == _board[col + 1][row]) {
                            return false;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        continue;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /** Add a tile to a random, empty position, choosing a value (2 or
     *  4) at random.  Has no effect if the board is currently full. */
    void setRandomPiece() {
        if (_count == SQUARES) {
            return;
        }
        int[] tile = _game.getRandomTile();
        if (_board[tile[1]][tile[2]] != 0) {
            setRandomPiece();
        } else {
            _board[tile[1]][tile[2]] = tile[0];
            _game.addTile(tile[0], tile[1], tile[2]);
            _count++;
        }
    }

    /** Perform the result of tilting the board toward KEY.
     *  Returns true iff the tilt changes the board. **/
    boolean tiltBoard(String key) {
        int[][] board = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                board[r][c] =
                    _board[r][c];
            }
        }
        innerPlay(key);
        return (Arrays.deepEquals(board, _board));
    }

    /** Return the row number on a playing board that corresponds to row R
     *  and column C of a board turned so that row 0 is in direction SIDE (as
     *  specified by the definitions of NORTH, EAST, etc.).  So, if SIDE
     *  is NORTH, then tiltRow simply returns R (since in that case, the
     *  board is not turned).  If SIDE is WEST, then column 0 of the tilted
     *  board corresponds to row SIZE - 1 of the untilted board, and
     *  tiltRow returns SIZE - 1 - C. */
    int tiltRow(Side side, int r, int c) {
        switch (side) {
        case NORTH:
            return r;
        case EAST:
            return c;
        case SOUTH:
            return SIZE - 1 - r;
        case WEST:
            return SIZE - 1 - c;
        default:
            throw new IllegalArgumentException("Unknown direction");
        }
    }

    /** Return the column number on a playing board that corresponds to row
     *  R and column C of a board turned so that row 0 is in direction SIDE
     *  (as specified by the definitions of NORTH, EAST, etc.). So, if SIDE
     *  is NORTH, then tiltCol simply returns C (since in that case, the
     *  board is not turned).  If SIDE is WEST, then row 0 of the tilted
     *  board corresponds to column 0 of the untilted board, and tiltCol
     *  returns R. */
    int tiltCol(Side side, int r, int c) {
        switch (side) {
        case NORTH:
            return c;
        case EAST:
            return SIZE - 1 - r;
        case SOUTH:
            return SIZE - 1 - c;
        case WEST:
            return r;
        default:
            throw new IllegalArgumentException("Unknown direction");
        }
    }

    /** Return the side indicated by KEY ("Up", "Down", "Left",
     *  or "Right"). */
    Side keyToSide(String key) {
        if (key.equals("\u2191")) {
            key = "Up";
        }
        if (key.equals("\u2193")) {
            key = "Down";
        }
        if (key.equals("\u2190")) {
            key = "Left";
        }
        if (key.equals("\u2192")) {
            key = "Right";
        }
        switch (key) {
        case "Up":
            return NORTH;
        case "Down":
            return SOUTH;
        case "Left":
            return WEST;
        case "Right":
            return EAST;
        default:
            throw new IllegalArgumentException("unknown key designation");
        }
    }

    /** Represents the board: _board[r][c] is the tile value at row R,
     *  column C, or 0 if there is no tile there. */
    private final int[][] _board = new int[SIZE][SIZE];

    /** True iff --testing option selected. */
    private boolean _testing;
    /** THe current input source and output sink. */
    private Game _game;
    /** The score of the current game, and the maximum final score
     *  over all games in this session. */
    private int _score, _maxScore;
    /** Number of tiles on the board. */
    private int _count;
}
