package it.unitn.ds;

import akka.actor.ActorRef;
import akka.actor.AbstractActor;
import akka.actor.Props;

import java.io.Serializable;
import java.util.*;
import java.time.Duration;


public class Node extends AbstractActor {
    final static int CSDURATION = 2000;         // the duration (in milliseconds) of the critical session
    final static int FDURATION = 2000;          // the duration (in milliseconds) of the FAILURE
    private final int id;                       // node id (not really necessary)
    private ActorRef holder;                    // relative position of the privileged node
    private Boolean using;                      // if this node is currently executing the critical session
    private LinkedList<ActorRef> request_q;     // queue of the requests
    private Boolean asked;                      // true if a node has sent a request message to the current holder
    private Boolean recovering;                 // signals to itself if a recovering phase is on going
    private Boolean failed;                     // used to signal to itself that the node is simulating a failure, 
                                                // allows to ignore messages reception during failure
    private HashMap<ActorRef, Advise> contacted_neighbors; // used only if recovering==TRUE to collect data usefull for recover from failure
    private ArrayList<ActorRef> neighbors;      // local info about the tree structure, only neigbor nodes


    /* ------------------------ Message types ------------------------ */

    // message used to initialize the node with neighbors
    public static class InitNode implements Serializable {
        private final ArrayList<ActorRef> neighbors;

        public InitNode(ArrayList<ActorRef> neighbors) {
            this.neighbors = neighbors;
        }
    }

    // message used to spread the initial information about the token holder
    public static class HolderInfo implements Serializable {
        private final int id;

        public HolderInfo(int id){
            this.id = id;
        }
    }

    // A message requesting a node to ask for entering the critical session
    public static class StartRequest implements Serializable {}

    // A request message
    public static class Request implements Serializable {}

    // A privilege message
    public static class Privilege implements Serializable {}

    // Message used by Sys to simulate failing 
    public static class Fail implements Serializable {}

    // Message used to exit from Failure and start recovering phase
    public static class Recover implements Serializable {}

    // Message used by a node to inform neighbours about Restart after failure 
    public static class Restart implements Serializable {}

