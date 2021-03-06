import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;

import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.BlendMode;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Objects;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private TabPane tab_pane;
    @FXML
    private GridPane tab_main, query_view_main, connections_view_main;
    @FXML
    private TextArea query_field_main;
    @FXML
    private TableView<ObservableList> sql_table_main;
    @FXML
    private Label msg_main;
    @FXML
    private Tab main_tab;
    @FXML
    private Button exe_btn_main;
    @FXML
    private ComboBox<String> history_main;

    private SaveAndRestore sar = new SaveAndRestore();

    //to store the previously executed queries
    private LinkedList<String> prevQueries = new LinkedList<>();

    //to store the active connections
    private LinkedList<ConnectionInfo> connections = new LinkedList<>();

    //to store the buttons for the connections in the connections_view
    private LinkedList<Buttons> buttons = new LinkedList<>();

    //to be able to call methods from TabController class
    private static TabController tc = new TabController();
    static void injectTabController(TabController tabCont){
        tc = tabCont;
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        NewConnectionController.injectMainController(this);
        MainClass.injectMainController(this);
        TabController.injectMainController(this);
        //Database.injectMainController(this);


        //adding the saved connections to the connections_view as buttons if there are any
        if (sar.ipInDB()) {
            String[] cons = sar.restoreFromDB().split(";");
            for (String curr : cons) {
                String conName = curr.split(",")[0];
                String hostName = curr.split(",")[1];
                String port = curr.split(",")[2];
                String databaseName = curr.split(",")[3];
                String user = curr.split(",")[4];
                String pass = curr.split(",")[5];
                String DBtype = curr.split(",")[6];
                Connection con = Database.createConnection(hostName, port, databaseName, user, pass, DBtype);
                addCon(conName, hostName, port, databaseName, user, pass, con);
            }
            addConButtons();
        }

        exe_btn_main.setTooltip(new Tooltip("Or use [Ctrl+Enter]"));

        //the query can also be executed with [Ctrl+Enter]
        query_field_main.setOnKeyPressed(e ->  {
            if ((new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN)).match(e)) {
                executeAction();
            }
        });
    }


    //the window in which needed information about the database is input by the user
    public void openNewConWindow(){
        //loading the window in a background task, in order to show a progress indicator
        Task<Parent> loadTask = new Task<Parent>() {
            @Override
            public Parent call() throws IOException {
                return FXMLLoader.load(getClass().getResource("newConnection.fxml"));
            }
        };

        ProgressIndicator progress = new ProgressIndicator();
        progress.progressProperty().bind(loadTask.progressProperty());
        BorderPane root = new BorderPane();
        root.setPrefSize(70, 70);
        root.setCenter(progress);
        root.setBackground(Background.EMPTY);
        Stage taskUpdateStage = new Stage();
        taskUpdateStage.initStyle(StageStyle.TRANSPARENT);
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        taskUpdateStage.setScene(scene);
        taskUpdateStage.show();

        loadTask.setOnSucceeded(e -> {
            taskUpdateStage.hide();
            Stage stage = new Stage();
            stage.setTitle("Create a new Connection");
            stage.setScene(new Scene(loadTask.getValue(), 600, 460));
            stage.setResizable(false);
            stage.show();
        });

        new Thread(loadTask).start();
    }


    void switchToQueryView(){
        connections_view_main.setVisible(false);
        query_view_main.setVisible(true);
    }


    private void switchToConnectionsView(){
        query_view_main.setVisible(false);
        connections_view_main.setVisible(true);
    }


    //creating a new tab and switching to it after pressing the button "New"
    public void addNewTab() throws IOException{
        Tab newTab = new Tab("New Connection");
        tab_pane.getTabs().add(tab_pane.getTabs().size() - 1, newTab);
        tab_pane.getSelectionModel().select(newTab);

        Parent root = FXMLLoader.load(getClass().getResource("tab.fxml"));
        newTab.setContent(root);

        tc.setPrefSizeTab(tab_main.getWidth(), tab_main.getHeight());
    }


    //giving the current tab a name
    void setTab(String name){
        Tab currentTab = tab_pane.getSelectionModel().getSelectedItem();
        currentTab.setText(name);
    }


    //adding a connection to the LinkedList
    void addCon(String conName, String hostName, String port, String databaseName, String user, String pass, Connection connection){
        connections.add(new ConnectionInfo(conName, hostName, port, databaseName, user, pass, connection));
    }


    //getting lastly added connection
    String getCon(){
        if (connections.size() == 0) return "null";
        return connections.getLast().getConName();
    }


    LinkedList<ConnectionInfo> allConnections(){
        return connections;
    }


    LinkedList<Buttons> allButtons() {
        return buttons;
    }


    void tabPaneRequestFocus(){
        tab_pane.requestFocus();
    }


    boolean isMainTab () {
        return tab_pane.getSelectionModel().getSelectedItem() == main_tab;
    }


    String getCurrentTabName(){
        return tab_pane.getSelectionModel().getSelectedItem().getText();
    }


    //checks if a connection is already active
    boolean isConNameInList(String name){
        for (ConnectionInfo current : connections){
            if (current.getConName().equals(name)) return true;
        }
        return false;
    }


    //after pressing the button "Execute" the query is being sent to the database and executed
    public void executeAction() {

        String query = query_field_main.getText();
        Connection con = null;
        String firstWord = query.split(" ")[0];

        for (ConnectionInfo current : connections) {
            if (current.getConName().equals(getCurrentTabName()))
                con = current.getConnection();
        }

        boolean insert = firstWord.equalsIgnoreCase("insert");
        boolean update = firstWord.equalsIgnoreCase("update");
        boolean delete = firstWord.equalsIgnoreCase("delete");
        boolean create = firstWord.equalsIgnoreCase("create");
        boolean drop = firstWord.equalsIgnoreCase("drop");
        boolean use = firstWord.equalsIgnoreCase("use");
        boolean alter = firstWord.equalsIgnoreCase("alter");

        if (insert || update || delete || create || drop || use || alter) {
            if (Database.executeUpdate(query, Objects.requireNonNull(con), sql_table_main, msg_main))
                addQueryToQueue(query);
        } else {
            if (Database.executeQuery(query, con, sql_table_main, msg_main))
                addQueryToQueue(query);
        }
    }


    public void closeConnection() throws SQLException{
        String conName = getCurrentTabName();
        for (ConnectionInfo curr : connections) {
            if (curr.getConName().equals(conName)) {
                curr.getConnection().close();
                connections.remove(curr);
                sar.removeFromDB(curr.getConName());
                break;
            }
        }
        removeConButtons();
        removeButton(conName);
        addConButtons();
        setTab("New Connection");
        switchToConnectionsView();
    }


    boolean buttonNotInList(Button btn) {
        for (Buttons curr : buttons) {
            if (curr.getButton().equals(btn)) return false;
        }
        return true;
    }


    void removeButton(String btnName) {
        for (Buttons curr : buttons) {
            if (curr.getName().equals(btnName)) {
                buttons.remove(curr);
                return;
            }
        }
    }


    //adding the active connections to the connections_view as buttons
    private void addConButtons() {
        int i = 1, j = 0;
        for (ConnectionInfo current : connections) {
            Button conButton = new Button(current.getConName());
            if (buttonNotInList(conButton)) buttons.add(new Buttons(current.getConName(), conButton));
            conButton.setMaxWidth(125);
            conButton.setPrefHeight(62);
            conButton.setBlendMode(BlendMode.MULTIPLY);
            conButton.setOnAction(e -> {
                switchToQueryView();
                setTab(current.getConName());
            });
            connections_view_main.add(conButton, i, j);
            i++;
            if (i == 4) {
                i = 0;
                j++;
            }
        }
    }


    private void removeConButtons() {
        for (Buttons curr : buttons) {
            connections_view_main.getChildren().remove(curr.getButton());
        }
    }


    private void addQueryToQueue(String query) {
        prevQueries.remove(query);
        prevQueries.add(query);
        if (prevQueries.size() > 10)
            prevQueries.removeFirst();
        history_main.setItems(FXCollections.observableArrayList(prevQueries));
        if (prevQueries.size() == 2)
            history_main.setVisibleRowCount(5);
        if (prevQueries.size() == 3)
            history_main.setVisibleRowCount(10);
    }


    public void selectQueryFromQueue_main() {
        String query = history_main.getSelectionModel().getSelectedItem();
        history_main.getSelectionModel().clearSelection();
        query_field_main.setText(query);

//        Task<Void> sleeper = new Task<Void>() {
//            @Override
//            protected Void call() {
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                }
//                return null;
//            }
//        };
//        sleeper.setOnSucceeded(event -> {
//            history_main.getSelectionModel().clearSelection();
//            query_field_main.setText(query);
//        });
//        new Thread(sleeper).start();


//        new Timer().schedule(
//                new TimerTask() {
//                    @Override
//                    public void run() {
//                        history_main.getSelectionModel().clearSelection();
//                        query_field_main.setText(query);
//                    }
//                },
//                200
//        );

    }

}














