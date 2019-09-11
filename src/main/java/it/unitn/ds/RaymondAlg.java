package it.unitn.ds;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.IOException;

import it.unitn.ds.Node.*;

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
        final ActorRef node8 = system.actorOf(Node.props(8));
        final ActorRef node9 = system.actorOf(Node.props(9));
        final ActorRef node10 = system.actorOf(Node.props(10));

        // Create a tree (like the one in Fig 7 pag 71)
        Tree<ActorRef> tree_root = new Tree<>(node1);
        Tree<ActorRef> tree_n2 = tree_root.addChild(new Tree<>(node2));
        Tree<ActorRef> tree_n3 = tree_root.addChild(new Tree<>(node3));
        Tree<ActorRef> tree_n4 = tree_root.addChild(new Tree<>(node4));
        Tree<ActorRef> tree_n5 = tree_n2.addChild(new Tree<>(node5));
        Tree<ActorRef> tree_n6 = tree_n2.addChild(new Tree<>(node6));
        Tree<ActorRef> tree_n7 = tree_n3.addChild(new Tree<>(node7));
        Tree<ActorRef> tree_n8 = tree_n3.addChild(new Tree<>(node8));
        Tree<ActorRef> tree_n9 = tree_n4.addChild(new Tree<>(node9));
        Tree<ActorRef> tree_n10 = tree_n4.addChild(new Tree<>(node10));

        SendJoinMessage(tree_root);  // send local information about the tree to everyone (starting from the root)

        node6.tell(new StartInitialization(), null);     // set a node as the initial privileged one

        try {
            System.out.println(">>> Press ENTER to start the simulation <<<");
            System.in.read();

            node10.tell(new StartRequest(), null);
            node7.tell(new StartRequest(), null);
            node4.tell(new Fail(), null);
            try {
                Thread.sleep(5000);
            }catch (InterruptedException e) {
                System.out.printf("Interr exc"); 
            }
            node5.tell(new StartRequest(), null);
            node1.tell(new StartRequest(), null);
            node9.tell(new StartRequest(), null);
            node9.tell(new Fail(), null);
            try {
                Thread.sleep(5000);
            }catch (InterruptedException e) {
                System.out.printf("Interr exc"); 
            }
            
            node3.tell(new StartRequest(), null);
            node1.tell(new StartRequest(), null);
            node6.tell(new Fail(), null);
            try {
                Thread.sleep(5000);
            }catch (InterruptedException e) {
                System.out.printf("Interr exc"); 
            }
            node6.tell(new StartRequest(), null);
            node2.tell(new StartRequest(), null);
            node1.tell(new Fail(), null);
              try {
                Thread.sleep(5000);
            }catch (InterruptedException e) {
                System.out.printf("Interr exc"); 
            }
            node8.tell(new StartRequest(), null);
            

            System.in.read();

        } catch (IOException ioe) {
            System.out.printf("IO exc");
        }

        system.terminate();
    }

    // Send local information about the tree, traversing it
    private static void SendJoinMessage(Tree<ActorRef> node) {
        JoinTree join = new JoinTree(node);
        node.getData().tell(join, null);
        node.getChildren().forEach(each -> SendJoinMessage(each));
    }
}
