import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import pa1.City;
import pa1.GameEngine;
import pa1.GameMap;
import pa1.Player;
import pa1.exceptions.TooPoorException;
import pa1.ministers.Minister;
import pa1.technologies.Technology;
import ui.*;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class GameApplication extends Application {
    Stage stage;

    // GameMap
    private static GameEngine gameEngine = new GameEngine();

    // UI Panes
    Menu menu = new Menu();
    GameCanvas gameCanvas;
    InfoBar infoBar = new InfoBar(gameEngine.getMap());

    // UI Scenes
    Scene menuScene = new Scene(menu);
    Scene gameplayScene;

    Player currentPlayer;

    private static Random random = new Random();

    // Generate a random integer between 0 (inclusive) and max (exclusive)
    private static int getRandomInt(int max) {
        return max > 0 ? random.nextInt(max) : 0;
    }

    // Choose one element randomly in the list
    private static <T> T chooseOneRandomly(List<T> list) {
        if (list.isEmpty()) return null;
        int index = getRandomInt(list.size());
        return list.get(index);
    }

    private class ComputerThread implements Runnable {
        private Player player;

        ComputerThread() {
            this.player = currentPlayer;
        }

        @Override
        public void run() {
            List<Minister> readyMinisters = player.getReadyMinisters();
            for (Minister minister : readyMinisters) {
                // TODO: implement a computer thread that makes random decisions
                /*
                 * Step 1:
                 * Choose 5 things randomly, from the player member variable
                 * 1. choose a random action number from 0 to 9
                 * 2. choose a random city from the player's cities
                 * 3. choose a random neighbor of above chosen city (to be used as attackTarget)
                 * 4. choose a random technology from the player's technologies
                 * 5. pick a random number from 0 to the number of troops stationed at the city
                 *    chosen at (2), call this number troopNum
                 *
                 * You may find the methods getRandomInt() and chooseOneRandomly() useful
                 */

                // 1
                int actionNumber = getRandomInt(10);

                // 2
                City city = chooseOneRandomly(player.getCities());

                // 3
                City attackTarget = chooseOneRandomly(gameEngine.getMap().getNeighboringCities(city));

                // 4
                Technology technology = chooseOneRandomly(player.getTechnologies());

                // 5
                int troopNum;
                if (city != null) {
                    troopNum = getRandomInt(city.getTroops() + 1);
                } else {
                    troopNum = 0;
                }

                Platform.runLater(() -> {
                    /*
                     * Step2:
                     * 1. Call gameEngine.processPlayerCommand() method, using player, minister,
                     *    and the 5 randomly chosen items to fill the parameters.
                     * 2. Output the message returned by gameEngine.processPlayerCommand() using infoBar.writeLog()
                     * 3. Since gameEngine.processPlayerCommand() may throw a TooPoorException,
                     *    catch the TooPoorException and print the error message using infoBar.writeLog()
                     */
                    try {
                        // 1, 2
                        infoBar.writeLog(player, gameEngine.processPlayerCommand(actionNumber, player,
                                minister, city, attackTarget, technology, troopNum));
                    } catch (TooPoorException e) {
                        // 3
                        infoBar.writeLog(player, e.getMessage());
                    }

                    // Step 3: call the render() of gameCanvas method to update the Canvas.
                    gameCanvas.render();
                });

                // Simulates human decision making
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }
            Platform.runLater(GameApplication.this::endCurrentPlayerTurn);
        }
    }

    public void beginPlayerTurn(Player player) {
        /*
         * TODO: begin a player turn
         * 1. Set currentPlayer to player
         * 2. Call the beginTurn() method for each minister of the player
         *    Hint: you can use the getMinisters() method of the Player class to get all ministers of a player
         * 3. Check if the player is a human player using the isHuman() method in the Player class
         * 4. If the player is a human player,
         *    4.1. enable the infoBar buttons using infoBar.setDisableButtons(false)
         *    4.2. display the player's information in the infoBar
         *         using the method infoBar.displayPlayer(Player))
         * 5. If the player is not a human player:
         *    5.1. disable the infoBar buttons using infoBar.setDisableButtons(true))
         *    5.2. start a new computer player thread
         *         5.2.1. create a ComputerThread object
         *         5.2.2. create a Thread object using the above ComputerThread object
         *         5.2.3. call the start() method of the Thread object
         */

        // 1
        currentPlayer = player;

        // 2
        for (Minister minister : player.getMinisters()) {
            minister.beginTurn();
        }

        // 3
        if (player.isHuman()) {
            // 4
            infoBar.setDisableButtons(false); // 4.1
            infoBar.displayPlayer(player); // 4.2
        } else {
            // 5
            infoBar.setDisableButtons(true); // 5.1
            Runnable computerThread = new ComputerThread(); // 5.2.1
            Thread thread = new Thread(computerThread); // 5.2.2
            thread.start(); // 5.2.3
        }
    }

    public void initHandlers() {
        /*
         * TODO: initialize all handlers
         * set the handlers in the menu and infoBar components to invoke the appropriate
         * methods of this class
         *
         * 1. Call the setNewGameHandler() method of menu with an event handler that invokes
         *    the newGameHandler() method of this class
         *    Hint: you may use a lambda expression, i.e., e -> newGameHandler()
         * 2. Call the setLoadGameHandler() method of menu with an event handler that invokes
         *    the loadGameHandler() method of this class.
         * 3. Call the setQuitHandler() method of menu with an event handler that invokes
         *    the quitGameHandler() method of this class.
         * 4. Call the setGameActionHandler() method of infoBar with this::gameActionHandler
         * 5. Call the setMenuButtonHandler() method of infoBar with a handler that set the scene
         *    of stage to menuScene
         * 6. Call the setSkipButtonHandler() method of infoBar with a handler that invokes
         *    the endCurrentPlayerTurn() method.
         */

        // 1
        menu.setNewGameHandler(e -> newGameHandler()); // Refer to textbook p. 627

        // 2
        menu.setLoadGameHandler(e -> loadGameHandler());

        // 3
        menu.setQuitHandler(e -> quitGameHandler());

        // 4
        infoBar.setGameActionHandler(this::gameActionHandler);

        // 5
        infoBar.setMenuButtonHandler(e -> stage.setScene(menuScene));

        // 6
        infoBar.setSkipButtonHandler(e -> endCurrentPlayerTurn());
    }

    public void newGameHandler() {
        GameMap gameMap = gameEngine.getMap();
        try {
            gameMap.loadPlayers("players.txt");
            gameMap.loadMap("map.txt");
        } catch (IOException e) {
            Platform.exit();
        }

        /*
         * TODO: complete the new game handler
         *
         * 1. Instantiate the gameCanvas object using gameMap and infoBar
         * 2. Display the GameCanvas by calling its render() method
         * 3. Clear the log in the info bar by calling its clearLog() method
         * 4. Create an HBox with gameCanvas and infoBar.
         * 5. Instantiate the gameplayScene object using the HBox created at step 4.
         * 6. Add the style sheet to gameplayScene to make it better looking.
         *    i.e., gameplayScene.getStylesheets().add("style.css");
         *    This "style.css" file is a style template we set, you do not need to touch it.
         * 7. Set the scene of stage to gameplayScene using the setScene() method of stage.
         * 8. Display the first player by calling beginPlayerTurn with the first player.
         *    Hint: you can get all players using the getPlayers() method in GameMap.
         *    The method returns a List<Player> object, and you can get the i^th element
         *    in a List<> object using its get(i) method.
         */

        // 1
        gameCanvas = new GameCanvas(gameMap, infoBar);

        // 2
        gameCanvas.render();

        // 3
        infoBar.clearLog();

        // 4
        HBox hBox = new HBox(2);
        hBox.getChildren().add(gameCanvas);
        hBox.getChildren().add(infoBar);

        // 5
        Scene gameplayScene = new Scene(hBox);

        // 6
        gameplayScene.getStylesheets().add("style.css");

        // 7
        stage.setScene(gameplayScene);

        // 8
        beginPlayerTurn(gameMap.getPlayers().get(0));
    }

    public void endCurrentPlayerTurn() {
        /*
        EndGame endGame = new EndGame(currentPlayer);
        endGame.setMenuButtonHandler(e -> stage.setScene(menuScene));
        Scene endGameScene = new Scene(endGame);
        endGameScene.getStylesheets().add("style.css");
        stage.setScene(endGameScene);
        */

        currentPlayer.getCities().forEach(c -> infoBar.writeLog(currentPlayer, c.growAtTurnEnd()));
        currentPlayer.getCities().forEach(c -> infoBar.writeLog(currentPlayer, c.invokeRandomEvent(Math.random())));

        if (gameEngine.isGameOver()) {
            Player winner = gameEngine.getWinner();
            EndGame endGame = new EndGame(winner);
            endGame.setMenuButtonHandler(e -> stage.setScene(menuScene));
            Scene endGameScene = new Scene(endGame);
            endGameScene.getStylesheets().add("style.css");
            stage.setScene(endGameScene);
            return;
        }

        Player nextPlayer = currentPlayer;
        do {
            List<Player> players = gameEngine.getMap().getPlayers();
            int index = players.indexOf(nextPlayer);
            index = (index + 1) % players.size();
            nextPlayer = players.get(index);

        } while (!nextPlayer.hasAnyCity());

        beginPlayerTurn(nextPlayer);
    }

    public void gameActionHandler(int actionNum, Minister selectedMinister, City selectedCity, City selectedNeighbor, Technology selectedTech, int troopNum) {
        try {
            if (selectedMinister.isReady()) {
                String msg = gameEngine.processPlayerCommand(
                        actionNum,
                        currentPlayer,
                        selectedMinister,
                        selectedCity,
                        selectedNeighbor,
                        selectedTech,
                        troopNum);
                infoBar.writeLog(currentPlayer, msg);
            } else {
                infoBar.writeLog(currentPlayer, "The selected minister is already done.");
            }
        } catch (TooPoorException e) {
            infoBar.writeLog(currentPlayer, e.getMessage());
        } catch (NullPointerException e) {
            infoBar.writeLog(currentPlayer, "Check your info bar selection");
        }

        gameCanvas.render();
        infoBar.displayPlayer(currentPlayer);

        if (!currentPlayer.hasReadyMinister() || gameEngine.isGameOver())
            endCurrentPlayerTurn();
    }

    public void loadGameHandler() {
    }

    public void quitGameHandler() {
        Platform.exit();
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        menuScene.getStylesheets().add("style.css");
        stage.setScene(menuScene);
        stage.show();
        initHandlers();
    }

    public static void main(String[] args) {
        launch();
    }
}