    // Message used to send local knowledge to a restarting node 
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
    }

    static public Props props(int id) {
        return Props.create(Node.class, () -> new Node(id));
    }

    /* ------------------------ Actor behaviour ------------------------ */

    // initializes the node with neighbors
    private void onInitNode(InitNode m){
        this.request_q = new LinkedList<>();
        this.using = Boolean.FALSE;
        this.asked = Boolean.FALSE;
        this.recovering = Boolean.FALSE;
        this.neighbors = m.neighbors;
        this.failed = Boolean.FALSE;
        System.out.printf("*** Node %02d INITIALIZED with %02d neighbours ***, \n", this.id, this.neighbors.size());
    }

    // spreads initial information about the token holder
    private void onHolderInfo(HolderInfo m){
        if (m.id == -1){
            // if I am the initial holder
            this.holder = getSelf();
            System.out.printf("*** Node %02d set holder to SELF ***, \n", this.id);
        }else{
            // otherwise it is the sender
            this.holder = getSender();
            System.out.printf("*** Node %02d set holder to %02d ***, \n", this.id, m.id);
        }
        // spread the info to neigbors
        this.neighbors.forEach(neighbor -> {
            if (neighbor != getSender()){
                neighbor.tell(new HolderInfo(this.id), getSelf());
            }
        });
        System.out.printf("*** Node %02d is READY  ***, \n", this.id);
    }

    private void onStartRequest(StartRequest m) {
        if (!this.failed){
            this.request_q.add(getSelf());  // add self to the queue
            assignPrivilege();
            makeRequest();
        }else{
            System.out.printf("Node %02d ignores StartRequest message because is failed \n", this.id);
        }
    }

    private void onRequestMsg(Request m) {
        if (!this.failed){
            this.request_q.add(getSender());    // add sender to the queue
            assignPrivilege();
            makeRequest();
        }else{
            System.out.printf("Node %02d ignores Request message because is failed \n", this.id);
        }
    }

    private void onPrivilegeMsg(Privilege m) {
        if (!this.failed){
            this.holder = getSelf();     // set holder to self
            assignPrivilege();
            makeRequest();
        }else{
            System.out.printf("Node %02d ignores Priviledge message because is failed \n", this.id);
        }      
    }

    // simulates the failure of the node and prepares a
    // clean/new local environment for the Recovery phase
    // launches also the recovery phase
    private void onFailMsg(Fail m){
        // inform itself about failure
        this.failed = Boolean.TRUE;

        // delete any previous knowledge about the algorithm status 
        this.request_q = new LinkedList<>();
        this.using = Boolean.FALSE;
        this.asked = Boolean.FALSE;
        this.holder = null;
        System.out.printf("### Node %02d ### ###### FAILED ######, \n\t Waiting for Recovery...\n", this.id);

        // schedule a future message to be sent to itself in order to exit from Failure after FDURATION milliseconds
        // and start Recover phase
        context().system().scheduler().scheduleOnce(Duration.ofMillis(this.FDURATION), getSelf(), new Recover(), context().system().dispatcher(), null);
    }

    private void onRecover(Recover m){
        // inform other methods that we are restarting
        // if some messages are received during this phase 
        this.recovering = Boolean.TRUE;
        
        // exit from failure mode
        this.failed = Boolean.FALSE;

        // we wait for a reasonable long time to allow all messages 
        // to be sent and received between the other nodes
        int duration = 5000;
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0L;
        while (elapsedTime < duration) {
            elapsedTime = (new Date()).getTime() - startTime;
        }
        // inform neighbours about Restart a hashtable to 
        // memorize contacted neighbors and their status
        this.contacted_neighbors = new HashMap<>();
        this.neighbors.forEach(neighbor -> {
            neighbor.tell(new Restart(), getSelf());
            this.contacted_neighbors.put(neighbor, null);
        });
    }

    private void onRestart(Restart m){
        if (!this.failed){
            System.out.printf("Node %02d receives Restart\n", this.id);
            getSender().tell(new Advise(this.holder, this.request_q, this.asked, this.id), getSelf());
        }else{
            System.out.printf("Node %02d ignores Restart message because is failed \n", this.id);
        }
    }

    private void onAdvise(Advise m){
        if (!this.failed){
            System.out.printf("Node %02d receives Advise\n", this.id);
            // never execute this actions if the node is not recovering
            if (this.recovering){
                if (this.contacted_neighbors.get(getSender()) == null){
                    // set neighbor as visited and save its data
                    this.contacted_neighbors.put(getSender(), m);
                }
                if (collectedAllAdvises()){
                    inferStatus();
                }
            }
        }else{
            System.out.printf("Node %02d ignores Advise message because is failed \n", this.id);
        }
        
    }

    private void assignPrivilege() {
        // ensure that this method is never 
        // called during the recovery phase
        if (!this.recovering) {
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
        System.out.printf("### Node %02d ### enter the ##### Critical Section #####\n", this.id);

        // simulate the critical session
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0L;
        while (elapsedTime < CSDURATION) {
            elapsedTime = (new Date()).getTime() - startTime;
        }

        System.out.printf("Node %02d exit the Section Section\n", this.id);

        this.using = Boolean.FALSE;
        assignPrivilege();
        makeRequest();
    }

    private Boolean collectedAllAdvises(){
        for (ActorRef key : contacted_neighbors.keySet()){
            if (contacted_neighbors.get(key) == null){
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    private void inferStatus(){
        Boolean holder_for_all = Boolean.TRUE;

        for (ActorRef neighbor : contacted_neighbors.keySet()){
            Advise advise = contacted_neighbors.get(neighbor);
           
            if (advise.holder != getSelf() && this.holder == null){ 
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
        } else if (this.contacted_neighbors.get(this.holder).request_q.contains(getSelf())){
            this.asked = Boolean.TRUE;
        }

        this.recovering = Boolean.FALSE;

        // restart participation to algorithm
        assignPrivilege();
        makeRequest();

        System.out.printf("### Node %02d ### has infered:\n\t holder:%02d\n\t asked: %b\n\t size of req_q: %02d\n", 
            this.id, this.contacted_neighbors.get(holder).id, this.asked, this.request_q.size());

    }

    // Mapping between the received message type and actor methods
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartRequest.class, this::onStartRequest)
                .match(Request.class, this::onRequestMsg)
                .match(Privilege.class, this::onPrivilegeMsg)
                .match(Fail.class, this::onFailMsg)
                .match(Restart.class, this::onRestart)
                .match(Advise.class, this::onAdvise)
                .match(InitNode.class, this::onInitNode)
                .match(HolderInfo.class, this::onHolderInfo)
                .match(Recover.class, this::onRecover)
                .build();
    }

}

