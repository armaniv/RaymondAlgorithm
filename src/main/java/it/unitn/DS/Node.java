package it.unitn.DS;

import akka.actor.ActorRef;
import akka.actor.AbstractActor;
import akka.actor.Props;
import java.io.Serializable;
import java.util.*;


public class Node extends AbstractActor {
    private int id;     // node id (not really necessary)
    private ActorRef holder;     // relative position of the privileged node
    private Boolean using;  // if this node is currently executing the critical session
    private LinkedList<ActorRef> request_q;    // holds the node of those neighbours that have sent a request
    private Boolean asked;  // true if a node has sent a request message to the current holder

    private Tree<ActorRef> tree_node;   // the local information about the tree


    /* ------------------------ Message types ------------------------ */

    // Start message that gives at every participant the local information about the tree
    public static class JoinTreeMsg implements Serializable {
        private final Tree<ActorRef> tree_node; // node of the tree

        public JoinTreeMsg(Tree<ActorRef> tree_node) {
            this.tree_node = tree_node;
        }
    }


    // A message requesting a node to start the initialization
    public static class StartInitializationMsg implements Serializable {}

    // Initialization message
    public static class InitializationMsg implements Serializable { }


    /* ------------------------ Actor constructor ------------------------ */

    public Node(int id) {
        this.id = id;
    }

    static public Props props(int id) {
        return Props.create(Node.class, () -> new Node(id));
    }


    /* ------------------------ Actor behaviour ------------------------ */

    // Mapping between the received message type and actor methods
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(JoinTreeMsg.class,               this::onJoinTreeMsg)
                .match(StartInitializationMsg.class,    this::onStartInitializationMsg)
                .match(InitializationMsg.class,         this::onInitializationMsg)
                .build();
    }

    private void onJoinTreeMsg(JoinTreeMsg msg) {
        this.tree_node = msg.tree_node;
        // System.out.printf("Joining a tree with ID %02d\n", this.id);
    }

    private void onStartInitializationMsg(StartInitializationMsg m) {
        sendInitilizationMsg();
    }

    private void sendInitilizationMsg() {
        this.holder = getSelf();
        this.request_q = new LinkedList<>();
        this.using = Boolean.FALSE;
        this.asked = Boolean.FALSE;
        InitializationMsg m = new InitializationMsg();
        FloodTree(m,null);
    }

    private void onInitializationMsg(InitializationMsg m) {
        this.holder = getSender();
        this.request_q = new LinkedList<>();
        this.using = Boolean.FALSE;
        this.asked = Boolean.FALSE;
        FloodTree(m, getSender());
    }

    private void FloodTree(Serializable m, ActorRef exclusion) {
        List<ActorRef> receiver = new ArrayList<>();

        if(this.tree_node.getParent()!=null)
            receiver.add(this.tree_node.getParent().getData());
        if(this.tree_node.getChildren()!=null)
            this.tree_node.getChildren().forEach(each -> receiver.add(each.getData()));

        for (ActorRef p: receiver) {
            if (!p.equals(exclusion)) {
                System.out.printf("Node %02d send init to %s\n", this.id,p);
                p.tell(m, getSelf());
            }
        }
    }


}

