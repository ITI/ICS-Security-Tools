package ui;

import com.sun.javafx.scene.control.behavior.TabPaneBehavior;
import com.sun.javafx.scene.control.skin.TabPaneSkin;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Pane;
import ui.graphing.Graph;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The TabController is responsible for managing the Tabs displayed in the main UI.
 * When a tab is selected, the tree corresponding to that graph gets set in the main display.
 * Some tabs can be closed, and this handles that.
 */
public class TabController {
    protected Pane paneNavigation;
    protected TabPane paneTabs;

    protected final Map<Tab, Graph> graphFromTabs = new LinkedHashMap<>();

    public TabController() {
        paneNavigation = null;
        paneTabs = null;
    }

    public void Init(Pane paneTreeContainer, TabPane paneContent) {
        paneNavigation = paneTreeContainer;
        paneTabs = paneContent;

        // When the tab is changed, change the tree view.
        paneTabs.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == null) {
                SetTreeView(null);
            } else {
                SetTreeView(graphFromTabs.get(newValue).getTreeView());
            }
        });
    }

    private void SetTreeView(TreeView treeView) {
        if(treeView == null) {
            paneNavigation.getChildren().clear();
        } else {
            if (paneNavigation.getChildren().size() == 0) {
                paneNavigation.getChildren().add(treeView);
            } else {
                //There should only ever be 0 or 1 children, so ignore the possibility of more.
                paneNavigation.getChildren().set(0, treeView);
            }
        }
    }

    public Tab AddContent(Graph graphNew) {
        return AddContent(graphNew, graphNew.titleProperty());
    }
    public Tab AddContent(Graph graphNew, String title) {
        return AddContent(graphNew, new SimpleStringProperty(title));
    }
    public Tab AddContent(Graph<?, ?> graphNew, ObservableValue<String> title) {
        Tab tabNew = new Tab();
        tabNew.textProperty().bind(title);
        tabNew.setContent(graphNew.getVisualizationView());

        graphNew.getTreeView().prefHeightProperty().bind(paneNavigation.heightProperty());
        graphNew.getTreeView().prefWidthProperty().bind(paneNavigation.widthProperty());
        graphFromTabs.put(tabNew, graphNew);
        paneTabs.getTabs().add(tabNew);

        return tabNew;
    }

    public void ShowTab(Tab tab) {
        paneTabs.getSelectionModel().select(tab);
    }

    public void clear() {
        //Remove all tabs
        paneNavigation.getChildren().clear();
        graphFromTabs.clear();
        //Change tabs last since the events will fire to update the tree.
        paneTabs.getTabs().clear();
    }

    public void clearTopology() {
        //Get all tabs that are closeable
        List<Tab> closable = paneTabs.getTabs().stream()
                .filter(tab -> tab.isClosable())
                .collect(Collectors.toList());
        //remove graphs
        closable.forEach(tab -> graphFromTabs.remove(tab));
        //close tabs
        closable.forEach(tab -> {
            if (tab.getTabPane().getSkin() instanceof TabPaneSkin) {
                TabPaneBehavior behavior = ((TabPaneSkin) tab.getTabPane().getSkin()).getBehavior();
                if (behavior.canCloseTab(tab)) {
                    behavior.closeTab(tab);
                }
            }
        });
    }

    public Collection<Graph> getGraphs() {
        return graphFromTabs.values();
    }
}
