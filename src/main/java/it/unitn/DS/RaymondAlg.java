package it.unitn.DS;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.IOException;

import it.unitn.DS.Node.JoinTreeMsg;

public class RaymondAlg {

    public static void main(String[] args) {
        // Create the actor system
        final ActorSystem system = ActorSystem.create("Raymond");

        final ActorRef node1 = system.actorOf(Node.props(1));
        final ActorRef node2 = system.actorOf(Node.props(2));
        final ActorRef node3 = system.actorOf(Node.props(3));
        final ActorRef node4 = system.actorOf(Node.props(4));
        final ActorRef node5 = system.actorOf(Node.props(5));
        final ActorRef node6 = system.actorOf(Node.props(6));
        final ActorRef node7 = system.actorOf(Node.props(7));


        // initialize a tree like the one in Fig.7 pag 71 of 'A Tree-Based Algorithm for Distributed Mutual Exclusion'
        Tree<ActorRef> tree_root = new Tree<ActorRef>(node1);
        Tree<ActorRef> tree_n2 = tree_root.addChild(new Tree<>(node2));
        Tree<ActorRef> tree_n3 = tree_root.addChild(new Tree<>(node3));
        Tree<ActorRef> tree_n4 = tree_n2.addChild(new Tree<>(node4));
        Tree<ActorRef> tree_n5 = tree_n2.addChild(new Tree<>(node5));
        Tree<ActorRef> tree_n6 = tree_n3.addChild(new Tree<>(node6));
        Tree<ActorRef> tree_n7 = tree_n3.addChild(new Tree<>(node7));

        SendJoinMessage(tree_root);

        /*
        try {
            System.out.println(">>> Press ENTER to exit <<<");

            System.in.read();
        }
        catch (IOException ioe) {}*/

        system.terminate();
    }

    private static <T> void SendJoinMessage(Tree<ActorRef> node) {
        JoinTreeMsg join = new JoinTreeMsg(node);
        node.getData().tell(join, null);
        node.getChildren().forEach(each -> SendJoinMessage(each));
    }

    private static <T> void printTree(Tree<T> node) {
        System.out.println(node.getData().toString());
        node.getChildren().forEach(each -> printTree(each));
    }
}
