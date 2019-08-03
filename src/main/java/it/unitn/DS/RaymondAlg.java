package it.unitn.DS;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.IOException;

import it.unitn.DS.Node.*;

public class RaymondAlg {

    public static void main(String[] args) {
        // Create the actor system
        final ActorSystem system = ActorSystem.create("Raymond");

        // Create the actors
        final ActorRef node1 = system.actorOf(Node.props(1));
        final ActorRef node2 = system.actorOf(Node.props(2));
        final ActorRef node3 = system.actorOf(Node.props(3));
        final ActorRef node4 = system.actorOf(Node.props(4));
        final ActorRef node5 = system.actorOf(Node.props(5));
        final ActorRef node6 = system.actorOf(Node.props(6));
        final ActorRef node7 = system.actorOf(Node.props(7));

        // Create a tree
        Tree<ActorRef> tree_root = new Tree<ActorRef>(node1);
        Tree<ActorRef> tree_n2 = tree_root.addChild(new Tree<>(node2));
        Tree<ActorRef> tree_n3 = tree_root.addChild(new Tree<>(node3));
        Tree<ActorRef> tree_n4 = tree_n2.addChild(new Tree<>(node4));
        Tree<ActorRef> tree_n5 = tree_n2.addChild(new Tree<>(node5));
        Tree<ActorRef> tree_n6 = tree_n3.addChild(new Tree<>(node6));
        Tree<ActorRef> tree_n7 = tree_n3.addChild(new Tree<>(node7));

        SendJoinMessage(tree_root);  // send local information about the tree to everyon

        node2.tell(new StartInitializationMsg(), null);     // set a node as the initial privileged one

        try {
            System.out.println(">>> Press ENTER to exit <<<");
            System.in.read();

            node2.tell(new StartRequestMsg(), null);
            node5.tell(new StartRequestMsg(), null);
            node4.tell(new StartRequestMsg(), null);

            System.in.read();
        } catch (IOException ioe) {
        }

        system.terminate();
    }

    // Send local information about the tree, traversing it
    private static void SendJoinMessage(Tree<ActorRef> node) {
        JoinTreeMsg join = new JoinTreeMsg(node);
        node.getData().tell(join, null);
        node.getChildren().forEach(each -> SendJoinMessage(each));
    }
}
