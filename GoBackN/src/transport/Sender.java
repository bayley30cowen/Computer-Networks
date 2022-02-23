        package transport;
import java.util.*;
public class Sender extends NetworkHost {

    /*
     * Predefined Constant (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and Packet payload
     *
     *
     * Predefined Member Methods:
     *
     *  void startTimer(double increment):
     *       Starts a timer, which will expire in "increment" time units, causing the interrupt handler to be called.  You should only call this in the Sender class.
     *  void stopTimer():
     *       Stops the timer. You should only call this in the Sender class.
     *  void udtSend(Packet p)
     *       Sends the packet "p" into the network to arrive at other host
     *  void deliverData(String dataSent)
     *       Passes "dataSent" up to app layer. You should only call this in the Receiver class.
     *
     *  Predefined Classes:
     *
     *  NetworkSimulator: Implements the core functionality of the simulator
     *
     *  double getTime()
     *       Returns the current time in the simulator. Might be useful for debugging. Call it as follows: NetworkSimulator.getInstance().getTime()
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for debugging. Call it as follows: NetworkSimulator.getInstance().printEventList()
     *
     *  Message: Used to encapsulate a message coming from the application layer
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      void setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *      String getData():
     *          returns the data contained in the message
     *
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet, which is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload):
     *          creates a new Packet with a sequence field of "seq", an ack field of "ack", a checksum field of "check", and a payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an ack field of "ack", a checksum field of "check", and an empty payload
     *    Methods:
     *      void setSeqnum(int seqnum)
     *          sets the Packet's sequence field to seqnum
     *      void setAcknum(int acknum)
     *          sets the Packet's ack field to acknum
     *      void setChecksum(int checksum)
     *          sets the Packet's checksum to checksum
     *      void setPayload(String payload) 
     *          sets the Packet's payload to payload
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      String getPayload()
     *          returns the Packet's payload
     *
     */
    
    private final int ACK = 1; //Constants, including avg Round Trip Delay (RTT)
    private final int RTT = 20;
    private final int WINDOW_SIZE = 8; // Maximum no of send, unacked packets
    
    // Add any necessary class variables here. They can hold state information for the sender. 
    // Also add any necessary methods (e.g. checksum of a String)
    private int base; //seq no of oldest unacked packet
    private int currSeqNo; //availble seq number
    private ArrayList<Packet> pipeline; //used to buffer, allows for re-transmission
    
    /* Private Methods */
    
    //The payload of packet is input and computes the SUM of the ascii codes of individual chars
    //Returns the sum of message chars ascii values.
    private int checkSum(String payload) {
        int sum = 0;
        for (int i=0;i < payload.length() ;i++) {
            sum += payload.charAt(i);
        }
        return sum;
    }
    
    //This method is a corruption checker. It will return true if and only if packet was coruppted during transmission.
    //Input consists of packet being checked.
    private boolean corruptCheck(Packet p) {
        return (p.getSeqnum() + p.getAcknum() + checkSum(p.getPayload())) != p.getChecksum();
    }
    
    /*Public Methods */
    
    // This is the constructor.  Don't touch!
    public Sender(int entityName) {
        super(entityName);
    }
    
    // This method will be called once, before any of your other sender-side methods are called. 
    // It can be used to do any required initialisation (e.g. of member variables you add to control the state of the sender).
    @Override
    public void init() {
        base = 0;
        currSeqNo = 0;
        pipeline = new ArrayList<>();
    }
    
    // This method will be called whenever the app layer at the sender has a message to send.  
    // The job of your protocol is to ensure that the data in such a message is delivered in-order, and correctly, to the receiving application layer.
    @Override
    public void output(Message message) {
        //Ensures the message is only processed if unacked packets are less than the window size
        if (base + WINDOW_SIZE > currSeqNo) {
            //Setup fo packet data
            String payload = message.getData();
            //Create a packet
            Packet current = new Packet(currSeqNo, ACK, (currSeqNo + ACK + checkSum(payload)), payload);
            //Add to pipeline for retransmission (if needed)
            pipeline.add(current);
            //Sending packet to reciever, if no packets in pipleine, then start the timer
            udtSend(current);
            if (currSeqNo == base) {
                startTimer(RTT *2);
            }
            //Update the current availble seq number.
            currSeqNo++;
        }
    }
    
    
    // This method will be called whenever a packet sent from the receiver (i.e. as a result of a udtSend() being done by a receiver procedure) arrives at the sender.  
    // "packet" is the (possibly corrupted) packet sent from the receiver.
    @Override
    public void input(Packet packet) {
        //If ACK isnt corrupted or a duplicate, transmission done!
        if (!(corruptCheck(packet))) {
            //Updated base used to remove Acked packets from pipeline
            base = packet.getSeqnum() + 1;
                //Stops timer if no more packets in pipelines, otherwise restart timer.
            if (base == currSeqNo) {
                stopTimer();
            }
            else {
                startTimer(RTT * 2);
            }
        }
    }
    
    
    // This method will be called when the senders's timer expires (thus generating a timer interrupt). 
    // You'll probably want to use this method to control the retransmission of packets. 
    // See startTimer() and stopTimer(), above, for how the timer is started and stopped. 
    @Override
    public void timerInterrupt() {
        //If one packet was lost or delayed heavily, restart timer, retransmit packet waiting for Ack
        startTimer(RTT*2);
        for (int x = base; base < currSeqNo; x++) {
            udtSend(pipeline.get(x));
        }
    }
}
