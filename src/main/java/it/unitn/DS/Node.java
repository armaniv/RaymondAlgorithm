package it.unitn.DS;

import akka.actor.ActorRef;
import akka.actor.AbstractActor;
import akka.actor.Props;

import java.io.Serializable;


public class Node extends AbstractActor {
    private int id;
    private Tree<ActorRef> tree_node;


    /* ------------------------ Message types ------------------------ */

    // Start message that informs every participant about its neighbour
    public static class JoinTreeMsg implements Serializable {
        private final Tree<ActorRef> tree_node; // node of the tree

        public JoinTreeMsg(Tree<ActorRef> tree_node) {
            this.tree_node = tree_node;
        }
    }


    /* ------------------------ Actor constructor ------------------------ */

    public Node(int id) {
        this.id = id;
    }

    static public Props props(int id) {
        return Props.create(Node.class, () -> new Node(id));
    }

    /* ------------------------ Actor behaviour ------------------------ */


    // Here we define the mapping between the received message types
    // and our actor methods
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(JoinTreeMsg.class, this::onJoinTreeMsg)
                .build();
    }

    private void onJoinTreeMsg(JoinTreeMsg msg) {
        this.tree_node = msg.tree_node;
        System.out.printf("%s: joining a tree with ID %02d\n", getSelf().path().name(), this.id);
    }
}

