package it.unitn.DS;

import akka.actor.ActorRef;
import akka.actor.AbstractActor;
import akka.actor.Props;

import java.io.Serializable;
import java.util.*;


public class Node extends AbstractActor {
    final static int CSDURATION = 2000;         // the duration (in milliseconds) of the critical session
    private final int id;                       // node id (not really necessary)
    private ActorRef holder;                    // relative position of the privileged node
    private Boolean using;                      // if this node is currently executing the critical session
    private LinkedList<ActorRef> request_q;     // queue of the requests
    private Boolean asked;                      // true if a node has sent a request message to the current holder
    private Tree<ActorRef> tree_node;           // local information about the tree


    /* ------------------------ Message types ------------------------ */

    // Start message that gives at every participant the local information about the tree
    public static class JoinTreeMsg implements Serializable {
        private final Tree<ActorRef> tree_node; // node of the tree

        public JoinTreeMsg(Tree<ActorRef> tree_node) {
            this.tree_node = tree_node;
        }
    }


    // A message requesting a node to start the initialization
    public static class StartInitializationMsg implements Serializable {
    }

    // A Initialization message
    public static class InitializationMsg implements Serializable {
    }

    // A message requesting a node to ask for entering the critical session
    public static class StartRequestMsg implements Serializable {
    }

    // A request message
    public static class RequestMsg implements Serializable {
    }

    // A privilege message
    public static class PrivilegeMsg implements Serializable {
    }


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
                .match(JoinTreeMsg.class, this::onJoinTreeMsg)
                .match(StartInitializationMsg.class, this::onStartInitializationMsg)
                .match(InitializationMsg.class, this::onInitializationMsg)
                .match(StartRequestMsg.class, this::onStartRequestMsg)
                .match(RequestMsg.class, this::onRequestMsg)
                .match(PrivilegeMsg.class, this::onPrivilegeMsg)
                .build();
    }

    private void onJoinTreeMsg(JoinTreeMsg msg) {
        this.tree_node = msg.tree_node;
        // System.out.printf("Joining a tree with ID %02d\n", this.id);
    }

    private void onStartInitializationMsg(StartInitializationMsg m) {
        sendInitilizationMsg();
    }

    private void onInitializationMsg(InitializationMsg m) {
        // initialize the node
        this.holder = getSender();
        this.request_q = new LinkedList<>();
        this.using = Boolean.FALSE;
        this.asked = Boolean.FALSE;
        // System.out.printf("Node %02d has holder %s\n", this.id, this.holder);
        FloodTree(m, getSender());   // propagate the message
    }

    private void onStartRequestMsg(StartRequestMsg m) {
        this.request_q.add(getSelf());  // add self to the queue
        assignPrivilege();
        makeRequest();
    }

    private void onRequestMsg(RequestMsg m) {
        this.request_q.add(getSender());    // add sender to the queue
        assignPrivilege();
        makeRequest();
    }

    private void onPrivilegeMsg(PrivilegeMsg m) {
        this.holder = getSelf();     // set holder to self
        assignPrivilege();
        makeRequest();
    }

    private void sendInitilizationMsg() {
        // initialize the node
        this.holder = getSelf();
        this.request_q = new LinkedList<>();
        this.using = Boolean.FALSE;
        this.asked = Boolean.FALSE;
        // send initialization message
        InitializationMsg m = new InitializationMsg();
        FloodTree(m, null);
    }

    // Send message within the tree. If 'exclusion' is set, a message is not sent to it
    private void FloodTree(Serializable m, ActorRef exclusion) {
        List<ActorRef> receiver = new ArrayList<>();    //list of who will receive the message

        // check parent and child/ren of a node and, if they are present, add they to 'receiver'
        if (this.tree_node.getParent() != null)
            receiver.add(this.tree_node.getParent().getData());
        if (this.tree_node.getChildren() != null)
            this.tree_node.getChildren().forEach(each -> receiver.add(each.getData()));

        for (ActorRef p : receiver) {
            if (!p.equals(exclusion)) {
                // System.out.printf("Node %02d send init to %s\n", this.id, p);
                p.tell(m, getSelf());
            }
        }
    }

    private void assignPrivilege() {
        // maybe we should add in the condition also:
        //      head(REQUEST_Q) != self
        if (this.holder == getSelf() && this.using == Boolean.FALSE && this.request_q.size() != 0) {
            this.holder = this.request_q.removeFirst();
            this.asked = Boolean.FALSE;
            if (this.holder == getSelf()) {
                this.using = Boolean.TRUE;
                criticalSection();
            } else {
                System.out.printf("Node %02d send privilege\n", this.id);
                PrivilegeMsg m = new PrivilegeMsg();
                this.holder.tell(m, getSelf());
            }
        }

    }

    private void makeRequest() {
        if (this.holder != getSelf() && this.request_q.size() != 0 && this.asked == Boolean.FALSE) {
            System.out.printf("Node %02d send request\n", this.id);
            RequestMsg m = new RequestMsg();
            this.holder.tell(m, getSelf());
            this.asked = Boolean.TRUE;
        }
    }

    private void criticalSection() {
        System.out.printf("Node %02d enter the critical session\n", this.id);

        // simulate the critical session
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0L;
        while (elapsedTime < CSDURATION) {
            elapsedTime = (new Date()).getTime() - startTime;
        }

        System.out.printf("Node %02d exit the critical session\n", this.id);

        this.using = Boolean.FALSE;
        assignPrivilege();
        makeRequest();
    }
}

