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
    private Boolean want_to_access_CS;

    private Boolean recovering;  
    private Hashtable<ActorRef, Tuple<Boolean, Advise>> contacted_neighbours;


    /* ------------------------ Message types ------------------------ */

    // Start message that gives at every participant the local information about the tree
    public static class JoinTree implements Serializable {
        private final Tree<ActorRef> tree_node; // node of the tree

        public JoinTree(Tree<ActorRef> tree_node) {
            this.tree_node = tree_node;
        }
    }


    // A message requesting a node to start the initialization
    public static class StartInitialization implements Serializable {}

    // A Initialization message
    public static class Initialization implements Serializable {}

    // A message requesting a node to ask for entering the critical session
    public static class StartRequest implements Serializable {}

    // A request message
    public static class Request implements Serializable {}

    // A privilege message
    public static class Privilege implements Serializable {}

    // Message used by Sys to simulate failing 
    public static class Fail implements Serializable {}

    // Message used by Sys to launch the Recovery phase 
    public static class Recovery implements Serializable {}

    // Message used by a node to inform neighbours about Restart after failure 
    public static class Restart implements Serializable {}

    // Message used to send local knowledge a restarting node 
    public static class Advise implements Serializable {
        private final ActorRef holder;
        private final LinkedList<ActorRef> request_q;
        private final Boolean asked;
        public final int id;

        public Advise(ActorRef holder, LinkedList<ActorRef> request_q, Boolean asked, int id) {
            this.holder = holder;
            this.request_q = request_q;
            this.asked = asked;
            this.id = id;
        }
    }

    /* ------------------------ Actor constructor ------------------------ */
    public Node(int id) {
        this.id = id;
        this.recovering = Boolean.FALSE;
        this.want_to_access_CS = Boolean.FALSE;
    }

    static public Props props(int id) {
        return Props.create(Node.class, () -> new Node(id));
    }

    /* ------------------------ Actor behaviour ------------------------ */

    private void onJoinTree(JoinTree msg) {
        this.tree_node = msg.tree_node;
    }

    private void onStartInitialization(StartInitialization m) {
        sendInitilization();
    }

    private void onInitialization(Initialization m) {
        // initialize the node
        this.holder = getSender();
        this.request_q = new LinkedList<>();
        this.using = Boolean.FALSE;
        this.asked = Boolean.FALSE;
        this.recovering = Boolean.FALSE;
        FloodTree(m, getSender());   // propagate the message
    }

    private void onStartRequest(StartRequest m) {
        this.want_to_access_CS = Boolean.TRUE;
        this.request_q.add(getSelf());  // add self to the queue
        assignPrivilege();
        makeRequest();
    }

    private void onRequestMsg(Request m) {
        this.request_q.add(getSender());    // add sender to the queue
        assignPrivilege();
        makeRequest();
    }

    private void onPrivilegeMsg(Privilege m) {
        this.holder = getSelf();     // set holder to self
        assignPrivilege();
        makeRequest();
    }

    // simulates the failure of the node and prepares a
    // clean/new local environment for the Recovery phase
    private void onFailMsg(Fail m){
        // delay for 2 seconds to ensure node 
        // doesn't receive messages during failure
        try{ 
            int ms = 2000;
            Thread.sleep(ms); 
        } 
        catch (InterruptedException e){
            e.printStackTrace();
        }
        // inform other methods that we are restarting
        // if some messages are received during this phase 
        this.recovering = Boolean.TRUE;
        // delete any previous knowledge about the algorithm status 
        this.request_q = new LinkedList<>();
        this.using = Boolean.FALSE;
        this.asked = Boolean.FALSE;
        this.holder = null;
        System.out.printf("Node %02d FAILED, starting Recovery...\n", this.id);
    }

    // starts the recovery phase this methods allows to simulate 
    // reception of messages during the recovery phase
    private void onRecoveryMsg(Recovery m){
        // delay for a period to ensure that messages of 
        // this node have been received
        try{ 
            int ms = 2000;
            Thread.sleep(ms); 
        } 
        catch (InterruptedException e){
            e.printStackTrace();
        }
        // inform neighbours about Restart a hashtable to 
        // memorize contacted neighbors and their status
        this.contacted_neighbours = new Hashtable<>();
        this.tree_node.getNeighbors().forEach(neighbor -> {
            neighbor.tell(new Restart(), getSelf());
            this.contacted_neighbours
                .put(neighbor, new Tuple<Boolean, Advise>(Boolean.FALSE, null));
        });
    }

    private void onRestart(Restart m){
        System.out.printf("Node %02d receives Restart\n", this.id);
        Advise advise = new Advise(this.holder, this.request_q, this.asked, this.id);
        getSender().tell(advise, getSelf());
    }

    private void onAdvise(Advise m){
        System.out.printf("Node %02d receives Advise\n", this.id);
        // never execute this method body if the node is not recovering
        if (this.recovering){
            if (this.contacted_neighbours.get(getSender()).x == Boolean.FALSE){
                // set neighbor as visited and save its data
                this.contacted_neighbours.get(getSender()).x = Boolean.TRUE;
                this.contacted_neighbours.get(getSender()).y = m;
            }
            if (collectedAllAdvises()){
                inferStatus();
            }
        }
    }

    private void sendInitilization() {
        // initialize the node
        this.holder = getSelf();
        this.request_q = new LinkedList<>();
        this.using = Boolean.FALSE;
        this.asked = Boolean.FALSE;
        // send initialization message
        Initialization m = new Initialization();
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
        // ensure that those methods are never 
        // called during the recovery phase
        if (!this.recovering) {
            // maybe we should add in the condition also:
            //      head(REQUEST_Q) != self
            if (this.holder == getSelf() && this.using == Boolean.FALSE && this.request_q.size() != 0) {
                this.holder = this.request_q.removeFirst();
                this.asked = Boolean.FALSE;
                if (this.holder == getSelf()) {
                    this.using = Boolean.TRUE;
                    if (this.want_to_access_CS){
                        criticalSection();
                        this.want_to_access_CS = Boolean.FALSE;
                    }
                } else {
                    System.out.printf("Node %02d send privilege\n", this.id);
                    Privilege m = new Privilege();
                    this.holder.tell(m, getSelf());
                }
            }
        }

    }

    private void makeRequest() {
        // ensure that this method is never 
        // called during the recovery phase
        if (!this.recovering) {
            if (this.holder != getSelf() && this.request_q.size() != 0 && this.asked == Boolean.FALSE) {
                System.out.printf("Node %02d send request\n", this.id);
                Request m = new Request();
                this.holder.tell(m, getSelf());
                this.asked = Boolean.TRUE;
            }
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

    private Boolean collectedAllAdvises(){
        for (ActorRef key : contacted_neighbours.keySet()){
            if (contacted_neighbours.get(key).x == Boolean.FALSE){
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    private void inferStatus(){
        for (ActorRef key : contacted_neighbours.keySet()){
            Tuple<Boolean, Advise> t = contacted_neighbours.get(key);
        }

        Boolean holder_for_all = Boolean.TRUE;

        for (ActorRef neighbor : contacted_neighbours.keySet()){
            Tuple<Boolean, Advise> data = contacted_neighbours.get(neighbor);
            Advise advise = data.y;
           
            if (advise.holder != getSelf()){ 
                // This node is my holder
                this.holder = neighbor;
                holder_for_all = Boolean.FALSE;

            }else if (advise.asked == Boolean.TRUE){
                // if I am the holder and the node has requested the token
                // it should be present in my request_queue
                // but only if not already present
                if (!this.request_q.contains((neighbor))){
                    this.request_q.add(neighbor);
                }
            }
        }

        // I have the token if i am holder for all neighbors
        if (holder_for_all){
            this.holder = getSelf();
            this.asked = Boolean.FALSE;
        } else if (this.contacted_neighbours.get(this.holder).y.request_q.contains(getSelf())){
            this.asked = Boolean.TRUE;
        }

        this.recovering = Boolean.FALSE;

        // restart participation to algorithm
        assignPrivilege();
        makeRequest();

        System.out.printf("Node %02d has infered:\n", this.id);
        System.out.printf("holder:%02d\n", this.contacted_neighbours.get(holder).y.id);
        System.out.printf("asked: %b\n", this.asked);
        System.out.printf("size of req_q: %02d\n\n", this.request_q.size());
    }

    // Mapping between the received message type and actor methods
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(JoinTree.class, this::onJoinTree)
                .match(StartInitialization.class, this::onStartInitialization)
                .match(Initialization.class, this::onInitialization)
                .match(StartRequest.class, this::onStartRequest)
                .match(Request.class, this::onRequestMsg)
                .match(Privilege.class, this::onPrivilegeMsg)
                .match(Fail.class, this::onFailMsg)
                .match(Recovery.class, this::onRecoveryMsg)
                .match(Restart.class, this::onRestart)
                .match(Advise.class, this::onAdvise)
                .build();
    }

    private class Tuple<X, Y> { 
        public X x; 
        public Y y; 
        public Tuple(X x, Y y) { 
          this.x = x; 
          this.y = y; 
        } 
      } 
}

