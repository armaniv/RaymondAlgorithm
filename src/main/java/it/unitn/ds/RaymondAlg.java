package it.unitn.ds;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.IOException;

import it.unitn.ds.Node.*;

import java.util.*;

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

        // create the tree as in page 71 from original paper
        node1.tell(new InitNode(new ArrayList<>(List.of(node2, node3, node4))), null);
        node2.tell(new InitNode(new ArrayList<>(List.of(node1, node5, node6))), null);
        node3.tell(new InitNode(new ArrayList<>(List.of(node1, node7, node8))), null);
        node4.tell(new InitNode(new ArrayList<>(List.of(node1, node10, node9))), null);
        node5.tell(new InitNode(new ArrayList<>(List.of(node2))), null);
        node6.tell(new InitNode(new ArrayList<>(List.of(node2))), null);
        node7.tell(new InitNode(new ArrayList<>(List.of(node3))), null);
        node8.tell(new InitNode(new ArrayList<>(List.of(node3))), null);
        node9.tell(new InitNode(new ArrayList<>(List.of(node4))), null);
        node10.tell(new InitNode(new ArrayList<>(List.of(node4))), null);

        node1.tell(new HolderInfo(Boolean.TRUE, 1), null);


        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            System.out.printf("Interr exc");
        }

        try {
            System.out.println(">>> Press ENTER to start the simulation <<<");
            System.in.read();

            
            node1.tell(new StartRequest(), null);

            node5.tell(new StartRequest(), null);
            node6.tell(new StartRequest(), null);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.printf("Interr exc");
            }

            node2.tell(new Fail(), null);

            System.in.read();

        } catch (IOException ioe) {
            System.out.printf("IO exc");
        }

        system.terminate();
    }
}
